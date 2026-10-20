package se.camabomedia.ekorraudio.report;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import se.camabomedia.ekorraudio.daisy.Ncc;

public class ReportRoundTripTest {

    private Path dir;
    private Ncc ncc;

    @Before
    public void makeBook() throws Exception {
        dir = Files.createTempDirectory("ekorr-report");
        Files.write(dir.resolve("ncc.html"),
                    ("<html><head>"
                     + "<meta name=\"dc:title\" content=\"Te, scones och nya äventyr\"/>"
                     + "<meta name=\"dc:identifier\" content=\"CA74908\"/>"
                     + "</head><body></body></html>").getBytes(StandardCharsets.UTF_8));
        ncc = new Ncc(dir.resolve("ncc.html").toFile());
    }

    @After
    public void removeBook() throws Exception {
        try (Stream<Path> files = Files.walk(dir)) {
            files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }

    @Test
    public void fileNameIsUnchangedFromEarlierVersions() throws Exception {
        assertEquals("CA74908_Te_scones_och_nya_äventyr_KORR.html", new Report(ncc).getReportFileName());
    }

    @Test
    public void openingDoesNotWriteAnything() throws Exception {
        Report report = new Report(ncc);
        assertFalse(report.exists());
        assertFalse(report.getReportDirectory().exists());
        assertTrue(report.getComments().isEmpty());
    }

    @Test
    public void commentsSurviveWriteAndRead() throws Exception {
        Report report = new Report(ncc);
        report.addComment(null, "12", "Läsfel", "a < b & c > \"d\"", "fel <em>x</em> &amp;", "rätt", null);
        report.addComment(null, "", "Övrigt", "sök", "fel", "", "UklGRg==");
        report.createAndWriteReport();
        assertTrue(report.exists());

        Report again = new Report(ncc);
        assertEquals(2, again.getComments().size());

        Comment first = again.getComments().get(0);
        assertEquals("1", first.getNr());
        assertEquals("12", first.getPage());
        assertEquals("Läsfel", first.getType());
        assertEquals("a < b & c > \"d\"", first.getSeek());
        assertEquals("fel <em>x</em> &amp;", first.getWrong());
        assertEquals("rätt", first.getRight());
        assertNull(first.getAudioData());

        Comment second = again.getComments().get(1);
        assertEquals("–", second.getPage());          // tom sida blir tankstreck
        assertEquals("UklGRg==", second.getAudioData());
    }
}
