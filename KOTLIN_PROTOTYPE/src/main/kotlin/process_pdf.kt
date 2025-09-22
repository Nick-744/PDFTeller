import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.Loader
import java.io.File

import opennlp.tools.tokenize.Tokenizer

fun processPdfTextWithStructure(filePath: File)
{
    val document      = Loader.loadPDF(filePath)
    val processedText = ArrayList<String>()

    val stripper = PDFTextStripper()
    for (currentPage in 1..document.numberOfPages)
    {
        stripper.startPage = currentPage
        stripper.endPage   = currentPage
        val pageText       = stripper.getText(document)
        val lines          = pageText.split('\n')

        val currentBlock = ArrayList<String>()
        for (i in 0..lines.size)
        {
            val line = lines[i]

            if (line.isEmpty()) continue

            if ((line.length < 50) && !line.endsWith('.'))
            {
                if (!currentBlock.isEmpty())
                {

                }
            }
        }
    }

    document.close()

//    return text
}
