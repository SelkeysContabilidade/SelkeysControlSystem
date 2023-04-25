package pdf_watcher

import Preferences.monitoredFolder
import java.nio.file.FileSystems
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey
import java.nio.file.WatchService
import kotlin.io.path.Path

class FolderMonitor(watchPath: String) {
    private val watchService: WatchService = FileSystems.getDefault().newWatchService()
    private val watchKey: WatchKey

    init {
        watchKey = Path(watchPath).register(watchService, StandardWatchEventKinds.ENTRY_CREATE)
    }

    fun close() = watchKey.cancel()
    fun getChangedFiles(): List<String> =
        watchService.take().let { watchEvent ->
            watchEvent.reset()
            return watchEvent.pollEvents().map { "$monitoredFolder/${it.context()}" }
        }

}