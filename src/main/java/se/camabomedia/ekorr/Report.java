package se.camabomedia.ekorr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Report
 */
public class Report {

    private Document doc;
    private ArrayList<Comment> comments = new ArrayList<>();
    private String reportDirectory = "___Korrektur-rapport";

    private File directory;
    private Ncc ncc;
    private String reportFileName;
    private String identifier;
    private String title;
    private String narrator;
    private String totalTime;
    private ArrayList<String> creators;
    private String proofreader;
    private final String ENDASH = "–";

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

        directory = new File(ncc.getDirectory(), reportDirectory);

        makeReportFileName();
        readReport();
        directory.mkdirs();
    }

    private void makeReportFileName() {
        // # % & {} \ <> * ? / $ ! ' " : @+ ` | =
        String sanitizedTitle = title
            .replaceAll(" ", "_")
            .replaceAll("\\W", "")
            .replaceAll("__", "_");
        reportFileName = String.join("_", identifier, sanitizedTitle + ".html");
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
        if (totalTime != null) {
            doc.getElementsByClass("total-time").get(0)
                .appendText("Tid: " + totalTime);
        }
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

        String nr = null;
        String page = null;
        String type = null;
        String seek = null;
        String wrong = null;
        String right = null;

        for (Element comment : doc.getElementsByClass("comment")) {
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



    //------------------------------------------------------------------------------------------------------------

    /**
     * Class for individual comments
     */
    class Comment {
        String commentNr = "";
        String page = "";
        String type = "";
        String seek = "";
        String wrong = "";
        String right = "";

        public Comment(String commentNr, String page, String type, String seek, String wrong, String right) {
            if (commentNr != null)
                this.commentNr = commentNr;
            makePageNumber(page);
            if (type != null)
                this.type = sanitize(type);
            if (seek != null)
                this.seek = sanitize(seek);
            if (wrong != null)
                this.wrong = sanitize(wrong);
            if (right != null)
                this.right = sanitize(right);
        }

        private void makePageNumber(String page) {
            if (page == null || page.strip() == "") {
                this.page = ENDASH;
            }
            else {
                this.page = sanitize(page);
            }
        }

        private String sanitize(String s) {
            return s
                .replaceAll("&", "&amp")
                .replaceAll("<", "&lt;")
                .trim();
        }

        public String getPage() {
            return page;
        }

        public String getType() {
            return type;
        }

        public String getSeek() {
            return seek;
        }

        public String getWrong() {
            return wrong;
        }

        public String getRight() {
            return right;
        }

        public void setNr(String nr) {
            commentNr = nr;
        }

        public String getNr() {
            return commentNr;
        }


        @Override
        public String toString() {
            String html = "<div class='comment'><table class='comment-table'><tbody>";

            html += "<tr><th class='comment-nr'>" + commentNr + ".</th><th class='comment-type'>" + type + "</th></tr>";
            // html += "<tr><td class='comment-head'>Typ:</td><td class='comment-type'>" + type + "</td></tr>";

            html += "<tr><td></td><td><table class='inner-table'><tbody>";

            if (!wrong.equals("")) {
                html += "<tr><td class='comment-head'>Fel:</td><td class='comment-wrong'>" + wrong + "</td></tr>";
            }

            if (!right.equals("")) {
                html += "<tr><td class='comment-head'>Rätt:</td><td class='comment-right'>" + right + "</td></tr>";
            }

            html += "<tr><td class='comment-head'>Sida:</td><td class='comment-page'>" + page + "</td></tr>";

            if (!seek.equals("")) {
                html += "<tr onclick=\"copyLastColumn(this)\">"
                    + "<td class='comment-head'>Sök:</td><td class='comment-seek'>"
                    + seek + "</td></tr>";
            }

            // Close elements....
            html += "</tbody></table></td></tr>";
            // Extra tr for spacing when pasted into Outlook
            html += "<tr><td>&nbsp;</td><td>&nbsp;</td></tr>";
            // Close rest of elements
            html += "</tbody></table></div>";
            return html;
        }
    }

}
