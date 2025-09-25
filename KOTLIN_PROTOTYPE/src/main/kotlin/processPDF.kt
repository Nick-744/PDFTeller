import opennlp.tools.sentdetect.SentenceDetectorME
import opennlp.tools.sentdetect.SentenceModel
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File

fun processPdfTextWithStructure(filePath: File): ArrayList<String>
{
    val document      = Loader.loadPDF(filePath)
    val processedText = ArrayList<String>()

    val stripper = PDFTextStripper()

    // IntelliJ in debug mode... creating a File(...) works fine!
    // val model = SentenceModel(File("C:/Users/nick1/Documents/..."))

    // In a JAR [packaged project], resources aren’t files [compressed entries]...
    val modelStream = object {}.javaClass.getResourceAsStream("/opennlp-en-ud-ewt-sentence-1.3-2.5.4.bin")
    val model       = SentenceModel(modelStream)
    val tokenizer   = SentenceDetectorME(model)

    for (currentPage in 1..document.numberOfPages)
    {
        // Get lines from the page
        stripper.startPage = currentPage
        stripper.endPage   = currentPage
        val pageText       = stripper.getText(document)
        val lines          = pageText.split(" \r\n", " \n", "\r\n", "\n").toTypedArray()

        val currentBlock = ArrayList<String>() // To accumulate lines of regular text!
        for (line in lines)
        {
            line.trimMargin()

            // STUPID PDFs sometimes have empty lines...
            if (line.isEmpty()) continue

            // Check if this might be a chapter/section header!
            if ((line.length < 50) && !line.endsWith("."))
            {
                // --- <> Process accumulated text <> --- //
                if (!currentBlock.isEmpty())
                {
                    val blockText = currentBlock.joinToString(" ")
                    val sentences = tokenizer.sentDetect(blockText)
                    processedText.addAll(sentences)
                    currentBlock.clear()
                }

                // Add the header as its own element...
                processedText.add(line)
            }
            else currentBlock.add(line) // Will be processed later
        }

        // Process any remaining text
        if (!currentBlock.isEmpty())
        {
            val blockText = currentBlock.joinToString(" ")
            val sentences = tokenizer.sentDetect(blockText)
            processedText.addAll(sentences)
        }
    }

    document.close()

    return processedText
}
