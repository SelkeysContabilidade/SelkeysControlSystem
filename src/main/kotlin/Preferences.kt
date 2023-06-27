import pdf_watcher.FolderMonitor.monitorState
import pdf_watcher.FolderMonitor.toggleMonitor
import java.util.*
import java.util.prefs.Preferences
import javax.swing.JFileChooser
import javax.swing.UIManager

object Preferences {
    var monitoredFolder: String
    var firstExecution: Boolean
    var moveFiles: Boolean
    var moveUnknownClients: Boolean
    var useSecondaryStorage: Boolean
    var authRecordFilePath: String

    private val prefs: Preferences = Preferences.userNodeForPackage(Preferences::class.java)
    val props = Properties()


    init {
        monitoredFolder = prefs.get("monitoredFolder", System.getProperty("user.dir"))
        authRecordFilePath =
            props.getProperty("authRecordFilePath", "${System.getProperty("user.home")}/.selkeys/tokenCache")
        moveFiles = prefs.get("moveFiles", "false").toBoolean()
        moveUnknownClients = prefs.get("moveUnknownClients", "false").toBoolean()
        useSecondaryStorage = prefs.get("useSecondaryStorage", "false").toBoolean()
        firstExecution = prefs.get("firstExecution", "true").toBoolean()
        Thread
            .currentThread()
            .contextClassLoader
            .getResourceAsStream("application.properties")
            .use(props::load)
    }

    fun selectMonitoredFolder(title: String = "Selecione uma pasta"): String {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        JFileChooser("/").apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            dialogTitle = title
            approveButtonText = "Selecionar"
            approveButtonToolTipText = "Seleciona o diretorio descrito a ser monitorado"
        }.let {
            it.showOpenDialog(null)
            monitoredFolder = it.selectedFile?.toString() ?: selectMonitoredFolder("Diretório invalido")
        }
        prefs.put("monitoredFolder", monitoredFolder)
        if (monitorState()) toggleMonitor()
        return monitoredFolder
    }

    fun disableFirstExecutionWarning() = prefs.put("firstExecution", "false")

    fun toggleMoveFiles(): Boolean {
        moveFiles = !moveFiles
        prefs.put("moveFiles", moveFiles.toString())
        return moveFiles
    }

    fun toggleSecondaryStorage(): Boolean {
        useSecondaryStorage = !useSecondaryStorage
        prefs.put("secondaryStorage", useSecondaryStorage.toString())
        return useSecondaryStorage
    }

    fun toggleMoveUnknownClients(): Boolean {
        moveUnknownClients = !moveUnknownClients
        prefs.put("moveUnknownClients", moveUnknownClients.toString())
        return moveUnknownClients
    }

}