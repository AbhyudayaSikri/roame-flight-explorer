package travel.roame.takehome.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val colors = lightColorScheme(
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

/** Spacing scale used on every screen, so lists, sheets and the card line up. */
object Spacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
}

@Composable
fun FlightExplorerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
