package se.camabomedia.ekorraudio.daisy;

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
    private File nccFile;
    private File directory;
    private String identifier;
    private String title;
    private String narrator;
    private String totalTime;
    private String contentFile = null;
    /** ncc:multimediaType, t.ex. "audioFullText" eller "audioNcc". */
    private String multimediaType = null;
    /**
     * Sant för en talbok utan textfil (bara ncc och smil, t.ex. "audioNcc").
     * Smil-filernas text-element pekar då på ncc.html, och NCC:n används
     * som innehåll.
     */
    private boolean audioOnly = false;
    private ArrayList<String> creators = new ArrayList<>();
    private ArrayList<NccItem> nccItems = new ArrayList<>();
    private ArrayList<String> smilFiles = new ArrayList<>();
    private Hashtable<String, NccItem> parIDTable = new Hashtable<>();
    private Hashtable<String, ArrayList<String>> breadCrumbsTable = new Hashtable<>();
    private ArrayList<String> pages = new ArrayList<>();

    public Ncc(String nccFileString) {
        try {
            File nccFile = new File(nccFileString);
            this.nccFile = nccFile;
            directory = nccFile.getParentFile();
            doc = Jsoup.parse(nccFile, "UTF-8");
            pages.add("n/a");
            setMetadata();
            findContentFile();
            populateNccItems();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public Ncc(File nccFile) {
        try {
            this.nccFile = nccFile;
            directory = nccFile.getParentFile();
            doc = Jsoup.parse(nccFile, "UTF-8");
            pages.add("n/a");
            setMetadata();
            findContentFile();
            populateNccItems();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Letar upp textfilen via smil-filernas text-element. Finns ingen (alla
     * pekar på ncc.html) är det en talbok utan text – då blir NCC:n
     * innehållet. För typerna utan text i ncc:multimediaType letas inte alls,
     * så att inte alla smil-filer läses i onödan.
     */
    private void findContentFile() {
        if (!isTextlessType(multimediaType)) {
            searchContentFile();
        }
        if (contentFile == null) {
            audioOnly = true;
            contentFile = nccFile.getName();
        }
    }

    private static boolean isTextlessType(String type) {
        return type != null
            && (type.equalsIgnoreCase("audioNcc") || type.equalsIgnoreCase("audioOnly"));
    }

    private void searchContentFile() {
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
                        String file = src.split("#")[0];
                        if (!file.isEmpty() && !file.equalsIgnoreCase(nccFile.getName()))
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
            else if (elt.attr("name").equals("ncc:totalTime"))
                totalTime = elt.attr("content");
            else if (elt.attr("name").equals("ncc:narrator"))
                narrator = elt.attr("content");
            else if (elt.attr("name").equals("ncc:multimediaType"))
                multimediaType = elt.attr("content").strip();
        }
    }

    private void populateNccItems() {

    // <span class="page-normal" id="page-68">
        Pattern headingPattern = Pattern.compile("^h(\\d)");
        ArrayList<String> breadCrumb = new ArrayList<>();
        int thisLevel = 1;
        int previousLevel = 1;
        String page = null;

        for (Element item : doc.body().children()) {
            String name = item.tagName();
            String cssClass = item.className();
            String text = item.text();
            Elements link = item.getElementsByTag("a");
            String[] href = link.attr("href").split("#");
            String smil = href[0];
            String parID = href[1];

            if (item.hasClass("page-normal")
                || item.hasClass("page-front")
                || item.hasClass("page-special")) {
                page = item.text();
                pages.add(page);
            }

            Matcher m = headingPattern.matcher(name);
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

            NccItem nccItem = new NccItem(name, cssClass, text, smil, parID,
                                          breadCrumb, thisLevel,page);
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

    public File getNccFile() {
        return nccFile;
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

    /** Sant om talboken saknar textfil – innehållet är då NCC:n. */
    public boolean isAudioOnly() {
        return audioOnly;
    }

    public String getMultimediaType() {
        return multimediaType;
    }

    public File getContentFilePath() {
        return new File(directory, contentFile);
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

    public ArrayList<String> getPages() {
        return pages;
    }

    public String getTotalTime() {
        return totalTime;
    }

    public String getNarrator() {
        return narrator;
    }

}
