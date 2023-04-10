package pdf_watcher

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File

class pdfMatcher {
    fun convertPDFToTxt(filePath: String): String =
        PDDocument
            .load(File(filePath))
            .use { PDFTextStripper().getText(it).replace("\\s+".toRegex(), " ") }
}