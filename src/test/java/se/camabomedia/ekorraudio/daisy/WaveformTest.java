package se.camabomedia.ekorraudio.daisy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Testar uppdelningen i bitar och segment med en påhittad avkodare (inget
 * riktigt ljud behövs). Avkodarens "ljud" kodar fil och tidpunkt, så att man
 * kan se att rätt del av rätt fil hamnar på rätt ställe i tidslinjen.
 */
public class WaveformTest {

    private static final String DEFAULT_BOOK =
        "c:/Users/qwfwq/camabo-workspace/leverans/CA60564_Box_PUBLISHER_fixgen/Daisy_202/ncc.html";

    private static Timeline timeline;
    private static final List<String> files = new ArrayList<>();

    @BeforeClass
    public static void load() {
        File book = new File(System.getProperty("ekorr.testbook", DEFAULT_BOOK));
        assumeTrue("Testboken finns inte: " + book, book.isFile());
        Ncc ncc = new Ncc(book);
        Smil smil = new Smil(ncc, new Content(ncc.getDirectory(), ncc.getContentFile()));
        timeline = new Timeline(ncc, smil);
        for (Timeline.Segment s : timeline.getSegments()) {
            if (!files.contains(s.file.getName())) {
                files.add(s.file.getName());
            }
        }
    }

    /**
     * "Ljudet" i fil nr k vid filtid t ms har värdet 1000 + 100·k + (t / 1000) % 100,
     * dvs. fil och hel sekund kan läsas ut ur nivån. Tystnad 0 mellan 2,0 och
     * 4,5 s i varje fil.
     */
    private static final Waveform.Decoder FAKE = (file, startMs, endMs) -> {
        int k = files.indexOf(file.getName());
        int n = (int) ((endMs - startMs) * Waveform.RATE / 1000);
        short[] s = new short[n];
        for (int i = 0; i < n; i++) {
            long t = startMs + i * 1000L / Waveform.RATE;
            s[i] = (short) (t >= 2000 && t < 4500 ? 0 : 1000 + 100 * k + (t / 1000) % 100);
        }
        return s;
    };

    private static int expected(long globalMs) {
        for (Timeline.Segment s : timeline.getSegments()) {
            if (globalMs >= s.start && globalMs < s.start + s.length()) {
                long t = s.clipBegin + (globalMs - s.start);
                int k = files.indexOf(s.file.getName());
                return t >= 2000 && t < 4500 ? 0 : (int) (1000 + 100 * k + (t / 1000) % 100);
            }
        }
        return 0;
    }

    @Test
    public void chunksMapGlobalTimeToTheRightFileAndOffset() throws Exception {
        Waveform w = new Waveform(timeline, FAKE);
        // Några bitar spridda över boken, däribland bitar över filgränser
        List<Long> chunks = new ArrayList<>();
        chunks.add(0L);
        for (Timeline.Segment s : timeline.getSegments().subList(1, Math.min(6, timeline.getSegments().size()))) {
            chunks.add(s.start / Waveform.CHUNK_MS);
        }
        chunks.add((timeline.duration() - 1) / Waveform.CHUNK_MS);

        for (long c : chunks) {
            short[] bins = w.decodeChunk(c);
            for (int b = 0; b < bins.length; b++) {
                long t = c * Waveform.CHUNK_MS + (long) b * Waveform.BIN_MS;
                if (t >= timeline.duration()) {
                    assertEquals(0, bins[b]);
                    continue;
                }
                // Inom en bin kan värdet byta sekund (eller fil) – toppen är max av de två
                int e = Math.max(expected(t), expected(Math.min(t + Waveform.BIN_MS - 1, timeline.duration() - 1)));
                assertEquals("bit " + c + ", tid " + t, e, bins[b]);
            }
        }
        w.close();
    }

    @Test
    public void prefetchLoadsAndPeaksAreReadable() throws Exception {
        Waveform w = new Waveform(timeline, FAKE);
        long from = timeline.getSegments().get(3).start - 10_000;   // över en filgräns
        long to = from + 40_000;
        assertEquals(-1, w.peak(from, from + 100));   // inte avkodat än

        Object lock = new Object();
        w.setOnChunkLoaded(() -> {
            synchronized (lock) {
                lock.notifyAll();
            }
        });
        w.prefetch(from, to);
        long deadline = System.currentTimeMillis() + 5000;
        while (w.peak(from, to) < 0 && System.currentTimeMillis() < deadline) {
            synchronized (lock) {
                lock.wait(100);
            }
        }
        assertTrue(w.peak(from, to) > 0);

        int[] levels = w.levels(from, to, 400);
        for (int v : levels) {
            assertTrue(v >= 0);
        }
        w.close();
    }

    @Test
    public void silencesFindsThePauseInEachFile() throws Exception {
        Waveform w = new Waveform(timeline, FAKE);
        Timeline.Segment s = timeline.getSegments().get(2);
        assumeTrue(s.clipBegin == 0 && s.length() > 6000);
        long c = s.start / Waveform.CHUNK_MS;
        w.prefetch(s.start, s.start + 6000);
        long deadline = System.currentTimeMillis() + 5000;
        while (w.peak(s.start, s.start + 6000) < 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        List<long[]> found = w.silences(s.start, s.start + 6000, 500, 2000);
        assertEquals(1, found.size());
        // Pausen hittas med binarnas upplösning (10 ms)
        assertEquals(s.start + 2000, found.get(0)[0], Waveform.BIN_MS);
        assertEquals(s.start + 4500, found.get(0)[1], Waveform.BIN_MS);
        assertTrue(c >= 0);
        w.close();
    }
}
