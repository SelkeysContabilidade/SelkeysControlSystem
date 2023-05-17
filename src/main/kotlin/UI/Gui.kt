package UI

import Preferences
import Preferences.disableFirstExecutionWarning
import Preferences.firstExecution
import Preferences.moveFiles
import Preferences.props
import Preferences.toggleMoveFiles
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Home
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import database.LocalDatabase.resyncDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pdf_watcher.FolderMonitor
import pdf_watcher.processFiles
import java.awt.Dimension
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.Path as KotlinPath

object Gui {

    private val menuBarHeight = 30.dp
    private val topBarHeight = 56.dp
    private val edgePadding = 10.dp

    //as suggest on conveyor website https://conveyor.hydraulic.dev/7.2/tutorial/tortoise/2-gradle/#setting-icons
    val appIcon: Painter? by lazy {
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

    private lateinit var coroutineScope: CoroutineScope
    private lateinit var scaffoldState: ScaffoldState
    private lateinit var windowState: WindowState
    private lateinit var currentScreen: MutableState<CurrentScreen>
    private lateinit var drawerSize: MutableState<DpSize>
    private lateinit var drawerState: DrawerState
    private lateinit var drawerOffset: MutableState<Float>
    private lateinit var contentSize: MutableState<DpSize>
    private lateinit var monitoredFolder: MutableState<String>


    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    @Preview
    fun FrameWindowScope.app(appScope: ApplicationScope, state: WindowState) {
        drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        drawerOffset = remember { mutableStateOf(drawerState.offset ?: 0F) }
        scaffoldState = rememberScaffoldState(drawerState = drawerState)
        coroutineScope = rememberCoroutineScope()
        windowState = state
        drawerSize = remember { mutableStateOf(DpSize(100.dp, 100.dp)) }
        currentScreen =
            remember { mutableStateOf(if (firstExecution) CurrentScreen.FIRST_EXECUTION else CurrentScreen.MAIN) }
        contentSize = remember { mutableStateOf(DpSize(300.dp, 300.dp)) }
        windowState.size = contentSize.value
        window.minimumSize = Dimension(395, drawerSize.value.height.value.toInt())
        monitoredFolder = remember { mutableStateOf(Preferences.monitoredFolder) }

        //not accessing the window state here breaks the first composition because it doesn't get properly updated
        //seems to be a bug in compose 1.4.0
        windowState.size

        Scaffold(
            scaffoldState = scaffoldState,
            topBar = { topBar() },
            backgroundColor = Color.Transparent,
            drawerBackgroundColor = Color(66, 98, 173, 250),
            drawerElevation = edgePadding,
            drawerShape = drawerSize(),
            drawerContent = { drawerContent() },
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .offset(y = menuBarHeight)
                    .background(Color(66, 98, 173, 240))
            ) {
                when (currentScreen.value) {
                    CurrentScreen.MAIN -> mainContent()
                    CurrentScreen.SETTINGS -> settingsContent()
                    CurrentScreen.ABOUT -> aboutContent()
                    CurrentScreen.FIRST_EXECUTION -> firstExecution()
                }
            }
        }
        menuBar(appScope)
    }

    @Composable
    private fun aboutContent() {
        Column(contentModifier()) {
            Text("Versão do java ${System.getProperty("java.version")}")
            Text("Versão do Sistema: ${props.getProperty("version")}")
        }
    }

    @Composable
    private fun firstExecution() {
        var dbUpdated by remember { mutableStateOf(false) }
        var selectedOperation by remember { mutableStateOf(false) }
        Column(contentModifier()) {
            Spacer(Modifier.size(5.dp))
            Text("Primeira Execução:", modifier = Modifier, fontSize = 1.5.em, color = Color.White)
            Button(onClick = {
                coroutineScope.launch(Dispatchers.Default) { resyncDatabase() }
                dbUpdated = true
            }) { Text("Atualizar banco de dados") }
            Button(onClick = {
                monitoredFolder.value = Preferences.selectMonitoredFolder()
                selectedOperation = true
            }) { Text("Selecionar local de operação") }
        }
        if (dbUpdated && selectedOperation) {
            disableFirstExecutionWarning()
            currentScreen.value = CurrentScreen.MAIN
        }
    }

    @Composable
    private fun mainContent() {
        Row(
            contentModifier(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            var monitoring by remember { mutableStateOf(FolderMonitor.monitorState()) }
            Button(onClick = { monitoring = FolderMonitor.toggleMonitor() }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (monitoring) {
                        Icon(painterResource("Icons/Eye.svg"), null)
                        Spacer(Modifier.width(5.dp))
                        Text("Monitoramento Ativado")
                    } else {
                        Icon(painterResource("Icons/Eye_Off.svg"), null)
                        Spacer(Modifier.width(5.dp))
                        Text("Monitoramento Desativado")
                    }
                }
            }
            Button(onClick = {
                coroutineScope.launch(Dispatchers.Default) {
                    processFiles(KotlinPath(monitoredFolder.value).listDirectoryEntries().map { it.toString() })
                }
            }) {
                Icon(painterResource("Icons/Play.svg"), null)
                Spacer(Modifier.width(5.dp))
                Text("Executar Uma Vez")
            }
        }
    }


