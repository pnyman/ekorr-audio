package se.camabomedia.ekorr;

import java.io.File;
import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * FullText
 */
public class FullText {

    private Document doc;

    public FullText(File directory, String fileName) {
        File fullTextFile = new File(directory, fileName);
        try {
            doc = Jsoup.parse(fullTextFile, "UTF-8");
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public Document getDoc() {
        return doc;
    }

    public String getTagText(String tagID) {
        return doc.getElementById(tagID).text();
    }

}
