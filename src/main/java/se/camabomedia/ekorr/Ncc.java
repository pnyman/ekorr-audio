package se.camabomedia.ekorr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * ParseNcc
 */
public class Ncc {
    private Document doc;
    public File directory;
    public String title;
    public String identifier;
    public ArrayList<String> creators = new ArrayList<>();
    public ArrayList<NccItem> nccItems = new ArrayList<>();
    public ArrayList<String> smilFiles = new ArrayList<>();

    public Ncc(String nccFileString) {
        try {
            File nccFile = new File(nccFileString);
            directory = nccFile.getParentFile();
            doc = Jsoup.parse(nccFile, "UTF-8");
            setMetadata();
            getNccItems();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void setMetadata() {
        for (Element elt : doc.getElementsByTag("meta")) {
            if (elt.attr("name").equals("dc:title"))
                title = elt.attr("content");
            else if (elt.attr("name").equals("dc:identifier"))
                identifier = elt.attr("content");
            else if (elt.attr("name").equals("dc:creator"))
                creators.add(elt.attr("content"));
        }
    }

    private void getNccItems() {
        for (Element item : doc.body().children()) {

            String name = item.tagName();
            String css_class = item.className();
            String text = item.text();
            Elements link = item.getElementsByTag("a");
            String[] href = link.attr("href").split("#");
            String smil = href[0];
            String parElementId = href[1];

            nccItems.add(new NccItem(name, css_class, text, smil, parElementId));

            if (!smilFiles.contains(smil))
                smilFiles.add(smil);

        }
    }

}
