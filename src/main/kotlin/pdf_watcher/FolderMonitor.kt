package pdf_watcher

import Preferences.monitoredFolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.nio.file.FileSystems
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey
import java.nio.file.WatchService
import kotlin.io.path.Path

object FolderMonitor {
    private val watchService: WatchService = FileSystems.getDefault().newWatchService()
    private var watchKey: WatchKey? = null
    private var coroutine: Job? = null
    private fun getChangedFiles(): List<String> =
        watchService.take().let { watchEvent ->
            watchEvent.reset()
            return watchEvent.pollEvents().map { "$monitoredFolder/${it.context()}" }
        }


    fun toggleMonitor(coroutineScope: CoroutineScope): Boolean {
        return if (watchKey != null) {
            watchKey?.cancel()
            coroutine?.cancel()
            coroutine = null
            watchKey = null
            false
        } else {
            watchKey = Path(monitoredFolder).register(watchService, StandardWatchEventKinds.ENTRY_CREATE)
            coroutine = coroutineScope.launch(Dispatchers.Default) {
                while (watchKey != null) {
                    processFiles(getChangedFiles())
                }
            }
            true
        }
    }

    fun monitorState() = if (watchKey != null && coroutine != null) true else false
}