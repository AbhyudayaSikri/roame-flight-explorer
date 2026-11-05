package travel.roame.takehome.android.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val lightColors = lightColorScheme(
    primary = Color(0xFF1B4F8A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4F7),
    onPrimaryContainer = Color(0xFF0B2545),
    secondary = Color(0xFF0E7C66),
    secondaryContainer = Color(0xFFD2EEE7),
    onSecondaryContainer = Color(0xFF053A2F),
    tertiary = Color(0xFFB3541E),
    error = Color(0xFFB3261E),
    background = Color(0xFFF7F8FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEEF1F5),
)

/** The same hues, lighter on dark surfaces. The status bar follows the system, so it needs a dark app. */
private val darkColors = darkColorScheme(
    primary = Color(0xFFA9C7F0),
    onPrimary = Color(0xFF0B2545),
    primaryContainer = Color(0xFF1D3F69),
    onPrimaryContainer = Color(0xFFD6E4F7),
    secondary = Color(0xFF7FD3BE),
    secondaryContainer = Color(0xFF0B5446),
    onSecondaryContainer = Color(0xFFD2EEE7),
    tertiary = Color(0xFFF0A273),
    error = Color(0xFFF2B8B5),
    background = Color(0xFF111418),
    surface = Color(0xFF16191E),
    surfaceVariant = Color(0xFF2A2F36),
)

/** Spacing scale used on every screen, so lists, sheets and the card line up. */
object Spacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
}

@Composable
fun FlightExplorerTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) darkColors else lightColors, content = content)
}
