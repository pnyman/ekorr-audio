package se.camabomedia.ekorr.audio.playback;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.util.Duration;

/**
 * Spelar upp ljud med mpv som extern process.
 *
 * Varje anrop till play() startar en ny mpv-process med --start/--end/--speed/--volume.
 * Positionen läses via mpv:s JSON-IPC (named pipe på Windows) var 100:e ms och
 * publiceras i currentTimeProperty() på JavaFX-tråden.
 */
public class MpvPlayer {

    private static final Pattern DATA_PATTERN = Pattern.compile("\"data\"\\s*:\\s*(-?[0-9.eE+\\-]+)");
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("\"request_id\"\\s*:\\s*(\\d+)");

    String mpvPath = "C:/Users/qwfwq/scoop/apps/mpv/0.41.0/mpv.com";

    public volatile Duration currentPosition = Duration.ZERO;
    private final IntegerProperty currentTime;

    private volatile Session session;

    public MpvPlayer() {
        this.currentTime = new SimpleIntegerProperty(this, "currentTime");
        // JavaFX behövs fortfarande för currentTimeProperty (Platform.runLater)
        if (!Platform.isFxApplicationThread()) {
            try {
                Platform.startup(() -> {});
            } catch (IllegalStateException alreadyStarted) {
                // Plattformen är redan igång – inget att göra
            }
        }
    }

    public Integer getCurrentTime() {
        return currentTime.get();
    }

    public void setCurrentTime(Integer s) {
        currentTime.set(s);
    }

    public IntegerProperty currentTimeProperty() {
        return this.currentTime;
    }

    /**
     * Startar uppspelning (blockerar inte).
     *
     * @param mediaFile sökväg till filen
     * @param startTime starttid i millisekunder
     * @param endTime   sluttid i millisekunder (<= startTime betyder spela till slutet)
     * @param speed     uppspelningshastighet (1.0 = normal)
     * @param volume    volym 0.0–1.0 (som i JavaFX MediaPlayer)
     */
    public void play(String mediaFile, double startTime, double endTime, double speed, double volume) {
        stop();

        System.out.println(mediaFile + ": " + startTime + " " + endTime);

        String pipeName = "\\\\.\\pipe\\ekorr-mpv-" + ProcessHandle.current().pid() + "-" + System.nanoTime();

        List<String> cmd = new ArrayList<>();
        cmd.add(mpvPath);
        cmd.add("--no-video");
        cmd.add("--really-quiet");
        cmd.add("--input-terminal=no");
        cmd.add("--idle=no");
        cmd.add("--keep-open=no");
        cmd.add("--input-ipc-server=" + pipeName);
        cmd.add("--start=" + seconds(startTime));
        if (endTime > startTime) {
            cmd.add("--end=" + seconds(endTime));
        }
        cmd.add("--speed=" + fmt(speed));
        cmd.add("--volume=" + fmt(Math.max(0, Math.min(1, volume)) * 100));
        cmd.add("--");
        cmd.add(mediaFile);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            System.err.println("Kunde inte starta mpv: " + e.getMessage());
            // Tom, redan avslutad session så att waitUntilFinished() inte hänger
            Session failed = new Session(null, pipeName);
            failed.done.countDown();
            session = failed;
            return;
        }

        Session s = new Session(process, pipeName);
        session = s;
        s.start();
    }

    public Duration getCurrentPosition() {
        Session s = session;
        if (s != null && s.isRunning()) {
            return currentPosition;
        }
        return Duration.ZERO;
    }

    public void waitUntilFinished() {
        Session s = session;
        if (s == null) {
            return;
        }
        try {
            s.done.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }

    public void stop() {
        Session s = session;
        if (s != null) {
            s.terminate();
        }
    }

    public void pause() {
        sendPause(true);
    }

    public void resume() {
        sendPause(false);
    }

    private void sendPause(boolean pause) {
        Session s = session;
        if (s == null || !s.isRunning()) {
            return;
        }
        try {
            s.request("[\"set_property\",\"pause\"," + pause + "]");
        } catch (IOException e) {
            System.err.println("Kunde inte " + (pause ? "pausa" : "återuppta") + " mpv: " + e.getMessage());
        }
    }

    private static String seconds(double millis) {
        return fmt(millis / 1000.0);
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    /** En mpv-process och dess IPC-anslutning. */
    private final class Session {
        final Process process;
        final String pipeName;
        final CountDownLatch done = new CountDownLatch(1);
        private final ScheduledExecutorService poller;
        private RandomAccessFile ipc;
        private int nextRequestId = 1;

        Session(Process process, String pipeName) {
            this.process = process;
            this.pipeName = pipeName;
            this.poller = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "mpv-position-poller");
                t.setDaemon(true);
                return t;
            });
        }

        void start() {
            process.onExit().thenRun(() -> {
                poller.shutdownNow();
                closeIpc();
                done.countDown();
            });
            poller.scheduleAtFixedRate(this::pollPosition, 50, 100, TimeUnit.MILLISECONDS);
        }

        boolean isRunning() {
            return process != null && process.isAlive();
        }

        void terminate() {
            if (process != null && process.isAlive()) {
                // Att döda processen stänger pipen, så en blockerad läsning släpper
                process.destroy();
            }
        }

        private void pollPosition() {
            try {
                String value = request("[\"get_property\",\"time-pos\"]");
                if (value != null) {
                    double ms = Double.parseDouble(value) * 1000.0;
                    currentPosition = Duration.millis(ms);
                    int msInt = (int) ms;
                    Platform.runLater(() -> setCurrentTime(msInt));
                }
            } catch (IOException | NumberFormatException e) {
                // mpv har avslutats eller svarade konstigt – onExit städar upp
            }
        }

        /**
         * Skickar ett kommando och väntar på svaret med samma request_id.
         * Returnerar fältet "data" som sträng, eller null om det saknas
         * (t.ex. innan filen laddats eller om pipen inte finns än).
         */
        synchronized String request(String commandJson) throws IOException {
            if (!ensureConnected()) {
                return null;
            }
            int id = nextRequestId++;
            String msg = "{\"command\":" + commandJson + ",\"request_id\":" + id + "}\n";
            ipc.write(msg.getBytes(StandardCharsets.UTF_8));

            String line;
            while ((line = ipc.readLine()) != null) {
                Matcher idMatcher = REQUEST_ID_PATTERN.matcher(line);
                if (idMatcher.find() && Integer.parseInt(idMatcher.group(1)) == id) {
                    Matcher dataMatcher = DATA_PATTERN.matcher(line);
                    return dataMatcher.find() ? dataMatcher.group(1) : null;
                }
                // Andra rader är händelser (file-loaded, playback-restart osv.) – hoppa över
            }
            throw new IOException("mpv stängde IPC-anslutningen");
        }

        private boolean ensureConnected() {
            if (ipc != null) {
                return true;
            }
            if (!isRunning()) {
                return false;
            }
            try {
                ipc = new RandomAccessFile(pipeName, "rw");
                return true;
            } catch (FileNotFoundException e) {
                // mpv har inte skapat pipen än – försök igen vid nästa poll
                return false;
            }
        }

        private void closeIpc() {
            RandomAccessFile r = ipc;
            ipc = null;
            if (r != null) {
                try {
                    r.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
