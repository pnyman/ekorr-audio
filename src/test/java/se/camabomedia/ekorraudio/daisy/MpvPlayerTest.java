package se.camabomedia.ekorraudio.daisy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Spelar riktigt ljud via mpv och EDL. Kräver en bok med ljudfiler och mpv.
 * Annan bok: mvn test -Dekorr.testbook=c:/sökväg/till/ncc.html
 */
public class MpvPlayerTest {

    private static final String DEFAULT_BOOK =
        "c:/Users/qwfwq/camabo-workspace/leverans/CA60564_Box_PUBLISHER_fixgen/Daisy_202/ncc.html";

    private static Timeline timeline;
    private static Path edl;

    @BeforeClass
    public static void load() throws Exception {
        File book = new File(System.getProperty("ekorr.testbook", DEFAULT_BOOK));
        assumeTrue("Testboken finns inte: " + book, book.isFile());
        Ncc ncc = new Ncc(book);
        Smil smil = new Smil(ncc, new Content(ncc.getDirectory(), ncc.getContentFile()));
        timeline = new Timeline(ncc, smil);
        assumeTrue("Ljudfilerna saknas", timeline.getSegments().get(0).file.isFile());

        edl = Files.createTempFile("ekorr-test-", ".edl");
        timeline.writeEdl(edl);
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (edl != null) {
            Files.deleteIfExists(edl);
        }
    }

    @Test
    public void opensPausedAtStartPosition() throws Exception {
        long start = timeline.startOf(timeline.size() / 2);
        try (MpvPlayer mpv = new MpvPlayer()) {
            mpv.open(edl, start);
            assertTrue(mpv.isOpen());
            assertTrue(mpv.isPaused());
            Thread.sleep(500);
            assertEquals(start, mpv.getPosition(), 50);
        }
    }

    @Test
    public void playsAcrossAudioFileBoundary() throws Exception {
        assumeTrue("Boken har bara en ljudfil", timeline.getSegments().size() > 1);
        Timeline.Segment second = timeline.getSegments().get(1);

        try (MpvPlayer mpv = new MpvPlayer()) {
            // 1,5 s före slutet på första ljudfilen, spela 3 s
            mpv.open(edl, second.start - 1500);
            Thread.sleep(300);
            mpv.play();
            Thread.sleep(3000);
            mpv.pause();
            Thread.sleep(200);

            long pos = mpv.getPosition();
            assertTrue("Positionen " + pos + " borde ha passerat filgränsen " + second.start,
                       pos > second.start + 500);
            // Och tidslinjen säger att vi nu är i ett par från den andra ljudfilen
            SmilAudio clip = timeline.get(timeline.indexAt(pos)).getSmilAudio().get(0);
            assertEquals(second.file.getName(), clip.getSrc());
        }
    }

    @Test
    public void seekMovesPositionAndKeepsPauseState() throws Exception {
        try (MpvPlayer mpv = new MpvPlayer()) {
            mpv.open(edl, 0);
            Thread.sleep(300);
            long target = timeline.startOf(timeline.size() - 2);
            mpv.seek(target);
            Thread.sleep(300);
            assertTrue(mpv.isPaused());
            assertEquals(target, mpv.getPosition(), 50);
            assertFalse(timeline.indexAt(mpv.getPosition()) < timeline.size() - 2);
        }
    }
}
