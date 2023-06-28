package se.camabomedia.ekorr;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Report
 */
public class Report {

    private File directory;
    private Document doc;

    public Report(File directory) {
        this.directory = directory;
        String f = "ReportStub.xhtml";
        URL url = getClass().getResource(f);

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

        setDate();
    }

    private void setDate() {
        Element elt = doc.getElementsByClass("date").get(0);
        elt.appendText(java.time.LocalDate.now().toString());
    }

    public Document getDoc() {
        return doc;
    }

    public void addRemark(String page, String noteType, String seek,
                               String wrong, String right) {

        String html = "<div class='comment'>"
            + "<table class='comment-table'>"
            + "<tr><td class='comment-head'>Sida:</td><td class='comment-page'>" + page
            + "<tr><td class='comment-head'>Typ:</td><td class='comment-type'>" + noteType
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
        doc.getElementById("comments").append(html);
    }

    public void writeReport() {
        final File f = new File(directory, "filename.html");
        try {
            FileUtils.writeStringToFile(f, doc.outerHtml(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


}
