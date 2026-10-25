package se.camabomedia.ekorraudio.ui;

import java.util.List;
import java.util.function.LongConsumer;

import se.camabomedia.ekorraudio.daisy.Timeline;
import se.camabomedia.ekorraudio.daisy.Waveform;

import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * Ljudkurvan (längst ned i fönstret), i ett av två lägen:
 *
 *  - rörlig: kurvan rullar förbi och positionsstrecket står still, en
 *    fjärdedel in från vänster
 *  - fast: kurvan står still och strecket rör sig; när det når högerkanten
 *    bläddras kurvan fram en skärmbredd (som att vända blad)
 *
 * Innehåll:
 *
 *  - kurvan (toppvärden), symmetrisk kring mittlinjen
 *  - lodräta streck vid varje mening (par) – där texten byts
 *  - den mening som spelas, svagt markerad
 *  - långa pauser, markerade med färg och längd i sekunder
 *  - uppspelningsposition (streck)
 *
 * Klick i kurvan hoppar dit.
 */
public class WaveformView extends Pane {

    /** Synligt fönster: så här långt före och efter positionen, ms. */
    private static final long BEFORE_MS = 5_000;
    private static final long AFTER_MS = 15_000;
    private static final long WINDOW_MS = BEFORE_MS + AFTER_MS;
    /**
     * Fast läge: när strecket kommer så här nära högerkanten bläddras kurvan,
     * och det nya fönstret börjar så här långt före positionen.
     */
    private static final long PAGE_MARGIN_MS = 500;
    private static final long PAGE_LEAD_MS = 1_000;

    /** Nivå (0–32767) under vilken ljudet räknas som tyst. */
    public static final int SILENCE_THRESHOLD = 600;
    /** Tystnad minst så här lång (ms) markeras som lång paus. */
    public static final long LONG_PAUSE_MS = 2_000;

    private final Canvas canvas = new Canvas();
    private Waveform waveform;
    private Timeline timeline;
    private long position = 0;
    private int currentIndex = -1;
    private LongConsumer onSeek = ms -> {};

    /** Fast kurva (strecket rör sig) i stället för rörlig. */
    private boolean fixed = false;
    /** Fast läge: fönstrets början, ms. */
    private long fixedStart = 0;

    private Palette palette = Palette.LIGHT;

    /** Färger per tema. */
    private static final class Palette {
        static final Palette LIGHT = new Palette("#fafafa", "#5a7fa8", "#c9c9c9", "#fff3b0",
                                                 "#d0302f", "#e0303033", "#808080");
        static final Palette SOLARIZED_LIGHT = new Palette("#fdf6e3", "#268bd2", "#d8d1bc", "#f3e2a9",
                                                           "#cb4b16", "#dc322f33", "#93a1a1");
        static final Palette SOLARIZED_DARK = new Palette("#002b36", "#2aa198", "#0e4553", "#124f5c",
                                                          "#cb4b16", "#dc322f40", "#586e75");

        final Color background, wave, boundary, current, playhead, pause, text;

        Palette(String background, String wave, String boundary, String current,
                String playhead, String pause, String text) {
            this.background = Color.web(background);
            this.wave = Color.web(wave);
            this.boundary = Color.web(boundary);
            this.current = Color.web(current);
            this.playhead = Color.web(playhead);
            this.pause = Color.web(pause);
            this.text = Color.web(text);
        }

        static Palette of(Theme theme) {
            switch (theme) {
            case SOLARIZED_LIGHT:
                return SOLARIZED_LIGHT;
            case SOLARIZED_DARK:
                return SOLARIZED_DARK;
            default:
                return LIGHT;
            }
        }
    }

    public WaveformView() {
        getChildren().add(canvas);
        // Utanför layouten – annars hindrar canvas:ens bredd panelen från att krympa
        canvas.setManaged(false);
        setMinHeight(40);
        setPrefHeight(80);
        // Canvas följer panelens storlek
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        canvas.widthProperty().addListener(o -> redraw());
        canvas.heightProperty().addListener(o -> redraw());

        canvas.setOnMouseClicked(e -> {
            if (timeline == null) {
                return;
            }
            long t = timeAt(e.getX());
            onSeek.accept(Math.max(0, Math.min(t, timeline.duration() - 1)));
        });
        redraw();
    }

    /** Ny bok (eller null när boken stängs). */
    public void setSource(Timeline timeline, Waveform waveform) {
        this.timeline = timeline;
        this.waveform = waveform;
        this.position = 0;
        this.currentIndex = -1;
        this.fixedStart = 0;
        redraw();
    }

    /** true = fast kurva med rörlig markering, false = rörlig kurva. */
    public void setFixed(boolean fixed) {
        this.fixed = fixed;
        fixedStart = pageStartFor(position);
        redraw();
    }

    public boolean isFixed() {
        return fixed;
    }

    public void setTheme(Theme theme) {
        palette = Palette.of(theme);
        redraw();
    }

    /** Anropas när användaren klickar i kurvan, med tiden i ms. */
    public void setOnSeek(LongConsumer handler) {
        onSeek = handler == null ? ms -> {} : handler;
    }

    /** Ny uppspelningsposition. Begär också avkodning runt den. */
    public void setPosition(long ms, int index) {
        position = ms;
        currentIndex = index;
        if (fixed && (ms < fixedStart || ms > fixedStart + WINDOW_MS - PAGE_MARGIN_MS)) {
            // Utanför (eller nära slutet av) fönstret: bläddra dit. Ett klick
            // i kurvan hamnar inom fönstret, så kurvan står kvar då.
            fixedStart = pageStartFor(ms);
        }
        if (waveform != null) {
            // Lite mer än det synliga, så att kurvan finns innan den behövs
            long from = windowStart();
            waveform.prefetch(from, from + WINDOW_MS + Waveform.CHUNK_MS);
        }
        redraw();
    }

