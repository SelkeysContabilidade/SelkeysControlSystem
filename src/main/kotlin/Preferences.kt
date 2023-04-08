import java.util.*
import java.util.prefs.Preferences
import javax.swing.JFileChooser
import javax.swing.UIManager

object Preferences {
    var monitoredFolder: String
    private var moveUnknownFiles: Boolean
    private val prefs: Preferences = Preferences.userNodeForPackage(Preferences::class.java)
    val props = Properties()


    init {
        monitoredFolder = prefs.get("monitoredFolder", System.getProperty("user.dir"))
        moveUnknownFiles = prefs.get("moveUnkownFiles", "false").toBoolean()
        Thread
            .currentThread()
            .contextClassLoader
            .getResourceAsStream("application.properties")
            .use(props::load)
    }

    fun selectNewFolder() {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        JFileChooser("/").apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            dialogTitle = "Selecione uma pasta"
            approveButtonText = "Selecionar"
            approveButtonToolTipText = "Seleciona o diretorio descrito a ser monitorado"
        }.let {
            it.showOpenDialog(null)
            monitoredFolder = it.selectedFile.toString()
        }
        prefs.put("monitoredFolder", monitoredFolder)
    }

}