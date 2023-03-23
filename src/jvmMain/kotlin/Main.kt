import androidx.compose.material.MaterialTheme
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
fun App() {
    var text by remember { mutableStateOf("Hello, World!") }

    MaterialTheme {
        Button(onClick = {
            text = System.getProperty("java.version")
        }) {
            Text(text)
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
private val appIcon: Painter? by lazy {
    // app.dir is set when packaged to point at our collected inputs.
    val appDirProp = System.getProperty("app.dir")
    val appDir = appDirProp?.let { Path.of(it) }
    // On Windows we should use the .ico file. On Linux, there's no native compound image format and Compose can't render SVG icons,
    // so we pick the 128x128 icon and let the frameworks/desktop environment rescale. On macOS we don't need to do anything.
    var iconPath = appDir?.resolve("app.ico")?.takeIf { it.exists() }
    iconPath = iconPath ?: appDir?.resolve("icon-square-128.png")?.takeIf { it.exists() }
    if (iconPath?.exists() == true) {
        BitmapPainter(iconPath.inputStream().buffered().use { loadImageBitmap(it) })
    } else {
        null
    }
}