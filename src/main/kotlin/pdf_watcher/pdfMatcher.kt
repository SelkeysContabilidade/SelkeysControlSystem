package pdf_watcher

import Preferences.monitoredFolder
import Preferences.moveFiles
import database.LocalDatabase
import database.LocalDatabase.findAllDocuments
import database.LocalDatabase.findTranslation
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths


fun buildDocumentName(document: LocalDatabase.Document, content: String, fileName: String): String? {
    if (document.identifier.toRegex() !in content) return null
    var registry = document.registryRegex.toRegex().find(content)?.value.orEmpty()
    val client = LocalDatabase.findByRegistry(registry)
    registry = registry.replace("/".toRegex(), "_").replace("[^0-9_.]*".toRegex(), "")
    var filename = ""
    var folder = document.baseFolderStructure
    document.getProceduresOrdered().forEach {
        val name = when (it.type) {
            "clientFolder" -> client?.baseFolderStructure ?: it.content.plus(" $registry").trim()
            "nickname" -> (client?.nickname ?: it.content.plus(" $registry").trim()) + " "
            "regex" -> it.content.toRegex().find(content)?.value.orEmpty()
            "regexTranslated" -> findTranslation(it.content.toRegex().find(content)?.value.orEmpty())
            "regexFilename" -> it.content.toRegex().find(fileName)?.value.orEmpty()
            "string" -> it.content
            else -> ""
        }
        if (it.isFolder) folder += name else filename += name
    }
    return "$monitoredFolder/" + folder + filename.replace("/".toRegex(), "_")
}

fun processFiles(files: List<String>): List<String> {
    val documents = findAllDocuments()
    val mutable = mutableSetOf<String>()
    val emptySpaces = "[\r\n\t\\s  ]+".toRegex()

    files.stream().parallel().forEach { filename ->
        try {
            val content = when (File(filename).extension) {
                "pdf" -> PDDocument.load(File(filename)).use { PDFTextStripper().getText(it) }
                "ofx" -> File(filename).readText()
                else -> null
            }?.replace(emptySpaces, " ")
            if (content != null) {
                val destination = documents.firstNotNullOfOrNull { buildDocumentName(it, content, filename) }
                moveOrCopy(filename, destination)
            }
        } catch (_: Exception) {
            mutable.add(filename)
        }
    }
    return mutable.toList()
}

fun moveOrCopy(source: String, destination: String?) {
    if (destination == null) throw Exception()
    if (moveFiles) {
        Files.createDirectories(Paths.get(File(destination).parent))
        if (!File(source).renameTo(File(destination))) throw Exception()
    } else File(source).copyTo(File(destination))
}