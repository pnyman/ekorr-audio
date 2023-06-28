package se.camabomedia.ekorr;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
    private ArrayList<Remark> remarks = new ArrayList<>();
    private String stub = "ReportStub.xhtml";
    private String reportDirectory = "___Korrektur_rapport";

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
        reportFile = String.join("_", ncc.getIdentifier(),
                                 ncc.getTitle().replaceAll(" ", "_") + ".xhtml");
        creators = ncc.getCreators();
        directory = new File(ncc.getDirectory(), reportDirectory);
        directory.mkdirs();
    }

    private void setMetadata() {
        doc.getElementsByClass("date").get(0)
            .appendText(java.time.LocalDate.now().toString());
        doc.getElementsByClass("identifier").get(0)
            .appendText(identifier);
        doc.getElementsByClass("title").get(0)
            .appendText(title);
        doc.getElementsByClass("creator").get(0)
            .appendText(String.join(", ", creators));
    }

    public Document getDoc() {
        return doc;
    }

    public void addRemark(String page, String type, String seek,
                          String wrong, String right) {
        remarks.add(new Remark(page, type, seek, wrong, right));
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

        // Add metadat
        setMetadata();

        // Add the remarks to the report
        for (Remark remark : remarks) {
            doc.getElementById("comments").append(remark.toString());
        }

        // Print the report
        File file = new File(directory, reportFile);
        System.out.println(file);
        try {
            FileUtils.writeStringToFile(file, doc.outerHtml(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void readReport() {
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

        // Element content = doc.getElementById("content");

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
            addRemark(page, type, seek, wrong, right);
        }
    }

    /**
     * Class for individual remarks
     */
    class Remark {
        String page;
        String type;
        String seek;
        String wrong;
        String right;

        public Remark(String page, String type, String seek, String wrong, String right) {
            this.page = page;
            this.type = type;
            this.seek = seek;
            this.wrong = wrong;
            this.right = right;
        }

        @Override
        public String toString() {
            String html = "<div class='comment'>"
                + "<table class='comment-table'>"
                + "<tr><td class='comment-head'>Sida:</td><td class='comment-page'>" + page
                + "<tr><td class='comment-head'>Typ:</td><td class='comment-type'>" + type
                + "</td></tr>";

            if (seek != null) {
                html += "<tr><td class='comment-head'>Sök:</td><td class='comment-seek'>"
                    + seek + "</td></tr>";
            }
            if (wrong != null) {
                html += "<tr><td class='comment-head'>Fel:</td><td class='comment-wrong'>"
                    + wrong + "</td></tr>";
            }
            if (right != null) {
                html += "<tr><td class='comment-head'>Rätt:</td><td class='comment-right'>"
                    + right + "</td></tr>";
            }

            html += "</table></div>";
            return html;
        }
    }

}
