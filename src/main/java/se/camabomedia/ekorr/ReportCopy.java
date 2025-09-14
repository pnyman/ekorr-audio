package se.camabomedia.ekorr;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOExceptionList;
import org.jsoup.Jsoup;

public class ReportCopy {

    private Report report;
    private File destDir;
    private File source;
    private File destination;

    public ReportCopy(Report report, File destDir) {
        this.report = report;
        this.destDir = destDir;
        this.source = report.getReportFile();
        try {
            setDestination();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void setDestination() throws IOException {
        destination = new File(destDir, report.getReportFileName());

        // If the report don't already exist, we are done
        if (!destination.exists()) {
            return;
        }

        // Destination does exist.
        // If checksums are equal it's the same project,
        // and we keep the destination created at the beginning
        String sourceChecksum = Jsoup.parse(report.getReportFile()).select("meta[name=checksum]").attr("content");
        String destChecksum = Jsoup.parse(destination).select("meta[name=checksum]").attr("content");
        if (sourceChecksum.equals(destChecksum)) {
            return;
        }

        // Checksums are not equal, so the projects are different.
        // Create a new destination with *-1.html.
        int cnt = 1;
        destination = new File(destination.getPath()
                               .replace(".html", "-" + Integer.toString(cnt) + ".html"));

        // If this destination does not exist, we are done.
        if (!destination.exists()) {
            return;
        }

        // New destination exists, so we see if the checksums match.
        // If so, we are done.
        destChecksum = Jsoup.parse(destination).select("meta[name=checksum]").attr("content");
        if (destChecksum.equals(sourceChecksum)) {
            return;
        }

        // Checksums didn't match, we keep on trying.
        boolean tryAgain = true;
        while (tryAgain) {
            // We make a new destination, with increment
            destination = new File(destination.getPath()
                                   .replace("-" + Integer.toString(cnt) + ".html",
                                            "-" + Integer.toString(++cnt) + ".html"));

            // If this destination does not exists, we are done.
            if (!destination.exists()) {
                tryAgain = false;
                return;
            }

            // New destination exists, so we see if the checksums match.
            // If so, we are done.
            destChecksum = Jsoup.parse(destination).select("meta[name=checksum]").attr("content");
            if (sourceChecksum.equals(destChecksum)) {
                tryAgain = false;
                return;
            }
        }
    }

    public void copy() throws IOException {
        try {
            FileUtils.copyFile(source, destination);
        }
        catch (IOException e) {
            System.out.println("Could not copy " + source + " to " + destination);
            e.printStackTrace();
            throw new IOException("Kunde inte kopiera rapport", e);
        }
    }
}
