package se.camabomedia.ekorr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        Pattern pattern = Pattern.compile("^h(\\d)");
        ArrayList<String> breadCrumb = new ArrayList<>();
        boolean heading;
        int thisLevel = 1;
        int previousLevel = 1;

        for (Element item : doc.body().children()) {
            String name = item.tagName();
            String cssClass = item.className();
            String text = item.text();
            Elements link = item.getElementsByTag("a");
            String[] href = link.attr("href").split("#");
            String smil = href[0];
            String parId = href[1];

            Matcher m = pattern.matcher(name);
            if (m.find()) {
                thisLevel = Integer.parseInt(m.group(1));
                if (thisLevel == 1) {
                    breadCrumb = new ArrayList<>();
                }
                else {
                    breadCrumb = new ArrayList<>(breadCrumb);
                    for (int i = previousLevel; i >= thisLevel; i--)
                        breadCrumb.remove(breadCrumb.size() - 1);
                }
                breadCrumb.add(parId);
            }

            NccItem nccItem = new NccItem(name, cssClass, text, smil, parId, breadCrumb, thisLevel);
            nccItems.add(nccItem);
            previousLevel = thisLevel;

            if (!smilFiles.contains(smil))
                smilFiles.add(smil);

        }
    }

}
