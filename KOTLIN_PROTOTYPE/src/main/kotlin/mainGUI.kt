fun main()
{
    val userFile = pickAFile()
    if (userFile == null)
    {
        println("Only PDF files are accepted!")
        return
    }

    val sentences = processPdfTextWithStructure(userFile)
    for (i in 0..10)
    {
        println(sentences[i])
        textToSpeech(sentences[i])
    }
}
