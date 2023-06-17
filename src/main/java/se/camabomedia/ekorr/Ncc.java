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
    public String title;
    public String identifier;
    public ArrayList<String> creators = new ArrayList<>();
    public ArrayList<NccItem> nccItems = new ArrayList<>();

    public Ncc(String ncc_file) {
        try {
            doc = Jsoup.parse(new File(ncc_file), "UTF-8");
            setMetadata();
            getNccItems();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    // <meta content="ULF BILTING" name="dc:creator"/>
    // <meta name="dc:title" content="Vägen till C"/>
    // <meta name="dc:identifier" content="V005414"/>

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
            String href = link.attr("href");
            String[] s = href.split("#");
            String smil = s[0];
            String fragment = s[1];

            nccItems.add(new NccItem(name, css_class, text, smil, fragment));
        }
    }

}
