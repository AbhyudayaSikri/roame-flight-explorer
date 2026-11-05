#!/usr/bin/env python3
"""Fallback flights API for machines without Java: Python 3.9+, standard library only.

    python3 fallback.py [--port=8080] [--mode=normal|empty|error|updated|slow] [--fixture=PATH] [--allow-control]

Serves GET /flights and GET /health like the Kotlin server in server/ (the one the docs use).
The fixture is --fixture, else flights.json next to this script, else ../fixtures/flights.json.
"""
import argparse
import json
import sys
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlparse

MODES = ("normal", "empty", "error", "updated", "slow")
SLOW_SECONDS = 3.0
EMPTY_ENVELOPE = b'{"flights":[]}'
ERROR_BODY = b'{"error":"simulated server failure (--mode=error)"}'


def find_fixture(path=None):
    """The fixture's bytes, from PATH or from the usual places next to this script."""
    here = Path(__file__).resolve().parent
    candidates = [Path(path)] if path else [here / "flights.json", here.parent / "fixtures" / "flights.json"]
    for candidate in candidates:
        if candidate.is_file():
            return candidate.read_bytes()
    raise FileNotFoundError("No fixture at " + " or ".join(str(c) for c in candidates) + "; pass --fixture=PATH.")


def with_updated_points(fixture, delta=500):
    """The fixture with every flight's points raised by DELTA (--mode=updated)."""
    data = json.loads(fixture)
    for flight in data["flights"]:
        flight["points"] += delta
    return json.dumps(data, separators=(",", ":")).encode()


class ServerState:
    def __init__(self, mode):
        self.mode = mode


def make_handler(fixture, state, allow_control, pause=time.sleep, log=sys.stderr):
    updated = with_updated_points(fixture)

    class Handler(BaseHTTPRequestHandler):
        def do_GET(self):
            path = urlparse(self.path).path
            if path == "/health":
                return self._send(200, b"ok", "text/plain; charset=utf-8")
            if path == "/flights":
                mode = state.mode
                if mode == "empty":
                    return self._send(200, EMPTY_ENVELOPE)
                if mode == "error":
                    return self._send(500, ERROR_BODY)
                if mode == "updated":
                    return self._send(200, updated)
                if mode == "slow":
                    pause(SLOW_SECONDS)
                return self._send(200, fixture)
            return self._send(404, b"Not found", "text/plain; charset=utf-8")

        def do_POST(self):
            parsed = urlparse(self.path)
            if allow_control and parsed.path == "/control/mode":
                value = (parse_qs(parsed.query).get("value") or [""])[0].strip().lower()
                if value in MODES:
                    state.mode = value
                    return self._send(200, value.encode(), "text/plain; charset=utf-8")
                return self._send(400, ("Unknown mode. Use one of: " + ", ".join(MODES)).encode(), "text/plain; charset=utf-8")
            return self._send(404, b"Not found", "text/plain; charset=utf-8")

        def _send(self, status, body, content_type="application/json"):
            self.send_response(status)
            self.send_header("Content-Type", content_type)
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)

        def log_request(self, code="-", size="-"):
            log.write(f"{self.command} {self.path} mode={state.mode} -> {code}\n")

    return Handler


def parse_args(argv):
    parser = argparse.ArgumentParser(description="Fallback flights API (see server/ for the main one).")
    parser.add_argument("--port", type=int, default=8080)
    parser.add_argument("--mode", default="normal", type=str.lower, choices=MODES)
    parser.add_argument("--fixture", default=None, help="flights JSON to serve")
    parser.add_argument("--allow-control", action="store_true", help="enable POST /control/mode?value=...")
    args = parser.parse_args(argv)
    if not 1 <= args.port <= 65535:
        parser.error("--port must be a number from 1 to 65535")
    return args


def main(argv=None):
    args = parse_args(sys.argv[1:] if argv is None else argv)
    try:
        fixture = find_fixture(args.fixture)
    except FileNotFoundError as error:
        sys.exit(str(error))
    server = ThreadingHTTPServer(("0.0.0.0", args.port), make_handler(fixture, ServerState(args.mode), args.allow_control))
    control = ", POST /control/mode enabled" if args.allow_control else ""
    print(f"Serving flights at http://0.0.0.0:{args.port}/flights (mode={args.mode}{control})", flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass


if __name__ == "__main__":
    main()
