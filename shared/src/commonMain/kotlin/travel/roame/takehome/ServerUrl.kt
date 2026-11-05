package travel.roame.takehome

private val SCHEME = Regex("^([A-Za-z][A-Za-z0-9+.-]*)://")
private val HOST_NAME = Regex("^[A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?(\\.[A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?)*$")
private val IPV6_HOST = Regex("^\\[[0-9A-Fa-f:.]+]$")

/** Why [text] can't be the flights server's address, or null when it can. Both apps show this as typed. */
fun serverUrlProblem(text: String): String? = checkServerUrl(text).problem

/**
 * [text] as the address the apps fetch from: trimmed, "http://" added when there's no scheme, the
 * scheme lowercased and any trailing "/" removed. Null when [serverUrlProblem] isn't.
 */
fun normalizedServerUrl(text: String): String? = checkServerUrl(text).url

private class ServerUrlCheck(val url: String?, val problem: String?)

private fun problem(text: String) = ServerUrlCheck(url = null, problem = text)

private fun checkServerUrl(text: String): ServerUrlCheck {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return problem("Enter the server's address, such as http://192.168.1.20:8080.")

    val schemeMatch = SCHEME.find(trimmed)
    val scheme = schemeMatch?.groupValues?.get(1)?.lowercase() ?: "http"
    if (scheme != "http" && scheme != "https") return problem("Use an http:// or https:// address.")
    val rest = if (schemeMatch == null) trimmed else trimmed.substring(schemeMatch.range.last + 1)
    if (rest.any { it == '?' || it == '#' }) return problem("Leave out the ? or # part.")

    val authority = rest.substringBefore('/')
    val path = rest.substring(authority.length).trimEnd('/')
    if ('@' in authority) return problem("Leave out the user name.")
    if (path.any { it.isWhitespace() }) return problem("That address has a space in it.")

    val host: String
    val portText: String?
    if (authority.startsWith("[")) {
        val close = authority.indexOf(']')
        if (close < 0) return problem("That host name has characters a server address can't have.")
        host = authority.substring(0, close + 1)
        val after = authority.substring(close + 1)
        portText = if (after.isEmpty()) null else if (after.startsWith(":")) after.drop(1) else return problem("That host name has characters a server address can't have.")
    } else {
        host = authority.substringBefore(':')
        portText = if (':' in authority) authority.substringAfter(':') else null
    }

    if (host.isEmpty()) return problem("Add the server's host name or IP address.")
    if (!HOST_NAME.matches(host) && !IPV6_HOST.matches(host)) {
        return problem("That host name has characters a server address can't have.")
    }
    if (portText != null) {
        val port = portText.takeIf { it.isNotEmpty() && it.all(Char::isDigit) && it.length <= 5 }?.toInt()
        if (port == null || port !in 1..65535) return problem("The port must be a number from 1 to 65535.")
    }
    val portPart = portText?.let { ":$it" }.orEmpty()
    return ServerUrlCheck(url = "$scheme://$host$portPart$path", problem = null)
}