    @Composable
    private fun settingsContent() {
        Row(contentModifier()) {
            Column {
                Button(onClick = { monitoredFolder.value = Preferences.selectMonitoredFolder() }) {
                    Icon(painterResource("Icons/Folder_Open.svg"), "Move Files")
                    Spacer(Modifier.width(5.dp))
                    Text("Selecionar local de operação")
                }
                var moveFilesButton by remember { mutableStateOf(moveFiles) }
                Button(onClick = { moveFilesButton = toggleMoveFiles() }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (moveFilesButton) {
                            Icon(painterResource("Icons/Move.svg"), "Move Files")
                            Spacer(Modifier.width(5.dp))
                            Text("Mover arquivos")
                        } else {
                            Icon(painterResource("Icons/Copy.svg"), "Copy Files")
                            Spacer(Modifier.width(5.dp))
                            Text("Copiar arquivos")
                        }
                    }
                }
            }
            Spacer(Modifier.width(5.dp))
            Column {
                Button(onClick = {
                    coroutineScope.launch(Dispatchers.Default) { resyncDatabase() }
                }) {
                    Icon(painterResource("Icons/Sync.svg"), null)
                    Spacer(Modifier.width(5.dp))
                    Text("Atualizar banco de dados")
                }
                Button(onClick = { currentScreen.value = CurrentScreen.ABOUT }) {
                    Icon(painterResource("Icons/Info.svg"), null)
                    Spacer(Modifier.width(5.dp))
                    Text("Sobre")
                }
            }
        }
    }

    @Composable
    private fun topBar() = TopAppBar(
        modifier = Modifier.offset(y = menuBarHeight),
        backgroundColor = Color(66, 98, 193, 255),
        title = {
            Column {
                Text("Selkeys")
                Text(
                    "Operando em ${monitoredFolder.value}",
                    Modifier.scale(0.8F),
                    fontStyle = FontStyle.Italic,
                    softWrap = false
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = { toggleDrawer() }) {
                Icon(Icons.Filled.Menu, contentDescription = null)
            }
        }
    )

    @Composable
    private fun drawerContent() {
        Box(Modifier
            .padding(top = menuBarHeight + topBarHeight, start = edgePadding)
            .wrapContentHeight(unbounded = true)
            .onSizeChanged {
                drawerSize.value =
                    DpSize(it.width.dp + edgePadding.value.dp, it.height.dp + menuBarHeight + topBarHeight)
            }
        ) {
            Column {
                IconButton(onClick = { currentScreen.value = CurrentScreen.MAIN }) {
                    Row {
                        Icon(Icons.Outlined.Home, contentDescription = "Página Principal")
                        Text("Página Principal")
                    }
                }
                IconButton(onClick = { currentScreen.value = CurrentScreen.SETTINGS }) {
                    Row {
                        Icon(Icons.Filled.Build, contentDescription = "Configurações")
                        Text("Configurações")
                    }
                }
            }
        }
    }

    @Composable
    private fun FrameWindowScope.menuBar(applicationScope: ApplicationScope) =
        WindowDraggableArea(
            modifier = Modifier
                .background(color = Color(75, 75, 75, 230))
                .height(menuBarHeight)
                .padding(start = 5.dp, end = 5.dp, top = 3.dp, bottom = 3.dp)
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.wrapContentWidth(Alignment.Start), verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painterResource("Icons/Selkeys.svg"),
                        contentDescription = "Selkeys Logo",
                        Modifier.background(Color.White),
                        Alignment.Center,
                        ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(text = "Selkeys Control System", color = Color.White)
                }
                Row(Modifier.wrapContentWidth(Alignment.End)) {
                    IconButton(onClick = { windowState.isMinimized = true }) {
                        Icon(painterResource("Icons/Minimize.svg"), contentDescription = "Minimize")
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    var toggleFullscreenIcon by remember { mutableStateOf("Icons/Fullscreen.svg") }
                    IconButton(
                        onClick = {
                            if (windowState.placement == WindowPlacement.Maximized) {
                                windowState.placement = WindowPlacement.Floating
                                toggleFullscreenIcon = "Icons/Fullscreen.svg"
                            } else {
                                windowState.placement = WindowPlacement.Maximized
                                toggleFullscreenIcon = "Icons/Fullscreen_exit.svg"
                            }
                        }) {
                        Icon(painterResource(toggleFullscreenIcon), "Toggle Fullscreen")
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    IconButton(onClick = { applicationScope.exitApplication() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Exit")
                    }
                }
            }
        }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun contentModifier(): Modifier {
        val contentOffset = remember { derivedStateOf { drawerState.offset ?: 0F } }
        return Modifier
            .wrapContentWidth(unbounded = true)
            .offset(max(edgePadding, contentOffset.value.dp + drawerSize.value.width + edgePadding * 2))
            .onSizeChanged { contentSize.value = DpSize(it.width.dp, it.height.dp + topBarHeight + menuBarHeight) }
            .padding(end = edgePadding * 2)
    }

    private fun drawerSize() = object : Shape {
        override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
            return Outline.Rectangle(
                Rect(
                    0f,
                    menuBarHeight.value + topBarHeight.value,
                    drawerSize.value.width.value + 10F,
                    size.height
                )
            )
        }
    }

    private fun toggleDrawer() = coroutineScope.launch {
        scaffoldState.drawerState.apply { if (isClosed) open() else close() }
    }

}

enum class CurrentScreen() {
    MAIN,
    SETTINGS,
    ABOUT,
    FIRST_EXECUTION,
}