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

fun match(files: List<String>, fileFunction: (String) -> String): List<Pair<String, String>> {
    val documents = findAllDocuments()
    return files
        .map { file ->
            val txtFile = fileFunction(file)
            Pair(file, documents.firstNotNullOfOrNull { buildDocumentName(it, txtFile, file) })
        }
        .mapNotNull { if (it.second != null) Pair(it.first, it.second!!) else null }
}


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

fun processFiles(files: List<String>) {
    match(files.filter { it.endsWith(".pdf") }) {
        try {
            PDDocument.load(File(it)).use { PDFTextStripper().getText(it).replace("[\\s ]+".toRegex(), " ") }
        } catch (_: Exception) {
            ""
        }
    }.forEach(::moveOrCopy)
    match(files.filter { it.endsWith(".ofx") }) {
        File(it).readText().replace("[\r\n\t ]+".toRegex(), " ")
    }.forEach(::moveOrCopy)
//    match(files.filter { it.endsWith(".zip") })
//    match(files.filter { it.endsWith(".xml") })
}

fun moveOrCopy(sourceDest: Pair<String, String>) {
    val (source, destination) = sourceDest
    try {
        if (moveFiles) {
            Files.createDirectories(Paths.get(File(destination).parent))
            File(source).renameTo(File(destination))
        } else File(source).copyTo(File(destination))
    } catch (_: Exception) {
        println("move or copy failed")
        println("moving from $source to $destination")
    }
}