package se.camabomedia.ekorr;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.Files;
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
    private String stub = "ReportStub.xhtml";
    private String reportDirectory = "___Korrektur-rapport";

    private File directory;
    // private Ncc ncc;
    private String reportFile;
    private String identifier;
    private String title;
    private ArrayList<String> creators;

    /**
     * Constructor
     */
    public Report(Ncc ncc) {
        this.identifier = ncc.getIdentifier();
        this.title = ncc.getTitle();
        this.creators = ncc.getCreators();

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
        reportFile = String.join("_", identifier, sanitizedTitle + ".xhtml");
    }


    public void addComment(String page, String type, String seek,
                          String wrong, String right) {
        comments.add(new Comment(page, type, seek, wrong, right));
    }

    public void createAndWriteReport() {
        // Read and parse the report stub
        URL url = getClass().getResource(stub);

        try {
            File file = new File(Paths.get(url.toURI()).toString());
            doc = Jsoup.parse(file);
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // Add metadata
        addMetadata();

        // Add the comments to the report
        for (Comment comment : comments) {
            doc.getElementById("comments").append(comment.toString());
        }

        // Print the report
        File file = new File(directory, reportFile);
        try {
            FileUtils.writeStringToFile(file, doc.outerHtml(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void addMetadata() {
        doc.getElementsByClass("date").get(0)
            .appendText(java.time.LocalDate.now().toString());
        doc.getElementsByClass("identifier").get(0)
            .appendText(identifier);
        doc.getElementsByClass("title").get(0)
            .appendText(title);
        doc.getElementsByClass("creator").get(0)
            .appendText(String.join(", ", creators));
    }

    /**
     * Reads and loads a report if it already exists.
     * @param directory  is a File object
     * @param reportFile is a String
     */
    public void readReport() {
        // If a report don't already exist, do nothing
        if (Files.notExists(Paths.get(directory.toString(), reportFile))) {
            return;
        }

        File file = new File(directory, reportFile);

        try {
            doc = Jsoup.parse(file);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String page = null;
        String type = null;
        String seek = null;
        String wrong = null;
        String right = null;

        for (Element comment : doc.getElementsByClass("comment")) {
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
            addComment(page, type, seek, wrong, right);
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
        return new File(directory, reportFile);
    }

    /**
     * Class for individual comments
     */
    class Comment {
        String page = "";
        String type = "";
        String seek = "";
        String wrong = "";
        String right = "";

        public Comment(String page, String type, String seek, String wrong, String right) {
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
            if (page == null || page == "") {
                this.page = "saknas";
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

        @Override
        public String toString() {
            String html = "<div class='comment'>"
                + "<table class='comment-table'>"
                + "<tr><td class='comment-head'>Sida:</td><td class='comment-page'>" + page
                + "<tr><td class='comment-head'>Typ:</td><td class='comment-type'>" + type
                + "</td></tr>";

            if (!seek.equals("")) {
                html += "<tr onclick=\"copyLastColumn(this)\">"
                    + "<td class='comment-head'>Sök:</td><td class='comment-seek'>"
                    + seek + "</td></tr>";
            }
            if (!wrong.equals("")) {
                html += "<tr><td class='comment-head'>Fel:</td><td class='comment-wrong'>"
                    + wrong + "</td></tr>";
            }
            if (!right.equals("")) {
                html += "<tr><td class='comment-head'>Rätt:</td><td class='comment-right'>"
                    + right + "</td></tr>";
            }

            html += "</table></div>";
            return html;
        }
    }

}
