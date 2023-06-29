package pdf_watcher

import Preferences.monitoredFolder
import Preferences.moveFiles
import Preferences.moveUnknownClients
import Preferences.useSecondaryStorage
import database.LocalDatabase
import database.LocalDatabase.findAllDocuments
import database.LocalDatabase.findTranslation
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File
import java.io.FileWriter
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.io.path.Path
import kotlin.io.path.createTempFile

val emptySpaces = "[\r\n\t\\s  ]+".toRegex()

fun buildDocumentName(document: LocalDatabase.Document, content: String, fileName: String): String? {
    if (document.secondaryStorage && !useSecondaryStorage) return null
    if (document.identifier.toRegex() !in content) return null
    var registry = document.registryRegex.toRegex().find(content)?.value.orEmpty()
    val client = LocalDatabase.findByRegistry(registry)
    if (client == null && !moveUnknownClients) return null

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
            "removeFromString" -> {
                if (it.isFolder) folder = folder.replace(it.content.toRegex(), "")
                else filename = filename.replace(it.content.toRegex(), "")
                ""
            }

            else -> ""
        }
        if (it.isFolder) folder += name else filename += name
    }
    return "$monitoredFolder/" + folder + filename.replace("/".toRegex(), "_")
}

fun processFiles(files: List<String>, split: Boolean = true): List<String> {
    val documents = findAllDocuments()
    val mutable = mutableSetOf<String>()
    files.stream().parallel().forEach { filename ->
        try {
            when (File(filename).extension.lowercase(Locale.getDefault())) {
                "pdf" -> processPDF(filename, documents)
                "ofx" -> processOFX(filename, documents)
                "xml" -> processXML(filename, documents, split)
                "zip" -> unzipFolder(filename)
            }
        } catch (_: Exception) {
            mutable.add(filename)
        }
    }
    return mutable.toList()
}

fun processOFX(filename: String, documents: List<LocalDatabase.Document>) {
    val element = File(filename).readText().replace(emptySpaces, " ")
    val destination = documents.mapNotNull { buildDocumentName(it, element, filename) }
    moveOrCopy(filename, destination)
}

fun processPDF(filename: String, documents: List<LocalDatabase.Document>) {
    val element = PDDocument.load(File(filename))
        .use { PDFTextStripper().getText(it) }
        .replace(emptySpaces, " ")
    val destination = documents.mapNotNull { buildDocumentName(it, element, filename) }
    moveOrCopy(filename, destination)
}

fun processXML(filename: String, documents: List<LocalDatabase.Document>, toSplit: Boolean) {
    var splits = false
    val content = File(filename).readText().replace(emptySpaces, " ")

    val destination = documents.mapNotNull { document ->
        if (document.identifier.toRegex() !in content) {
            return@mapNotNull null
        }
        if (document.getProceduresOrdered()[0].type == "split" && toSplit) {
            splits = true
            processFiles(splitXml(filename, document.getProceduresOrdered()[0]), false)
            return@mapNotNull null
        }
        buildDocumentName(document, content, filename)
    }
    if (moveFiles && splits) File(filename).delete()
    else moveOrCopy(filename, destination)
}

fun splitXml(filename: String, procedure: LocalDatabase.Procedure): List<String> {
    val xmlDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(filename))
    val splitTag = procedure.content

    var node = xmlDocument.firstChild
    val cloneRoot = node.cloneNode(false)
    var clone = cloneRoot

    while ((node != null) && (node.nodeName != splitTag)) {
        clone.appendChild(node.cloneNode(false))
        node = node.firstChild
        clone = clone.firstChild
    }
    //go back to one above the splitting tag, so the splitting tag is the first child
    node = node.parentNode

    val fileList = mutableListOf<String>()

    var file = createTempFile(suffix = ".xml").toFile()
    val transformer = TransformerFactory.newInstance().newTransformer()
    clone.appendChild(node.removeChild(node.firstChild))
    transformer.transform(DOMSource(cloneRoot), StreamResult(FileWriter(file)))
    fileList.add(file.absolutePath)

    for (i in 0 until node.childNodes.length) {
        file = createTempFile(suffix = ".xml").toFile()
        clone.replaceChild(node.removeChild(node.firstChild), clone.firstChild)
        transformer.transform(DOMSource(cloneRoot), StreamResult(FileWriter(file)))
        fileList.add(file.absolutePath)
    }
    return fileList
}

fun unzipFolder(filePath: String) {
    val files = FileSystems
        .newFileSystem(Paths.get(filePath), emptyMap<String, Any>())
        .use { fs ->
            fs.rootDirectories
                .flatMap { root ->
                    Files.walk(root).toList()
                        .filter(Files::isRegularFile)
                        .map { Files.copy(it, Path("$monitoredFolder/${it.fileName}")).toString() }
                }
        }
    processFiles(files)
    if (moveFiles) File(filePath).delete()
}

fun moveOrCopy(source: String, destination: List<String>) {
    if (destination.isEmpty()) throw Exception()
    destination.forEach { File(source).copyTo(File(it)) }
    if (moveFiles) File(source).delete()
}