package se.camabomedia.ekorraudio.daisy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Styr en mpv-process som lever kvar så länge ett media (en fil eller en
 * EDL-tidslinje) är öppet. Kommandon och positionsavläsning går via mpv:s
 * JSON-IPC över en named pipe (Windows).
 *
 * Klassen är fri från JavaFX: positions- och pausändringar skickas till en
 * {@link Listener} från en bakgrundstråd. Den som uppdaterar gränssnittet
 * måste själv flytta över till JavaFX-tråden (Platform.runLater).
 */
public class MpvPlayer implements AutoCloseable {

    public interface Listener {
        /** Aktuell position i ms. Anropas ungefär var 100:e ms från en bakgrundstråd. */
        void positionChanged(long ms);

        /** Anropas när pausläget ändras, även när mpv pausar själv vid slutet. */
        default void pausedChanged(boolean paused) {}
    }

    /** scoop:s "current" pekar alltid på installerad version. Annars mpv i PATH. */
    private static final String SCOOP_MPV = System.getProperty("user.home")
        + "/scoop/apps/mpv/current/mpv.com";

    private static final Pattern REQUEST_ID = Pattern.compile("\"request_id\"\\s*:\\s*(\\d+)");
    private static final Pattern DATA = Pattern.compile("\"data\"\\s*:\\s*(true|false|null|-?[0-9.eE+\\-]+)");
    private static final Pattern ERROR = Pattern.compile("\"error\"\\s*:\\s*\"([^\"]*)\"");

