import Preferences.firstExecution
import UI.Gui
import UI.Gui.app
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlin.system.exitProcess


fun main() = application {

    val state =
        rememberWindowState(placement = WindowPlacement.Floating, width = Dp.Unspecified, height = Dp.Unspecified)

    if (firstExecution) {
        //TODO START BY UPDATING DATABASE AND SETTING CONFIGS
//        runIntro()
    }

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

