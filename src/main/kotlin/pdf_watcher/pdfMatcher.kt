package pdf_watcher

import Preferences.monitoredFolder
import Preferences.moveFiles
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import database.LocalDatabase
import database.LocalDatabase.findAllDocuments
import database.LocalDatabase.findTranslation
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

fun matchPdf(files: List<String>): List<Pair<String, String>> {
    val documents = findAllDocuments()
    return files
        .map { file ->
            try {
                val txtFile = PDDocument
                    .load(File(file))
                    .use { PDFTextStripper().getText(it).replace("[\\s ]+".toRegex(), " ") }
                Pair(file, documents.firstNotNullOfOrNull { buildDocumentName(it, txtFile) })
            } catch (_: Exception) {
                Pair("", null)
            }
        }
        .mapNotNull { if (it.second != null) Pair(it.first, it.second!!) else null }
}

fun matchOfx(files: List<String>): List<Pair<String, String>> {
    val documents = findAllDocuments()
    return files
        .map { file ->
            val txtFile = File(file).readText().replace("[\r\n\t ]+".toRegex(), " ")
            Pair(file, documents.firstNotNullOfOrNull { buildDocumentName(it, txtFile) })
        }
        .mapNotNull { if (it.second != null) Pair(it.first, it.second!!) else null }
}


fun buildDocumentName(document: LocalDatabase.Document, file: String): String? {
    if (document.identifier.toRegex() !in file) return null
    var registry = document.registryRegex.toRegex().find(file)?.value.orEmpty()
    val client = LocalDatabase.findByRegistry(registry)
    registry = registry.replace("/".toRegex(), "_").replace("[^0-9_.]*".toRegex(), "")
    var filename = ""
    var folder = document.baseFolderStructure
    document.getProceduresOrdered().forEach {
        val name = when (it.type) {
            "clientFolder" -> client?.baseFolderStructure ?: it.content.plus(" $registry").trim()
            "nickname" -> (client?.nickname ?: it.content.plus(" $registry").trim()) + " "
            "regex" -> it.content.toRegex().find(file)?.value.orEmpty()
            "regexTranslated" -> findTranslation(it.content.toRegex().find(file)?.value.orEmpty())
            "string" -> it.content
            else -> ""
        }
        if (it.isFolder) folder += name else filename += name
    }
    return "$monitoredFolder/" + folder + filename.replace("/".toRegex(), "_")
}

fun matchXml(): Any {
    //TODO
    return ""

}

fun matchZip(): Any {
    //TODO
    return ""
}

fun processFiles(files: List<String>) {
    matchPdf(files.filter { it.endsWith(".pdf") }).forEach(::moveOrCopy)
    matchOfx(files.filter { it.endsWith(".ofx") }).forEach(::moveOrCopy)
//    matchZip(files.filter { it.endsWith(".zip") })
//    matchXml(files.filter { it.endsWith(".xml") })
}

private fun moveOrCopy(sourceDest: Pair<String, String>) {
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