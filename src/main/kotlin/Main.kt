import Preferences.monitoredFolder
import Preferences.props
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.inputStream

@Composable
@Preview
fun app() {
    val version = System.getProperty("java.version")
    var text by remember { mutableStateOf(props.getProperty("version")) }

    Column(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MaterialTheme {
            Button(onClick = {
                text = "Working at $monitoredFolder"
            }) {
                Text(text)
            }
        }
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Selkeys Control System",
        icon = appIcon
    ) {
        app()
    }
}

//as suggest on conveyor website https://conveyor.hydraulic.dev/7.2/tutorial/tortoise/2-gradle/#setting-icons
private val appIcon: Painter? by lazy {
    // app.dir is set when packaged to point at our collected inputs.
    val appDirProp = System.getProperty("app.dir")
    val appDir = appDirProp?.let { Path.of(it) }
    // On Windows we should use the .ico file. On Linux, there's no native compound image format and Compose can't render SVG icons,
    // so we pick the 128x128 icon and let the frameworks/desktop environment rescale. On macOS, we don't need to do anything.
    var iconPath = appDir?.resolve("app.ico")?.takeIf { it.exists() }
    iconPath = iconPath ?: appDir?.resolve("icon-square-128.png")?.takeIf { it.exists() }
    if (iconPath?.exists() == true) {
        BitmapPainter(iconPath.inputStream().buffered().use { loadImageBitmap(it) })
    } else {
        null
    }
}