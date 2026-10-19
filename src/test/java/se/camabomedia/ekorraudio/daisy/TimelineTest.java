package se.camabomedia.ekorraudio.daisy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Testar tidslinjen mot en riktig bok (ljudfilerna behövs inte).
 * Annan bok: mvn test -Dekorr.testbook=c:/sökväg/till/ncc.html
 */
public class TimelineTest {

    private static final String DEFAULT_BOOK =
        "c:/Users/qwfwq/camabo-workspace/leverans/CA60564_Box_PUBLISHER_fixgen/Daisy_202/ncc.html";

    private static Ncc ncc;
    private static Smil smil;
    private static Timeline timeline;

    @BeforeClass
    public static void load() {
        File book = new File(System.getProperty("ekorr.testbook", DEFAULT_BOOK));
        assumeTrue("Testboken finns inte: " + book, book.isFile());
        ncc = new Ncc(book);
        Content content = new Content(ncc.getDirectory(), ncc.getContentFile());
        smil = new Smil(ncc, content);
        timeline = new Timeline(ncc, smil);
    }

    @Test
    public void allParsAreInBookOrder() {
        assertEquals(smil.getSmilPars().size(), timeline.size());
        for (int i = 1; i < timeline.size(); i++) {
            assertTrue(timeline.get(i - 1).getNextSmilPar() == timeline.get(i));
        }
    }

    @Test
    public void segmentsAreContiguousOnTimeline() {
        List<Timeline.Segment> segs = timeline.getSegments();
        assertTrue(!segs.isEmpty());
        long t = 0;
        for (Timeline.Segment s : segs) {
            assertEquals("Segment " + s, t, s.start);
            assertTrue(s.length() > 0);
            t += s.length();
        }
        assertEquals(timeline.duration(), t);
    }

    @Test
    public void indexAtStartOfEachParFindsThatPar() {
        for (int i = 0; i < timeline.size(); i++) {
            if (timeline.endOf(i) > timeline.startOf(i)) {   // par med ljud
                assertEquals(i, timeline.indexAt(timeline.startOf(i)));
                assertEquals(i, timeline.indexAt(timeline.endOf(i) - 1));
            }
        }
        assertEquals(0, timeline.indexAt(-500));
        assertEquals(timeline.size() - 1, timeline.indexAt(timeline.duration() + 500));
    }

    @Test
    public void parStartsMatchSmilClipTimesWithinTheirFiles() {
        // Den globala tiden för ett par ska ligga lika långt in i sitt segment
        // som parets första klipp ligger in i ljudfilen
        for (Timeline.Segment s : timeline.getSegments()) {
            int i = timeline.indexAt(s.start);
            SmilAudio first = timeline.get(i).getSmilAudio().get(0);
            assertEquals(s.clipBegin, first.getClipBegin());
        }
    }

    @Test
    public void headingNavigationVisitsEveryHeadingInNcc() {
        long headingsInNcc = ncc.getNccItems().stream()
            .filter(item -> item.getName().matches("h[1-6]"))
            .filter(item -> smil.getParFromParID(item.getParID()) != null)
            .count();

        int visited = 0;
        int i = -1;
        while ((i = timeline.next(i, Timeline.Level.HEADING)) >= 0) {
            visited++;
        }
        assertEquals(headingsInNcc, visited);

        // och tillbaka igen
        int back = 0;
        i = timeline.size();
        while ((i = timeline.previous(i, Timeline.Level.HEADING)) >= 0) {
            back++;
        }
        assertEquals(visited, back);
    }

    @Test
    public void pageNavigationVisitsEveryPageInNcc() {
        long pagesInNcc = ncc.getNccItems().stream()
            .filter(item -> item.getCssClass().startsWith("page-"))
            .filter(item -> smil.getParFromParID(item.getParID()) != null)
            .count();
        assertTrue("Boken har inga sidor?", pagesInNcc > 0);

        int visited = 0;
        int i = -1;
        while ((i = timeline.next(i, Timeline.Level.PAGE)) >= 0) {
            // Sidnummerparet sätter sidan för sig självt
            assertTrue(timeline.get(i).getPage() != null);
            visited++;
        }
        assertEquals(pagesInNcc, visited);
    }

    @Test
    public void previousHeadingFromMiddleOfChapterGoesToItsStart() {
        int h1 = timeline.next(-1, Timeline.Level.HEADING);
        int h2 = timeline.next(h1, Timeline.Level.HEADING);
        assumeTrue(h2 - h1 > 1);
        assertEquals(h1, timeline.previous(h1 + 1, Timeline.Level.HEADING));
        assertEquals(h1, timeline.previous(h2, Timeline.Level.HEADING));
    }

    @Test
    public void edlHasHeaderAndOneLinePerSegment() throws Exception {
        Path edl = Files.createTempFile("ekorr", ".edl");
        try {
            timeline.writeEdl(edl);
            List<String> lines = Files.readAllLines(edl);
            assertEquals("# mpv EDL v0", lines.get(0));
            assertEquals(timeline.getSegments().size() + 1, lines.size());
            assertTrue(lines.get(1).matches("%\\d+%.+,start=\\d+\\.\\d{3},length=\\d+\\.\\d{3}"));
        }
        finally {
            Files.delete(edl);
        }
    }
}
