package se.camabomedia.ekorraudio.daisy;

import java.io.File;
import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * FullText
 */
public class Content {

    private Document doc;
    public File contentFile;

    public Content(File directory, String fileName) {
        contentFile = new File(directory, fileName);
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

    /**
     * Alla element med id, uppslagna en gång. doc.getElementById går igenom
     * hela dokumentet vid varje anrop – med tusentals synkpunkter i en stor
     * textfil blir det tiotals miljoner steg när boken öppnas.
     */
    private java.util.Map<String, org.jsoup.nodes.Element> ids;

    private java.util.Map<String, org.jsoup.nodes.Element> ids() {
        if (ids == null) {
            ids = new java.util.HashMap<>();
            if (doc != null) {
                for (org.jsoup.nodes.Element e : doc.getAllElements()) {
                    String id = e.id();
                    if (!id.isEmpty()) {
                        ids.putIfAbsent(id, e);   // första förekomsten, som getElementById
                    }
                }
            }
        }
        return ids;
    }

    /** Sant om dokumentet har ett element med detta id. */
    public boolean hasId(String id) {
        return ids().containsKey(id);
    }

    public String getTagText(String tagID) {
        org.jsoup.nodes.Element e = ids().get(tagID);
        return e == null ? null : e.text();
    }

}
