package se.camabomedia.ekorraudio.daisy;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Hela bokens ljud som en enda sammanhängande tidslinje.
 *
 * Alla ljudklipp i alla par läggs efter varandra i bokordning. Varje par får
 * en global start- och sluttid (ms från bokens början). Klipp som ligger
 * direkt efter varandra i samma ljudfil slås ihop till {@link Segment}.
 * Segmenten skrivs som en mpv-EDL, så att mpv spelar boken som en fil –
 * oavsett hur ljudfiler och smil-filer förhåller sig till varandra.
 *
 * Navigering (fras, rubrik, sida) sker med index i listan av par.
 */
public class Timeline {

    public enum Level {
        PHRASE("Fras"), HEADING("Rubrik"), PAGE("Sida");

        private final String label;

        Level(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** Ett sammanhängande stycke av en ljudfil, placerat på tidslinjen. */
    public static final class Segment {
        public final File file;
        /** Start i ljudfilen, ms. */
        public final long clipBegin;
        /** Slut i ljudfilen, ms. */
        public final long clipEnd;
        /** Start på bokens tidslinje, ms. */
        public final long start;

        Segment(File file, long clipBegin, long clipEnd, long start) {
            this.file = file;
            this.clipBegin = clipBegin;
            this.clipEnd = clipEnd;
            this.start = start;
        }

        public long length() {
            return clipEnd - clipBegin;
        }

        @Override
        public String toString() {
            return file.getName() + " [" + clipBegin + "–" + clipEnd + "] @" + start;
        }
    }

    /** Glapp (ms) som fortfarande räknas som "direkt efter" vid sammanslagning. */
    private static final long JOIN_TOLERANCE = 1;
    private static final Pattern HEADING = Pattern.compile("h[1-6]");

    private final List<SmilPar> pars;
    private final long[] starts;
    private final long[] ends;
    private final Map<SmilPar, Integer> indexOf = new IdentityHashMap<>();
    private final List<Segment> segments = new ArrayList<>();
    private final int[] headings;
    private final int[] pages;
    private final long duration;
    /** Global starttid (ms) för varje ljudklipp, stigande. */
    private final long[] clipStarts;

    public Timeline(Ncc ncc, Smil smil) {
        pars = Collections.unmodifiableList(new ArrayList<>(smil.getSmilPars()));
        int n = pars.size();
        starts = new long[n];
        ends = new long[n];

        File directory = ncc.getDirectory();
        File segFile = null;
        long segBegin = 0, segEnd = 0, segStart = 0;
        long t = 0;
        ArrayList<Long> clips = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            SmilPar par = pars.get(i);
            indexOf.put(par, i);
            starts[i] = t;

            for (SmilAudio clip : par.getSmilAudio()) {
                File f = new File(directory, clip.getSrc());
                boolean continues = segFile != null
                    && segFile.equals(f)
                    && Math.abs(clip.getClipBegin() - segEnd) <= JOIN_TOLERANCE;

                if (continues) {
                    segEnd = clip.getClipEnd();
                }
                else {
                    if (segFile != null) {
                        segments.add(new Segment(segFile, segBegin, segEnd, segStart));
                    }
                    segFile = f;
                    segBegin = clip.getClipBegin();
                    segEnd = clip.getClipEnd();
                    segStart = t;
                }
                long clipStart = segStart + (clip.getClipBegin() - segBegin);
                if (clips.isEmpty() || clips.get(clips.size() - 1) < clipStart) {
                    clips.add(clipStart);
                }
                // Global tid räknas ur segmentets geometri, så att EDL:en och
                // tidslinjen alltid stämmer exakt med varandra
                t = segStart + (segEnd - segBegin);
            }
            ends[i] = t;
        }
        if (segFile != null) {
            segments.add(new Segment(segFile, segBegin, segEnd, segStart));
        }
        duration = t;
        clipStarts = clips.stream().mapToLong(Long::longValue).toArray();

        // Rubriker och sidor hämtas från NCC:n, som pekar på par-id:n
        TreeSet<Integer> h = new TreeSet<>();
        TreeSet<Integer> p = new TreeSet<>();
        for (NccItem item : ncc.getNccItems()) {
            SmilPar par = smil.getParFromParID(item.getParID());
            Integer idx = par == null ? null : indexOf.get(par);
            if (idx == null) {
                continue;
            }
            if (HEADING.matcher(item.getName()).matches()) {
                h.add(idx);
            }
            else if (item.getCssClass().startsWith("page-")) {
                p.add(idx);
            }
        }
        headings = h.stream().mapToInt(Integer::intValue).toArray();
        pages = p.stream().mapToInt(Integer::intValue).toArray();
    }

    public int size() {
        return pars.size();
    }

    public SmilPar get(int index) {
        return pars.get(index);
    }

