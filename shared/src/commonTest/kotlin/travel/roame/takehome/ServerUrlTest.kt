package travel.roame.takehome

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ServerUrlTest {
    @Test
    fun acceptsTheAddressesTestersUse() {
        assertEquals("http://192.168.1.20:8080", normalizedServerUrl("http://192.168.1.20:8080"))
        assertEquals("http://10.0.2.2:8080", normalizedServerUrl("http://10.0.2.2:8080/"))
        assertEquals("http://localhost:8080", normalizedServerUrl("  http://localhost:8080  "))
        assertEquals("http://TN00897.local:8080", normalizedServerUrl("http://TN00897.local:8080"))
        assertEquals("https://flights.example.com", normalizedServerUrl("https://flights.example.com"))
        assertEquals("http://[::1]:8080", normalizedServerUrl("http://[::1]:8080"))
        assertEquals("http://example.com/api", normalizedServerUrl("http://example.com/api/"))
        assertNull(serverUrlProblem("http://192.168.1.20:8080"))
    }

    @Test
    fun addsHttpWhenTheSchemeIsMissingAndLowercasesIt() {
        assertEquals("http://192.168.1.20:8080", normalizedServerUrl("192.168.1.20:8080"))
        assertEquals("http://Mac.local:8080", normalizedServerUrl("HTTP://Mac.local:8080"))
    }

    @Test
    fun rejectsWhatCantBeAServerAddress() {
        listOf(
            "", "   ", "ftp://host", "http://", "http://:8080", "http://host:0", "http://host:70000", "http://host:",
            "http://host:abc", "http://my host", "http://host:8080?mode=slow", "http://host#top", "http://user@host",
            "http://[::1", "http://ho_st",
        ).forEach { text ->
            assertNull(normalizedServerUrl(text), text)
            assertNotNull(serverUrlProblem(text), text)
        }
    }

    @Test
    fun explainsWhatToFix() {
        assertEquals("Enter the server's address, such as http://192.168.1.20:8080.", serverUrlProblem("  "))
        assertEquals("Use an http:// or https:// address.", serverUrlProblem("ftp://host"))
        assertEquals("Add the server's host name or IP address.", serverUrlProblem("http://:8080"))
        assertEquals("The port must be a number from 1 to 65535.", serverUrlProblem("http://host:70000"))
        assertEquals("Leave out the ? or # part.", serverUrlProblem("http://host:8080?mode=slow"))
        assertEquals("Leave out the user name.", serverUrlProblem("http://user@host"))
        assertEquals("That host name has characters a server address can't have.", serverUrlProblem("http://my host"))
    }
}
