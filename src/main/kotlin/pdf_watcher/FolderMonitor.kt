package pdf_watcher

import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchService

class FolderMonitor(watchPath: Path) {
    private val watcher: WatchService = FileSystems.getDefault().newWatchService()

    init {
        watchPath.register(watcher, StandardWatchEventKinds.ENTRY_CREATE)
    }

    fun getChangedFiles(): List<String> =
        watcher.take().let { watchEvent ->
            watchEvent.reset()
            return watchEvent.pollEvents().map { it.context().toString() }
        }

}