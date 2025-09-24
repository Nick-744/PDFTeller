import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.JFileChooser
import java.io.File

fun pickAFile(): File?
{
    val chooser = JFileChooser()
    val filter  = FileNameExtensionFilter(
        "PDF Documents", "pdf"
    )
    chooser.setFileFilter(filter)

    val returnVal       = chooser.showOpenDialog(null)
    var userFile: File? = null
    if (returnVal == JFileChooser.APPROVE_OPTION) // User selected a file!
    {
        val selected = chooser.selectedFile
        if (selected.extension.lowercase() == "pdf")
            userFile = selected // Enforce the pdf file type...
    }

    return userFile
}
