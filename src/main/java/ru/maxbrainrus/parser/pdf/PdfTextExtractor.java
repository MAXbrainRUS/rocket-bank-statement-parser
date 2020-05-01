package ru.maxbrainrus.parser.pdf;

import com.itextpdf.text.pdf.PdfReader;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.itextpdf.text.pdf.parser.PdfTextExtractor.getTextFromPage;

@Slf4j
public class PdfTextExtractor {
    /**
     * Get text content from pdf file
     *
     * @param fileName name of file
     * @return List of text content per page
     */
    public static List<String> getTextFromPdf(String fileName) {
        try (AutoClosablePdfReader reader = new AutoClosablePdfReader(fileName)) {
            int numberOfPages = reader.getNumberOfPages();
            List<String> res = new ArrayList<>(numberOfPages);
            for (int i = 1; i <= numberOfPages; i++) {
                String textFromPage = getTextFromPage(reader, i);
                res.add(textFromPage);
            }
            return res;
        } catch (IOException e) {
            log.error("An error occurred while read pdf source report.", e);
            throw new RuntimeException(e);
        }
    }

    private static class AutoClosablePdfReader extends PdfReader implements AutoCloseable {
        public AutoClosablePdfReader(String filename) throws IOException {
            super(filename);
        }
    }
}