    /** Processer som ska dödas om JVM:en avslutas (t.ex. System.exit i App). */
    private static final Set<Process> LIVE = ConcurrentHashMap.newKeySet();
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> LIVE.forEach(Process::destroy)));
    }

    private final String mpvPath;
    private final Object ipcLock = new Object();

    private volatile Listener listener;
    private volatile Process process;
    private RandomAccessFile ipc;            // skyddas av ipcLock
    private int nextRequestId = 1;           // skyddas av ipcLock
    private ScheduledExecutorService poller;

    private volatile long position;
    private volatile boolean paused = true;
    /** Uppspelningshastighet, 1.0 = normal. Behålls när ett nytt media öppnas. */
    private volatile double speed = 1.0;
    /** Ökas vid varje sökning, så att en avläsning som påbörjades före sökningen kastas. */
    private final AtomicLong seekGeneration = new AtomicLong();

    public MpvPlayer() {
        this(defaultMpvPath());
    }

    public MpvPlayer(String mpvPath) {
        this.mpvPath = mpvPath;
    }

    public static String defaultMpvPath() {
        String env = System.getenv("EKORR_MPV");
        if (env != null && !env.isBlank()) {
            return env;
        }
        return new File(SCOOP_MPV).isFile() ? SCOOP_MPV : "mpv";
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /**
     * Öppnar en ljudfil eller EDL-fil, pausad vid startMs. Stänger ett
     * tidigare öppnat media. Blockerar tills IPC-anslutningen är klar.
     */
    public synchronized void open(Path media, long startMs) throws IOException {
        close();

        String pipeName = "\\\\.\\pipe\\ekorr-mpv-" + ProcessHandle.current().pid() + "-" + System.nanoTime();

        List<String> cmd = new ArrayList<>();
        cmd.add(mpvPath);
        cmd.add("--no-video");
        cmd.add("--input-terminal=no");
        cmd.add("--msg-level=all=error");
        cmd.add("--idle=no");
        cmd.add("--keep-open=yes");          // stanna (pausad) vid slutet i stället för att avsluta
        cmd.add("--pause");
        cmd.add("--hr-seek=yes");            // exakta sökningar
        cmd.add("--speed=" + String.format(java.util.Locale.ROOT, "%.2f", speed));
        // Tonhöjden behålls vid ändrad hastighet (mpv:s standard, men uttryckligt)
        cmd.add("--audio-pitch-correction=yes");
        cmd.add("--input-ipc-server=" + pipeName);
        cmd.add("--start=" + Timeline.seconds(Math.max(0, startMs)));
        cmd.add("--");
        cmd.add(media.toAbsolutePath().toString());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);

        Process p;
        try {
            p = pb.start();
        }
        catch (IOException e) {
            throw new IOException("Kunde inte starta mpv (" + mpvPath + "): " + e.getMessage(), e);
        }
        LIVE.add(p);
        p.onExit().thenRun(() -> LIVE.remove(p));
        process = p;
        position = Math.max(0, startMs);
        paused = true;

        connect(p, pipeName);

        poller = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "mpv-poller");
            t.setDaemon(true);
            return t;
        });
        poller.scheduleWithFixedDelay(this::poll, 100, 100, TimeUnit.MILLISECONDS);
    }

    /** Väntar på att mpv skapar pipen (normalt några tiotal ms). */
    private void connect(Process p, String pipeName) throws IOException {
        long deadline = System.currentTimeMillis() + 5000;
        while (true) {
            try {
                RandomAccessFile r = new RandomAccessFile(pipeName, "rw");
                synchronized (ipcLock) {
                    ipc = r;
                }
                return;
            }
            catch (FileNotFoundException notYet) {
                if (!p.isAlive()) {
                    throw new IOException("mpv avslutades direkt (kod " + p.exitValue()
                                          + "). Kontrollera att filen går att spela.");
                }
                if (System.currentTimeMillis() > deadline) {
                    p.destroy();
                    throw new IOException("Fick ingen kontakt med mpv via " + pipeName);
                }
                try {
                    Thread.sleep(20);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    p.destroy();
                    throw new IOException("Avbruten", e);
                }
            }
        }
    }

    public boolean isOpen() {
        Process p = process;
        return p != null && p.isAlive();
    }

    public void play() {
        setPaused(false);
    }

    public void pause() {
        setPaused(true);
    }

    public void togglePause() {
        setPaused(!paused);
    }

    public boolean isPaused() {
        return paused;
    }

    /** Senast kända position i ms. */
    public long getPosition() {
        return position;
    }

    private void setPaused(boolean p) {
        if (!isOpen()) {
            return;
        }
        if (command("[\"set_property\",\"pause\"," + p + "]")) {
            paused = p;
            Listener l = listener;
            if (l != null) {
                l.pausedChanged(p);
            }
        }
    }

    /** Sätter hastigheten (1.0 = normal, 1.5 = 150 %). Gäller även nästa open(). */
    public void setSpeed(double newSpeed) {
        speed = newSpeed;
        if (isOpen()) {
            command("[\"set_property\",\"speed\","
                    + String.format(java.util.Locale.ROOT, "%.2f", newSpeed) + "]");
        }
    }

    public double getSpeed() {
        return speed;
    }

    /** Söker till absolut position (ms). Pausläget behålls. */
    public void seek(long ms) {
        if (!isOpen()) {
            return;
        }
        seekGeneration.incrementAndGet();
        long target = Math.max(0, ms);
        if (command("[\"seek\"," + Timeline.seconds(target) + ",\"absolute+exact\"]")) {
            position = target;
        }
    }

    @Override
    public synchronized void close() {
        if (poller != null) {
            poller.shutdownNow();
            poller = null;
        }
        Process p = process;
        process = null;
        if (p != null && p.isAlive()) {
            // Att döda processen stänger pipen, så en blockerad läsning släpper låset
            p.destroy();
        }
        synchronized (ipcLock) {
            if (ipc != null) {
                try {
                    ipc.close();
                }
                catch (IOException ignored) {
                }
                ipc = null;
            }
        }
        paused = true;
    }

    // ---------------------------------------------------------------- intern

    private void poll() {
        if (!isOpen()) {
            return;
        }
        long generation = seekGeneration.get();
        String pos = get("time-pos");
        String pau = get("pause");
        if (generation != seekGeneration.get()) {
            return; // en sökning hann komma emellan – värdet är inaktuellt
        }

        Listener l = listener;
        if (pos != null && !pos.equals("null")) {
            try {
                position = Math.round(Double.parseDouble(pos) * 1000);
                if (l != null) {
                    l.positionChanged(position);
                }
            }
            catch (NumberFormatException ignored) {
            }
        }
        if (pau != null && !pau.equals("null")) {
            boolean p = Boolean.parseBoolean(pau);
            if (p != paused) {
                paused = p;
                if (l != null) {
                    l.pausedChanged(p);
                }
            }
        }
    }

    private String get(String property) {
        try {
            return request("[\"get_property\",\"" + property + "\"]", true);
        }
        catch (IOException e) {
            return null;
        }
    }

    private boolean command(String commandJson) {
        try {
            return request(commandJson, false) != null;
        }
        catch (IOException e) {
            System.err.println("mpv: " + e.getMessage());
            return false;
        }
    }

    /**
     * Skickar ett kommando och väntar på svaret med samma request_id.
     * Händelser och andra svar som kommer emellan hoppas över.
     *
     * @return värdet i "data" om wantData, annars "success"; null vid fel
     */
    private String request(String commandJson, boolean wantData) throws IOException {
        synchronized (ipcLock) {
            if (ipc == null) {
                return null;
            }
            int id = nextRequestId++;
            String msg = "{\"command\":" + commandJson + ",\"request_id\":" + id + "}\n";
            ipc.write(msg.getBytes(StandardCharsets.UTF_8));

            String line;
            while ((line = ipc.readLine()) != null) {
                Matcher idm = REQUEST_ID.matcher(line);
                if (!idm.find() || Integer.parseInt(idm.group(1)) != id) {
                    continue;
                }
                Matcher em = ERROR.matcher(line);
                if (em.find() && !em.group(1).equals("success")) {
                    return null; // t.ex. "property unavailable" innan filen laddats
                }
                if (!wantData) {
                    return "success";
                }
                Matcher dm = DATA.matcher(line);
                return dm.find() ? dm.group(1) : null;
            }
            throw new IOException("mpv stängde IPC-anslutningen");
        }
    }
}
