package se.camabomedia.ekorr;

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
    private String base64EncodedAudio;

    // private final String ENDASH = "–";

    /**
     * Constructor
     */
    public Report(Ncc ncc) {
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
        directory.mkdirs();
        checksum = getChecksum();
        try {
            createAndWriteReport();
        } catch (URISyntaxException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void makeReportFileName() {
        String temp;
        // # % & {} \ <> * ? / $ ! ' " : @+ ` | =
        String sanitizedTitle = title
            .replaceAll(" ", "_")
            .replaceAll("\\W", "")
            .replaceAll("__", "_");
        temp = String.join("_", identifier, sanitizedTitle);
        temp = StringUtils.substring(temp, 0, 50); // max filename length is 255
        temp = temp.replace("_$", "");
        reportFileName = temp + ".html";
    }


    public void addComment(String nr, String page, String type, String seek,
                          String wrong, String right) {
        comments.add(new Comment(nr, page, type, seek, wrong, right));
    }

    public void createAndWriteReport() throws URISyntaxException, IOException {
        InputStream inputStream = getClass().getResourceAsStream("/se/camabomedia/ekorr/ReportStub.html");
        File tempFile = File.createTempFile("reportstub", ".html");

        // Write the InputStream contents to the temporary file
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
        inputStream.close();

        // Read and parse the report stub
        doc = Jsoup.parse(tempFile);
        addMetadata();

        int nr = 0;
        for (Comment comment : comments) {
            comment.setNr(Integer.toString(++nr));
            doc.getElementById("comments").append(comment.toString());
        }

        // Print the report
        File file = new File(directory, reportFileName);
        FileUtils.writeStringToFile(file, doc.outerHtml(), StandardCharsets.UTF_8);

        // Delete the tempfile
        tempFile.delete();
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
            doc = Jsoup.parse(file);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        for (Element comment : doc.getElementsByClass("comment")) {
            String nr = "";
            String page = "";
            String type = "";
            String seek = "";
            String wrong = "";
            String right = "";

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
            addComment(nr, page, type, seek, wrong, right);
        }
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
        for (Comment comment : comments) {
            System.out.println("[ "
                               + "nr: " + comment.getNr() + " | "
                               + "page: " + comment.getPage() + " | "
                               + "type: " + comment.getType() + " | "
                               + "seek: " + comment.getSeek() + " | "
                               + "wrong: " + comment.getWrong() + " | "
                               + "right: " + comment.getRight() + " "
                               + "]");
        }
        return null;
    }

}
