package pdf_watcher

import Preferences.monitoredFolder
import kotlinx.coroutines.*
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


    fun toggleMonitor(): Boolean {
        return if (watchKey != null) {
            watchKey?.cancel()
            coroutine?.cancel()
            coroutine = null
            watchKey = null
            false
        } else {
            watchKey = Path(monitoredFolder).register(watchService, StandardWatchEventKinds.ENTRY_CREATE)
            coroutine = CoroutineScope(Dispatchers.Default).launch {
                while (watchKey != null) {
                    var files = getChangedFiles()
                    CoroutineScope(Dispatchers.Default).launch {
                        for (i in 0..3) {
                            files = processFiles(files)
                            if (files.isEmpty()) break
                            delay(3000)
                        }
                    }
                }
            }
            true
        }
    }

    fun monitorState() = watchKey != null && coroutine != null
}