package se.camabomedia.ekorraudio.daisy;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Ljudkurva för boken, avkodad löpande runt uppspelningen.
 *
 * Bokens tidslinje delas i bitar om {@link #CHUNK_MS}. En bit avkodas när den
 * behövs (prefetch), i en bakgrundstråd, och lagras som toppvärden per
 * {@link #BIN_MS} ms. De senast använda bitarna hålls i minnet.
 *
 * Avkodningen görs av en {@link Decoder} – normalt mpv i kodningsläge, som
 * ger 2000 värden per sekund, mono, 16 bitar. Klassen är fri från JavaFX:
 * när en bit blivit klar anropas en Runnable från bakgrundstråden.
 */
public class Waveform implements AutoCloseable {

    /** Längd på en avkodad bit, ms (global tid). */
    public static final long CHUNK_MS = 30_000;
    /** Upplösning på toppvärdena, ms. */
    public static final int BIN_MS = 10;
    /** Avkodarens samplingsfrekvens. */
    public static final int RATE = 2000;

    private static final int SAMPLES_PER_BIN = RATE * BIN_MS / 1000;   // 20
    private static final int BINS_PER_CHUNK = (int) (CHUNK_MS / BIN_MS);
    /** Bitar i minnet (40 × 30 s = 20 minuter, ca 240 kB). */
    private static final int CACHE_CHUNKS = 40;

    /** Avkodar [startMs, endMs) i en ljudfil till 2000 Hz, mono, 16 bitar. */
    public interface Decoder {
        short[] decode(File file, long startMs, long endMs) throws IOException;
    }

    private final Timeline timeline;
    private final Decoder decoder;
    private final ExecutorService worker;
    private volatile Runnable onChunkLoaded = () -> {};

    /** Avkodade bitar: index -> toppvärden (0–32767). LRU-ordning. */
    private final Map<Long, short[]> chunks = new LinkedHashMap<Long, short[]>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, short[]> eldest) {
            return size() > CACHE_CHUNKS;
        }
    };
    private final Set<Long> pending = ConcurrentHashMap.newKeySet();
    private volatile boolean closed = false;

    public Waveform(Timeline timeline, Decoder decoder) {
        this.timeline = timeline;
        this.decoder = decoder;
        this.worker = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "waveform-decoder");
            t.setDaemon(true);
            return t;
        });
    }

    /** Med mpv som avkodare. */
    public Waveform(Timeline timeline, String mpvPath) {
        this(timeline, new MpvDecoder(mpvPath));
    }

    /** Anropas (från bakgrundstråden) när en bit avkodats. */
    public void setOnChunkLoaded(Runnable r) {
        onChunkLoaded = r == null ? () -> {} : r;
    }

    /**
     * Ser till att bitarna för [fromMs, toMs) avkodas. Närmast först: biten
     * med fromMs läggs först i kön.
     */
    public void prefetch(long fromMs, long toMs) {
        if (closed) {
            return;
        }
        long first = Math.max(0, fromMs) / CHUNK_MS;
        long last = Math.max(0, Math.min(toMs, timeline.duration()) - 1) / CHUNK_MS;
        for (long c = first; c <= last; c++) {
            final long chunk = c;
            synchronized (chunks) {
                if (chunks.containsKey(chunk)) {
                    continue;
                }
            }
            if (pending.add(chunk)) {
                worker.submit(() -> load(chunk));
            }
        }
    }

    /**
     * Nivån (0–32767) för varje del av [fromMs, toMs) delat i n lika delar,
     * eller -1 där ljudet inte är avkodat än.
     */
    public int[] levels(long fromMs, long toMs, int n) {
        int[] result = new int[n];
        double step = (toMs - fromMs) / (double) n;
        for (int i = 0; i < n; i++) {
            long a = fromMs + Math.round(i * step);
            long b = fromMs + Math.round((i + 1) * step);
            result[i] = peak(a, Math.max(b, a + BIN_MS));
        }
        return result;
    }

    /** Högsta toppvärdet i [fromMs, toMs), 0 utanför boken, -1 om ej avkodat. */
    public int peak(long fromMs, long toMs) {
        if (toMs <= 0 || fromMs >= timeline.duration()) {
            return 0;
        }
        int max = 0;
        long from = Math.max(0, fromMs);
        long to = Math.min(toMs, timeline.duration());
        for (long bin = from / BIN_MS; bin * BIN_MS < to; bin++) {
            long chunk = bin / BINS_PER_CHUNK;
            short[] data;
            synchronized (chunks) {
                data = chunks.get(chunk);
            }
            if (data == null) {
                return -1;
            }
            int v = data[(int) (bin % BINS_PER_CHUNK)];
            if (v > max) {
                max = v;
            }
        }
        return max;
    }

    /**
     * Tysta partier i [fromMs, toMs) som är minst minLengthMs långa.
     * Bara avkodade delar räknas. Returnerar par {start, slut} i ms.
     */
    public List<long[]> silences(long fromMs, long toMs, int threshold, long minLengthMs) {
        List<long[]> result = new ArrayList<>();
        long runStart = -1;
        long from = Math.max(0, fromMs) / BIN_MS * BIN_MS;
        long to = Math.min(toMs, timeline.duration());
        for (long t = from; t < to; t += BIN_MS) {
            int v = peak(t, t + BIN_MS);
            boolean silent = v >= 0 && v < threshold;
            if (silent && runStart < 0) {
                runStart = t;
            }
            if (!silent && runStart >= 0) {
                if (t - runStart >= minLengthMs) {
                    result.add(new long[] { runStart, t });
                }
                runStart = -1;
            }
        }
        if (runStart >= 0 && to - runStart >= minLengthMs) {
            result.add(new long[] { runStart, to });
        }
        return result;
    }

    @Override
    public void close() {
        closed = true;
        worker.shutdownNow();
    }

    // ---------------------------------------------------------------- intern

    private void load(long chunk) {
        try {
            if (closed) {
                return;
            }
            short[] bins = decodeChunk(chunk);
            synchronized (chunks) {
                chunks.put(chunk, bins);
            }
            onChunkLoaded.run();
        }
        catch (Exception e) {
            if (!closed) {
                System.err.println("Ljudkurva: kunde inte avkoda " + Timeline.format(chunk * CHUNK_MS)
                                   + ": " + e.getMessage());
            }
        }
        finally {
            pending.remove(chunk);
        }
    }

    /** Avkodar en bit, som kan sträcka sig över flera ljudfiler. */
    short[] decodeChunk(long chunk) throws IOException {
        long c0 = chunk * CHUNK_MS;
        long c1 = c0 + CHUNK_MS;
        short[] bins = new short[BINS_PER_CHUNK];

        for (Timeline.Segment s : timeline.getSegments()) {
            long g0 = Math.max(c0, s.start);
            long g1 = Math.min(c1, s.start + s.length());
            if (g0 >= g1) {
                continue;
            }
            long fileStart = s.clipBegin + (g0 - s.start);
            long fileEnd = s.clipBegin + (g1 - s.start);
            short[] samples = decoder.decode(s.file, fileStart, fileEnd);

            // Varje värde läggs i den bin dess globala tid hör till. Filbiten
            // börjar sällan jämnt på en bin-gräns (t.ex. 8216 ms), så räkna
            // exakt i värden (1 värde = 0,5 ms) i stället för i hela binar.
            long offset = (g0 - c0) * RATE / 1000;     // värden från bitens start
            for (int i = 0; i < samples.length; i++) {
                int idx = (int) ((offset + i) / SAMPLES_PER_BIN);
                if (idx >= BINS_PER_CHUNK) {
                    break;
                }
                int v = Math.min(Math.abs((int) samples[i]), Short.MAX_VALUE);
                if (v > bins[idx]) {
                    bins[idx] = (short) v;
                }
            }
        }
        return bins;
    }

    /**
     * Avkodning med mpv i kodningsläge till en temporärfil (inte till stdout,
     * där mpv kan blanda in egna meddelanden).
     *
     * Utdata fylls ut med nollor till hela block om 64 KiB, så bara de första
     * (längd × 2000) värdena används. Omsamplingen ger en fördröjning på ca
     * 32 ms (64 värden) i början; den hoppas över och slutet förlängs lika mycket.
     */
    public static class MpvDecoder implements Decoder {
        private static final int RESAMPLE_DELAY_SAMPLES = 64;
        private final String mpvPath;

        public MpvDecoder(String mpvPath) {
            this.mpvPath = mpvPath;
        }

        @Override
        public short[] decode(File file, long startMs, long endMs) throws IOException {
            Path out = Files.createTempFile("ekorr-wave-", ".raw");
            try {
                long delayMs = RESAMPLE_DELAY_SAMPLES * 1000L / RATE;
                List<String> cmd = new ArrayList<>();
                cmd.add(mpvPath);
                cmd.add("--no-config");
                cmd.add("--no-video");
                cmd.add("--msg-level=all=error");
                cmd.add("--start=" + Timeline.seconds(startMs));
                cmd.add("--end=" + Timeline.seconds(endMs + delayMs));
                cmd.add("--af=lavfi=[aresample=" + RATE + ",aformat=sample_fmts=s16:channel_layouts=mono]");
                cmd.add("--o=" + out.toAbsolutePath());
                cmd.add("--of=s16le");
                cmd.add("--oac=pcm_s16le");
                cmd.add("--");
                cmd.add(file.getAbsolutePath());

                Process p = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
                try {
                    p.waitFor();
                }
                catch (InterruptedException e) {
                    p.destroy();
                    Thread.currentThread().interrupt();
                    throw new IOException("Avbruten");
                }

                byte[] bytes = Files.readAllBytes(out);
                int wanted = (int) ((endMs - startMs) * RATE / 1000);
                int available = bytes.length / 2 - RESAMPLE_DELAY_SAMPLES;
                int n = Math.max(0, Math.min(wanted, available));
                short[] samples = new short[n];
                ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
                for (int i = 0; i < n; i++) {
                    samples[i] = buf.getShort((RESAMPLE_DELAY_SAMPLES + i) * 2);
                }
                return samples;
            }
            finally {
                Files.deleteIfExists(out);
            }
        }
    }
}
