"""Tests for fallback.py: the same behaviour as the Kotlin server's FlightsModuleTest.

    python3 -m unittest discover -s server -p "test_*.py"
"""
import io
import json
import threading
import unittest
import urllib.error
import urllib.request
from http.server import ThreadingHTTPServer
from pathlib import Path

import fallback

FIXTURE_PATH = Path(__file__).resolve().parent.parent / "fixtures" / "flights.json"
FIXTURE = FIXTURE_PATH.read_bytes()


class FallbackServerTest(unittest.TestCase):
    def serve(self, mode, allow_control=False):
        self.pauses = []
        state = fallback.ServerState(mode)
        handler = fallback.make_handler(FIXTURE, state, allow_control, pause=self.pauses.append, log=io.StringIO())
        server = ThreadingHTTPServer(("127.0.0.1", 0), handler)
        threading.Thread(target=server.serve_forever, daemon=True).start()
        self.addCleanup(server.server_close)
        self.addCleanup(server.shutdown)
        self.base = f"http://127.0.0.1:{server.server_address[1]}"

    def request(self, path, method="GET"):
        try:
            with urllib.request.urlopen(urllib.request.Request(self.base + path, method=method)) as response:
                return response.status, response.headers.get("Content-Type"), response.read()
        except urllib.error.HTTPError as error:
            return error.code, error.headers.get("Content-Type"), error.read()

    def test_normal_mode_serves_the_fixture_byte_for_byte(self):
        self.serve("normal")
        status, content_type, body = self.request("/flights")
        self.assertEqual(200, status)
        self.assertEqual("application/json", content_type)
        self.assertEqual(FIXTURE, body)

    def test_empty_mode_serves_an_empty_envelope(self):
        self.serve("empty")
        status, _, body = self.request("/flights")
        self.assertEqual((200, []), (status, json.loads(body)["flights"]))

    def test_error_mode_fails_with_500(self):
        self.serve("error")
        self.assertEqual(500, self.request("/flights")[0])

    def test_updated_mode_adds_500_points_and_changes_nothing_else(self):
        self.serve("updated")
        original = json.loads(FIXTURE)["flights"]
        updated = json.loads(self.request("/flights")[2])["flights"]
        self.assertEqual(1000, len(updated))
        for before, after in zip(original, updated):
            self.assertEqual(before["points"] + 500, after["points"])
            self.assertEqual({k: v for k, v in before.items() if k != "points"}, {k: v for k, v in after.items() if k != "points"})

    def test_slow_mode_waits_three_seconds_then_serves_the_fixture(self):
        self.serve("slow")
        self.assertEqual(FIXTURE, self.request("/flights")[2])
        self.assertEqual([3.0], self.pauses)

    def test_health_returns_ok_in_every_mode(self):
        self.serve("error")
        self.assertEqual((200, b"ok"), (self.request("/health")[0], self.request("/health")[2]))

    def test_unknown_path_is_not_found(self):
        self.serve("normal")
        self.assertEqual(404, self.request("/flight")[0])

    def test_control_endpoint_switches_the_mode_when_allowed(self):
        self.serve("normal", allow_control=True)
        self.assertEqual(200, self.request("/control/mode?value=empty", method="POST")[0])
        self.assertEqual([], json.loads(self.request("/flights")[2])["flights"])
        self.assertEqual(400, self.request("/control/mode?value=sideways", method="POST")[0])
        self.assertEqual(200, self.request("/control/mode?value=normal", method="POST")[0])
        self.assertEqual(FIXTURE, self.request("/flights")[2])

    def test_control_endpoint_does_not_exist_unless_allowed(self):
        self.serve("normal")
        self.assertEqual(404, self.request("/control/mode?value=empty", method="POST")[0])
        self.assertEqual(FIXTURE, self.request("/flights")[2])


class FallbackSetupTest(unittest.TestCase):
    def test_finds_the_repository_fixture_by_default(self):
        self.assertEqual(FIXTURE, fallback.find_fixture())

    def test_a_fixture_path_must_exist(self):
        with self.assertRaises(FileNotFoundError):
            fallback.find_fixture("/no/such/flights.json")

    def test_flags_match_the_kotlin_server(self):
        args = fallback.parse_args(["--port=9090", "--mode=SLOW", "--fixture=/tmp/f.json", "--allow-control"])
        self.assertEqual((9090, "slow", "/tmp/f.json", True), (args.port, args.mode, args.fixture, args.allow_control))
        for bad in (["--port=70000"], ["--mode=sideways"], ["--verbose"]):
            with self.assertRaises(SystemExit):
                fallback.parse_args(bad)


if __name__ == "__main__":
    unittest.main()
