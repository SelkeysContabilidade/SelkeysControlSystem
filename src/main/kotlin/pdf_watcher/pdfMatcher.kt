package pdf_watcher

import Preferences.monitoredFolder
import database.LocalDatabase
import database.LocalDatabase.findAllDocuments
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File

fun pdfToTxt(filePath: String): String =
    PDDocument
        .load(File(filePath))
        .use { PDFTextStripper().getText(it).replace("\\s+".toRegex(), " ") }

fun matchPdf(changedFiles: List<String>) = changedFiles
    .filter { it.endsWith(".pdf") }
    .map { file -> findAllDocuments().firstNotNullOf { matchDocument(it, pdfToTxt(file)) } }


fun matchDocument(document: LocalDatabase.Document, file: String): String? {
    if (document.identifier.toRegex() !in file) return null
    val client = LocalDatabase.findByRegistry(document.registryRegex.toRegex().find(file)?.value.orEmpty())
    var filename = ""
    var folder = document.baseFolderStructure
    document.getProceduresOrdered().forEach {
        when (it.type) {
            "clientFolder" -> folder += client?.baseFolderStructure ?: it.content
            "folderRegex" -> folder += it.content.toRegex().find(file)?.value
            "folderRegexTranslated" -> folder += translateMatch(it.content, file)
            "folderString" -> folder += it.content
            "fileString" -> filename += it.content
            "nickname" -> filename += (client?.nickname ?: it.content) + " "
            "fileRegex" -> filename += it.content.toRegex().find(file)?.value
        }
    }
    return "$monitoredFolder/" + folder + filename.replace("/".toRegex(), "_")
}

fun translateMatch(regex: String, file: String): String =
    LocalDatabase.findTranslation(regex.toRegex().find(file)?.value.orEmpty())

