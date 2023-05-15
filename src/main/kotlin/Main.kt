import UI.Gui
import UI.Gui.app
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import database.LocalDatabase
import pdf_watcher.FolderMonitor
import kotlin.system.exitProcess


fun main() = application {
    init()
    val state =
        rememberWindowState(placement = WindowPlacement.Floating, width = Dp.Unspecified, height = Dp.Unspecified)
    Window(
        onCloseRequest = { exitProcess(0) },
        title = "Selkeys Control System",
        transparent = true,
        undecorated = true,
        icon = Gui.appIcon,
        state = state
    ) {
        app(this@application, state)
    }
}

fun init() {
    LocalDatabase
    FolderMonitor
}
