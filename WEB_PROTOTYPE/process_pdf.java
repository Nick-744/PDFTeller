import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ProcessPdfText {
    
    private static SentenceDetectorME sentenceDetector;
    
    // Initialize sentence detector (equivalent to importing sent_tokenize)
    static {
        try (InputStream modelIn = new FileInputStream("en-sent.bin")) {
            SentenceModel model = new SentenceModel(modelIn);
            sentenceDetector = new SentenceDetectorME(model);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static List<String> processPdfTextWithStructure(String filename) throws IOException {
        PdfReader doc = new PdfReader(filename);
        List<String> processedText = new ArrayList<>();
        
        try {
            for (int pageNum = 1; pageNum <= doc.getNumberOfPages(); pageNum++) {
                // Get lines from the page
                String pageText = PdfTextExtractor.getTextFromPage(doc, pageNum);
                String[] lines = pageText.split("\n");
                
                List<String> currentBlock = new ArrayList<>(); // To accumulate lines of regular text!
                for (String line : lines) {
                    line = line.trim();
                    
                    // STUPID PDFs sometimes have empty lines...
                    if (line.isEmpty()) {
                        continue;
                    }
                    
                    // Check if this might be a chapter/section header!
                    if (line.length() < 50 && !line.endsWith(".")) {
                        // --- <> Process accumulated text <> --- #
                        if (!currentBlock.isEmpty()) {
                            String blockText = String.join(" ", currentBlock);
                            String[] sentences = sentDetect(blockText);
                            for (String sentence : sentences) {
                                processedText.add(sentence);
                            }
                            currentBlock.clear();
                        }
                        
                        // Add the header as its own element...
                        processedText.add(line);
                    } else {
                        currentBlock.add(line); // Will be processed later
                    }
                }
                
                // Process any remaining text
                if (!currentBlock.isEmpty()) {
                    String blockText = String.join(" ", currentBlock);
                    String[] sentences = sentDetect(blockText);
                    for (String sentence : sentences) {
                        processedText.add(sentence);
                    }
                }
            }
        } finally {
            doc.close();
        }
        
        return processedText;
    }
    
    // Helper method to tokenize sentences (equivalent to sent_tokenize)
    private static String[] sentDetect(String text) {
        if (sentenceDetector != null) {
            return sentenceDetector.sentDetect(text);
        }
        // Fallback: simple sentence splitting if model not loaded
        return text.split("(?<=[.!?])\\s+");
    }
    
    public static void main(String[] args) {
        try {
            List<String> processedText = processPdfTextWithStructure("Understanding_Climate_Change.pdf");
            for (int i = 0; i < processedText.size(); i++) {
                System.out.println((i + 1) + ": " + processedText.get(i));
                if (i >= 20) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
