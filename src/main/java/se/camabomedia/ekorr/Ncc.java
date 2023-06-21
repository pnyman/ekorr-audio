package se.camabomedia.ekorr;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Hashtable;
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
    private File directory;
    private String title;
    private String identifier;
    private String contentFile = null;
    private ArrayList<String> creators = new ArrayList<>();
    private ArrayList<NccItem> nccItems = new ArrayList<>();
    private ArrayList<String> smilFiles = new ArrayList<>();
    private Hashtable<String, NccItem> parIDTable = new Hashtable<>();
    private Hashtable<String, ArrayList<String>> breadCrumbsTable = new Hashtable<>();

    public Ncc(String nccFileString) {
        try {
            File nccFile = new File(nccFileString);
            directory = nccFile.getParentFile();
            doc = Jsoup.parse(nccFile, "UTF-8");
            setMetadata();
            findContentFile();
            populateNccItems();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void findContentFile() {
        Path path = Paths.get(directory.toString());

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(path, "*.smil")) {
            for (Path smilFile : ds) {
                if (contentFile != null) {
                    break;
                }
                Document doc = Jsoup.parse(smilFile.toFile(), "UTF-8");
                for (Element par : doc.getElementsByTag("par")) {
                    try {
                        String src = par.select("text").first().attr("src");
                        if (!src.contains("ncc.html"))
                            contentFile = src.split("#")[0];
                    }
                    catch (Exception ignore) {}
                    if (contentFile != null) {
                        break;
                    }
                }
            }
        }
        catch (IOException e) {
            System.err.println(e);
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

    private void populateNccItems() {

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
            String parID = href[1];

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
                breadCrumb.add(parID);
            }

            NccItem nccItem = new NccItem(name, cssClass, text, smil, parID, breadCrumb, thisLevel);
            nccItems.add(nccItem);

            parIDTable.put(parID, nccItem);
            breadCrumbsTable.put(smil, breadCrumb);

            previousLevel = thisLevel;

            if (!smilFiles.contains(smil))
                smilFiles.add(smil);
        }
    }

    public NccItem getNccItem(String parID) {
        return parIDTable.get(parID);
    }

    public ArrayList<NccItem> getNccItems() {
        return nccItems;
    }

    public File getDirectory() {
        return directory;
    }

    public String getTitle() {
        return title;
    }

    public String getIdentifier() {
        return identifier;
    }

    public ArrayList<String> getCreators() {
        return creators;
    }

    public String getContentFile() {
        return contentFile;
    }

    public ArrayList<String> getSmilFiles() {
        return smilFiles;
    }

    public Hashtable<String, NccItem> getParIDTable() {
        return parIDTable;
    }

    public ArrayList<String> getBreadCrumbs(String smilFile) {
        return breadCrumbsTable.get(smilFile);
    }

}