    /** Index för par, eller -1 om det inte finns i tidslinjen. */
    public int indexOf(SmilPar par) {
        Integer i = par == null ? null : indexOf.get(par);
        return i == null ? -1 : i;
    }

    /** Global starttid (ms) för par nummer index. */
    public long startOf(int index) {
        return starts[index];
    }

    public long endOf(int index) {
        return ends[index];
    }

    public long duration() {
        return duration;
    }

    public List<Segment> getSegments() {
        return Collections.unmodifiableList(segments);
    }

    /**
     * Index för det par som hörs vid global tid ms. Par utan ljud har längd 0
     * och hoppas över – vid lika starttid vinner det sista, alltså det som
     * faktiskt hörs.
     */
    public int indexAt(long ms) {
        if (starts.length == 0) {
            return -1;
        }
        int lo = 0, hi = starts.length - 1, found = 0;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (starts[mid] <= ms) {
                found = mid;
                lo = mid + 1;
            }
            else {
                hi = mid - 1;
            }
        }
        return found;
    }

    /**
     * Index för sidnummerparet för sidan page (t.ex. "12"), eller -1.
     */
    public int indexOfPage(String page) {
        if (page == null) {
            return -1;
        }
        String wanted = page.strip();
        for (int i : pages) {
            String p = pars.get(i).getPage();
            if (p != null && p.strip().equals(wanted)) {
                return i;
            }
        }
        return -1;
    }

    /** Index för nästa sida efter from, eller size() om det inte finns fler. */
    public int nextPageOrEnd(int from) {
        int i = firstAfter(pages, from);
        return i < 0 ? size() : i;
    }

    /**
     * Starttid för nästa ljudklipp efter tiden ms, eller -1. Används som
     * "nästa fras" i talböcker utan text, där ett par ofta är ett helt
     * avsnitt med många klipp.
     */
    public long nextClipStart(long ms) {
        int i = Arrays.binarySearch(clipStarts, ms + 1);
        if (i < 0) {
            i = -i - 1;
        }
        return i < clipStarts.length ? clipStarts[i] : -1;
    }

    /**
     * Starttid för föregående ljudklipp, eller -1. Är man mer än
     * graceMs in i ett klipp går man till dess början (som i en
     * ljudspelare), annars till klippet före.
     */
    public long previousClipStart(long ms, long graceMs) {
        long limit = ms - graceMs;
        int i = Arrays.binarySearch(clipStarts, limit);
        if (i < 0) {
            i = -i - 1;
        }
        // i är första position med värde >= limit
        return i - 1 >= 0 ? clipStarts[i - 1] : (clipStarts.length > 0 ? clipStarts[0] : -1);
    }

    /** Nästa mål efter from på given nivå, eller -1. */
    public int next(int from, Level level) {
        switch (level) {
        case PHRASE:
            return from + 1 < size() ? from + 1 : -1;
        case HEADING:
            return firstAfter(headings, from);
        case PAGE:
            return firstAfter(pages, from);
        default:
            return -1;
        }
    }

    /** Föregående mål före from på given nivå, eller -1. */
    public int previous(int from, Level level) {
        switch (level) {
        case PHRASE:
            return from - 1 >= 0 ? from - 1 : -1;
        case HEADING:
            return lastBefore(headings, from);
        case PAGE:
            return lastBefore(pages, from);
        default:
            return -1;
        }
    }

    private static int firstAfter(int[] targets, int from) {
        int i = Arrays.binarySearch(targets, from + 1);
        if (i < 0) {
            i = -i - 1;
        }
        return i < targets.length ? targets[i] : -1;
    }

    private static int lastBefore(int[] targets, int from) {
        int i = Arrays.binarySearch(targets, from);
        if (i < 0) {
            i = -i - 1;
        }
        // i är nu första position med värde >= from
        return i - 1 >= 0 ? targets[i - 1] : -1;
    }

    /**
     * Skriver tidslinjen som en mpv-EDL (# mpv EDL v0). Filnamn skrivs med
     * längdprefix (%byte%namn) så att komma, semikolon och å/ä/ö fungerar.
     */
    public void writeEdl(Path edlFile) throws IOException {
        try (Writer w = Files.newBufferedWriter(edlFile, StandardCharsets.UTF_8)) {
            w.write("# mpv EDL v0\n");
            for (Segment s : segments) {
                String path = s.file.getAbsolutePath();
                int bytes = path.getBytes(StandardCharsets.UTF_8).length;
                w.write("%" + bytes + "%" + path
                        + ",start=" + seconds(s.clipBegin)
                        + ",length=" + seconds(s.length()) + "\n");
            }
        }
    }

    public static String seconds(long ms) {
        return String.format(Locale.ROOT, "%.3f", ms / 1000.0);
    }

    /** hh:mm:ss för visning. */
    public static String format(long ms) {
        long s = ms / 1000;
        return String.format("%d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60);
    }
}
