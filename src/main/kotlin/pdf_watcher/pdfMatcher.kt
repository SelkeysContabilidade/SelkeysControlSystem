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

fun pdfToTxt(filePath: String) = PDDocument
    .load(File(filePath))
    .use { PDFTextStripper().getText(it).replace("\\s+".toRegex(), " ") }

fun matchPdf(files: List<String>) = files
    .filter { it.endsWith(".pdf") }
    .map { file ->
        val txtFile = pdfToTxt(file)
        Pair(file, findAllDocuments().firstNotNullOfOrNull { buildDocumentName(it, txtFile) })
    }


fun buildDocumentName(document: LocalDatabase.Document, file: String): String? {
    if (document.identifier.toRegex() !in file) return null
    val registry = document.registryRegex.toRegex().find(file)?.value.orEmpty().replace("[^0-9]".toRegex(), "")
    val client = LocalDatabase.findByRegistry(registry)
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
    val renamedPdf = matchPdf(files)
    renamedPdf
        .filter { it.second != null }
        .forEach {
            try {
                if (moveFiles) {
                    Files.createDirectories(Paths.get(File(it.second!!).parent))
                    File(it.first).renameTo(File(it.second!!))
                } else File(it.first).copyTo(File(it.second!!))
            } catch (_: Exception) {
                println(it.first)
            }
        }
    val nameZip = matchZip()
    val nameXml = matchXml()
}