package se.camabomedia.ekorraudio.report;

import se.camabomedia.ekorraudio.daisy.CompoundChecksum;
import se.camabomedia.ekorraudio.daisy.Ncc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Report {

    private Document doc;
    private ArrayList<Comment> comments = new ArrayList<>();
    private final String reportDirectory = "___Korrektur-rapport";

    private File directory;
    private File projectDirectory;
    private Ncc ncc;
    private String reportFileName;
    private String identifier;
    private String title;
    private String narrator;
    private String totalTime;
    private ArrayList<String> creators;
    private String proofreader;
    private String checksum;

    /**
     * Läser en befintlig rapport, om det finns en. Ingenting skrivs till
     * disk – mappen och rapportfilen skapas först av
     * {@link #createAndWriteReport()}, när en kommentar sparas.
     */
    public Report(Ncc ncc) throws URISyntaxException, IOException {
        this.ncc = ncc;
        this.identifier = ncc.getIdentifier();
        this.title = ncc.getTitle();
        this.creators = ncc.getCreators();
        this.narrator = ncc.getNarrator();
        this.totalTime = ncc.getTotalTime();
        this.projectDirectory = ncc.getDirectory();

        directory = new File(projectDirectory, reportDirectory);

        makeReportFileName();
        readReport();
    }


    private void makeReportFileName() {
        String temp;
        // # % & {} \ <> * ? / $ ! ' " : @+ ` | =
        // OBS: namnet måste bli exakt som i tidigare versioner (även ekorr),
        // annars hittas befintliga rapporter inte. Ändra inte reglerna.
        String sanitizedTitle = (title == null ? "" : title)
            .replaceAll(" ", "_")
            .replaceAll("[^a-zA-ZåäöÅÄÖ0-9_]", "")
            .replaceAll("__", "_");

        temp = String.join("_", identifier == null ? "" : identifier, sanitizedTitle);
        temp = StringUtils.substring(temp, 0, 50); // max filename length is 255
        temp = temp.replace("_$", "");   // gör inget (inte regex), men behålls för kompatibilitet
        reportFileName = temp + "_KORR.html";
    }


    // public void addComment(String nr, String page, String type, String seek,
    //                       String wrong, String right) {
    //     comments.add(new Comment(nr, page, type, seek, wrong, right));
    // }

    public void addComment(String nr, String page, String type, String seek,
                           String wrong, String right, String base64EncodedAudio) {
        comments.add(new Comment(nr, page, type, seek, wrong, right, base64EncodedAudio));
    }


    /** Skapar rapportmappen vid behov och skriver rapporten med alla kommentarer. */
    public void createAndWriteReport() throws URISyntaxException, IOException {
        try {
            Files.createDirectories(directory.toPath());
        }
        catch (IOException e) {
            throw new IOException("Kunde inte skapa mapp:\n" + directory, e);
        }

        if (checksum == null) {
            checksum = getChecksum();
        }

        // Läs mallen direkt från resursen
        try (InputStream inputStream = getClass().getResourceAsStream("/se/camabomedia/ekorraudio/ReportStub.html")) {
            if (inputStream == null) {
                throw new IOException("Hittar inte rapportmallen ReportStub.html");
            }
            doc = Jsoup.parse(inputStream, "UTF-8", "");
        }
        addMetadata();

        int nr = 0;
        boolean showSeek = !ncc.isAudioOnly();   // ingen text att söka i
        for (Comment comment : comments) {
            comment.setNr(Integer.toString(++nr));
            comment.setShowSeek(showSeek);
            doc.getElementById("comments").append(comment.toString());
        }

        // Print the report
        File file = new File(directory, reportFileName);
        FileUtils.writeStringToFile(file, doc.outerHtml(), StandardCharsets.UTF_8);
    }

    /** Om det finns en sparad rapportfil för boken. */
    public boolean exists() {
        return getReportFile().isFile();
    }


    /**
     * Sets the metadata in the report file.
     */
    private void addMetadata() {
        doc.getElementsByClass("date").get(0)
            .appendText(java.time.LocalDate.now().toString());
        doc.getElementsByClass("identifier").get(0)
            .appendText(identifier);
        doc.getElementsByClass("title").get(0)
            .appendText(title);
        doc.getElementsByClass("creator").get(0)
            .appendText(String.join(", ", creators));
        if (narrator != null) {
            doc.getElementsByClass("narrator").get(0)
                .appendText("Inläsning: " + narrator)
                .appendElement("br");
        }
        if (proofreader != null) {
            doc.getElementsByClass("proofreader").get(0)
                .appendText("Korrektur: " + proofreader)
                .appendElement("br");
        }
        doc.getElementsByClass("nr-of-comments").get(0)
            .appendText("Antal kommentarer: " + getNumberOfComments())
            .appendElement("br");
        if (totalTime != null) {
            doc.getElementsByClass("total-time").get(0)
                .appendText("Bokens längd: " + totalTime);
        }

        // Check to see if the metadata tag exists
        // (for reports made with earlier versions of the program)
        Elements cs = doc.select("meta[name=checksum]");
        if (cs.isEmpty()) {
            doc.selectFirst("head").child(0).before("<meta name='checksum' content=''>");
        }
        doc.select("meta[name=checksum]").attr("content", checksum);
    }


    private String getChecksum() {
        String cs = null;
        try {
            cs = CompoundChecksum.checksum(projectDirectory.getPath(), ".smil");
        }
        catch (NoSuchAlgorithmException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return cs;
    }


    /**
     * Reads and loads a report if it already exists.
     * @param directory  is a File object
     * @param reportFileName is a String
     */
    public void readReport() {
        // If a report don't already exist, do nothing
        if (Files.notExists(Paths.get(directory.toString(), reportFileName))) {
            return;
        }

        File file = new File(directory, reportFileName);

        try {
            doc = Jsoup.parse(file, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        for (Element comment : doc.getElementsByClass("comment")) {
            String nr = "";
            String page = "";
            String type = "";
            String seek = "";
            String wrong = "";
            String right = "";
            String audio = null;
            String bookTime = "";
            String headingTime = "";
            String event = "";
            String heading = "";

            for (Element elt : comment.getElementsByClass("comment-nr")) {
                nr = elt.text().replaceAll("\\.$", "");
            }
            for (Element elt : comment.getElementsByClass("comment-page")) {
                page = elt.text();
            }
            for (Element elt : comment.getElementsByClass("comment-type")) {
                type = elt.text();
            }
            for (Element elt : comment.getElementsByClass("comment-seek")) {
                seek = elt.text();
            }
            for (Element elt : comment.getElementsByClass("comment-wrong")) {
                wrong = elt.text();
            }
            for (Element elt : comment.getElementsByClass("comment-right")) {
                right = elt.text();
            }
            for (Element elt : comment.getElementsByClass("comment-audio")) {
                audio = elt.child(0).child(0).attr("src");
                // src = data:audio/wav;base64,UklGRkJpAABXQVZFZm10IB...
                audio = audio.replace("data:audio/wav;base64,", "");
            }
            for (Element elt : comment.getElementsByClass("comment-booktime")) {
                bookTime = elt.text();
            }
            for (Element elt : comment.getElementsByClass("comment-headingtime")) {
                headingTime = elt.text();
            }
            for (Element elt : comment.getElementsByClass("comment-event")) {
                event = elt.text();
            }
            for (Element elt : comment.getElementsByClass("comment-heading")) {
                heading = elt.text();
            }
            addComment(nr, page, type, seek, wrong, right, audio);
            comments.get(comments.size() - 1)
                .setPosition(new Position(bookTime, headingTime, event, heading));
        }
    }

    /**
     * Lägger in en ny kommentar på rätt plats efter boktid: efter alla som
     * gjorts tidigare i boken (eller på samma ställe) och före den första som
     * gjorts senare. Ordningen bland de befintliga ändras inte, så en
     * ordning som gjorts för hand (Alt+pil) behålls.
     *
     * Kommentarer utan boktid (från äldre rapporter) räknas som om de låg
     * där kommentaren före dem ligger. En ny kommentar utan boktid hamnar sist.
     */
    public void insertInOrder(Comment comment) {
        long key = comment.getBookTimeMs();
        if (key < 0) {
            comments.add(comment);
            return;
        }
        int insertAt = comments.size();
        long previous = -1;
        for (int i = 0; i < comments.size(); i++) {
            long k = comments.get(i).getBookTimeMs();
            if (k < 0) {
                k = previous;
            }
            else {
                previous = k;
            }
            if (k > key) {
                insertAt = i;
                break;
            }
        }
        comments.add(insertAt, comment);
    }

    public void removeComment(Comment comment) {
        comments.remove(comment);
    }

    public void replaceComment(Comment oldComment, Comment newComment) {
        int index = comments.indexOf(oldComment);
        comments.set(index, newComment);
    }

    public void setComments(ArrayList<Comment> comments) {
        this.comments = comments;
    }

    public ArrayList<Comment> getComments() {
        return comments;
    }

    public File getReportDirectory() {
        return directory;
    }

    public File getReportFile() {
        return new File(directory, reportFileName);
    }

    public String getReportFileName() {
        return reportFileName;
    }

    public String getNccFile() {
        return ncc.getNccFile().toString();
    }

    public void setProofreader(String proofreader) {
        this.proofreader = proofreader;
    }

    public String getNumberOfComments() {
        return Integer.toString(comments.size());
    }

    @Override
    public String toString() {
        return "Report [" + reportFileName + ", " + comments.size() + " kommentarer]";
    }

}