    /** Fast läge: fönstret som börjar strax före ms. */
    private static long pageStartFor(long ms) {
        return Math.max(0, ms - PAGE_LEAD_MS);
    }

    // ---------------------------------------------------------------- ritning

    private long windowStart() {
        return fixed ? fixedStart : position - BEFORE_MS;
    }

    private double msPerPixel() {
        return WINDOW_MS / Math.max(1.0, canvas.getWidth());
    }

    private double xAt(long ms) {
        return (ms - windowStart()) / msPerPixel();
    }

    private long timeAt(double x) {
        return windowStart() + Math.round(x * msPerPixel());
    }

    public void redraw() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(palette.background);
        g.fillRect(0, 0, w, h);
        if (timeline == null || waveform == null || w < 2 || h < 2) {
            return;
        }

        long from = windowStart();
        long to = from + WINDOW_MS;
        double mid = h / 2;

        // Meningen som spelas
        if (currentIndex >= 0 && currentIndex < timeline.size()) {
            double x0 = xAt(timeline.startOf(currentIndex));
            double x1 = xAt(timeline.endOf(currentIndex));
            g.setFill(palette.current);
            g.fillRect(x0, 0, Math.max(1, x1 - x0), h);
        }

        // Långa pauser
        g.setFont(Font.font(10));
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.TOP);
        List<long[]> pauses = waveform.silences(from, to, SILENCE_THRESHOLD, LONG_PAUSE_MS);
        for (long[] p : pauses) {
            double x0 = xAt(p[0]);
            double x1 = xAt(p[1]);
            g.setFill(palette.pause);
            g.fillRect(x0, 0, x1 - x0, h);
            g.setFill(palette.playhead);
            g.fillText(String.format("%.1f s", (p[1] - p[0]) / 1000.0), (x0 + x1) / 2, 2);
        }

        // Kurvan: en stapel per pixel. -1 = inte avkodat än (streckad mittlinje)
        int n = (int) Math.ceil(w);
        int[] levels = waveform.levels(from, to, n);
        g.setFill(palette.wave);
        for (int x = 0; x < n; x++) {
            int v = levels[x];
            if (v < 0) {
                if (x % 6 < 3) {
                    g.fillRect(x, mid, 1, 1);
                }
                continue;
            }
            // Roten ur ger en mer läsbar kurva för tal (svaga ljud syns)
            double amp = Math.sqrt(v / 32767.0) * (mid - 2);
            g.fillRect(x, mid - amp, 1, Math.max(1, 2 * amp));
        }

        // Gränser mellan meningar
        g.setStroke(palette.boundary);
        g.setLineWidth(1);
        int i = Math.max(0, timeline.indexAt(from));
        for (; i < timeline.size() && timeline.startOf(i) <= to; i++) {
            double x = Math.floor(xAt(timeline.startOf(i))) + 0.5;
            if (x >= 0) {
                g.strokeLine(x, 0, x, h);
            }
        }

        drawMissingAudio(g, from, to, h);

        // Uppspelningsposition
        double px = Math.floor(xAt(position)) + 0.5;
        g.setStroke(palette.playhead);
        g.setLineWidth(1.5);
        g.strokeLine(px, 0, px, h);

        // Tid vid positionen
        // Tid vid positionen – till vänster om strecket nära högerkanten
        g.setFill(palette.text);
        g.setTextBaseline(VPos.BOTTOM);
        if (px > w - 60) {
            g.setTextAlign(TextAlignment.RIGHT);
            g.fillText(Timeline.format(position), px - 4, h - 2);
        }
        else {
            g.setTextAlign(TextAlignment.LEFT);
            g.fillText(Timeline.format(position), px + 4, h - 2);
        }
    }

    /**
     * Par utan ljud (inläsning saknas) har längd 0 och syns inte i kurvan.
     * De markeras med ett tjockt streck, en liten triangel och texten
     * "saknas" – eller "saknas (n)" om flera ligger i rad på samma ställe.
     */
    private void drawMissingAudio(GraphicsContext g, long from, long to, double h) {
        g.setStroke(palette.playhead);
        g.setFill(palette.playhead);
        g.setLineWidth(2);
        g.setFont(Font.font(10));
        g.setTextAlign(TextAlignment.LEFT);
        g.setTextBaseline(VPos.TOP);

        // indexAt väljer paret som hörs – backa förbi par utan ljud som
        // ligger på samma tid, så att de också kommer med
        int i = Math.max(0, timeline.indexAt(from));
        while (i > 0 && timeline.startOf(i - 1) >= from) {
            i--;
        }
        while (i < timeline.size() && timeline.startOf(i) <= to) {
            long start = timeline.startOf(i);
            if (timeline.endOf(i) != start || start < from) {
                i++;
                continue;
            }
            // Räkna par utan ljud i rad på samma ställe
            int count = 0;
            while (i < timeline.size() && timeline.startOf(i) == start && timeline.endOf(i) == start) {
                count++;
                i++;
            }
            double x = Math.floor(xAt(start)) + 0.5;
            g.strokeLine(x, 0, x, h);
            g.fillPolygon(new double[] { x - 5, x + 5, x }, new double[] { 0, 0, 7 }, 3);
            g.fillText(count == 1 ? "saknas" : "saknas (" + count + ")", x + 4, 8);
        }
    }
}
