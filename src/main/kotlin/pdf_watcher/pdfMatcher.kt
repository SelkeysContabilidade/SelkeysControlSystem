package pdf_watcher

import Preferences.monitoredFolder
import database.LocalDatabase
import database.LocalDatabase.findAllDocuments
import database.LocalDatabase.findTranslation
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File

fun pdfToTxt(filePath: String) = PDDocument
    .load(File(filePath))
    .use { PDFTextStripper().getText(it).replace("\\s+".toRegex(), " ") }

fun matchPdf(files: List<String>) = files
    .filter { it.endsWith(".pdf") }
    .map { file -> Pair(file, findAllDocuments().firstNotNullOfOrNull { buildDocumentName(it, pdfToTxt(file)) }) }


fun buildDocumentName(document: LocalDatabase.Document, file: String): String? {
    if (document.identifier.toRegex() !in file) return null
    val client = LocalDatabase.findByRegistry(document.registryRegex.toRegex().find(file)?.value.orEmpty())
    var filename = ""
    var folder = document.baseFolderStructure
    document.getProceduresOrdered().forEach {
        when (it.type) {
            "clientFolder" -> folder += client?.baseFolderStructure ?: it.content
            "folderRegex" -> folder += it.content.toRegex().find(file)?.value
            "folderRegexTranslated" -> folder += findTranslation(it.content.toRegex().find(file)?.value.orEmpty())
            "folderString" -> folder += it.content
            "fileString" -> filename += it.content
            "nickname" -> filename += (client?.nickname ?: it.content) + " "
            "fileRegex" -> filename += it.content.toRegex().find(file)?.value
        }
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
                File(it.first).copyTo(File(it.second!!))
            } catch (_: Exception) {

            }
        }
    val nameZip = matchZip()
    val nameXml = matchXml()
}