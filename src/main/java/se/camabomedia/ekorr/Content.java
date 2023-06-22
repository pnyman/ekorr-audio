package se.camabomedia.ekorr;

import java.io.File;
import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * FullText
 */
public class Content {

    private Document doc;

    public Content(File directory, String fileName) {
        File contentFile = new File(directory, fileName);
        try {
            doc = Jsoup.parse(contentFile, "UTF-8");
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
