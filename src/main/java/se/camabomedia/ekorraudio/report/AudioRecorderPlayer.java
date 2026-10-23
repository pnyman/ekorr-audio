package se.camabomedia.ekorraudio.report;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Spelar in och upp korta ljudkommentarer. Ljudet lagras som WAV kodat i
 * base64 (så som det sparas i rapporten).
 *
 * Inspelning: linjen öppnas direkt i startRecording (inte i en tråd), så ett
 * snabbt tryck–släpp kan inte stoppa en linje som inte finns än. En tråd läser
 * ljudet till minnet; stopRecording väntar in tråden innan ljudet kodas, så
 * inget kapas i slutet. Ingen temporärfil används.
 *
 * Alla publika metoder anropas från JavaFX-tråden.
 */
public class AudioRecorderPlayer {

    /** 11 025 Hz, 16 bitar, mono – litet nog för att ligga i rapporten. */
    private static final AudioFormat FORMAT = new AudioFormat(11025, 16, 1, true, false);

    /** Kortare inspelningar än så räknas som felklick och kastas. */
    private static final long MIN_RECORDING_MS = 300;

    public static class NoMicrophoneException extends Exception {
        public NoMicrophoneException() {
            super("Hittar ingen aktiv mikrofon.");
        }

        public NoMicrophoneException(String message) {
            super(message);
        }
    }

    private final Object lock = new Object();

    // Inspelning
    private TargetDataLine recordingLine;
    private Thread recordingThread;
    private ByteArrayOutputStream recorded;
    private volatile boolean recording = false;

    // Uppspelning
    private SourceDataLine playingLine;
    private volatile boolean playing = false;

    private String base64EncodedAudio = null;

    /** reportDirectory används inte längre (ingen temporärfil), behålls för anroparna. */
    public AudioRecorderPlayer(File reportDirectory) {
    }

    // ---------------------------------------------------------------- inspelning

    /**
     * Börjar spela in. Avbryter ev. uppspelning. Kastar NoMicrophoneException
     * om ingen mikrofon finns eller om den används av ett annat program.
     */
    public void startRecording() throws NoMicrophoneException {
        synchronized (lock) {
            if (recording) {
                return;
            }
            stopPlaying();

            DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
            if (!AudioSystem.isLineSupported(info)) {
                throw new NoMicrophoneException();
            }
            TargetDataLine line;
            try {
                line = (TargetDataLine) AudioSystem.getLine(info);
                line.open(FORMAT);
            }
            catch (LineUnavailableException | IllegalArgumentException e) {
                throw new NoMicrophoneException("Mikrofonen kan inte användas: " + e.getMessage());
            }
            line.start();

            recordingLine = line;
            recorded = new ByteArrayOutputStream();
            recording = true;

            ByteArrayOutputStream out = recorded;
            recordingThread = new Thread(() -> {
                byte[] buffer = new byte[4096];
                // read blockerar medan linjen går. När den stoppats returnerar
                // read det som finns kvar i bufferten och sedan 0 – då är vi klara.
                while (true) {
                    int n = line.read(buffer, 0, buffer.length);
                    if (n <= 0) {
                        break;
                    }
                    out.write(buffer, 0, n);
                }
            }, "audio-recorder");
            recordingThread.setDaemon(true);
            recordingThread.start();
        }
    }

    /**
     * Slutar spela in och kodar ljudet. En för kort inspelning (felklick)
     * kastas, och ett tidigare ljud behålls då.
     */
    public void stopRecording() {
        synchronized (lock) {
            if (!recording) {
                return;
            }
            recording = false;
            // Stoppa först och låt tråden läsa ut resten av bufferten – stänger
            // man direkt slängs det sista som spelats in
            recordingLine.stop();
            try {
                recordingThread.join(2000);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            recordingLine.close();   // släpper också en read som mot förmodan väntar kvar
            recordingLine = null;
            recordingThread = null;

            byte[] pcm = recorded.toByteArray();
            recorded = null;
            long ms = pcm.length * 1000L / (long) (FORMAT.getFrameRate() * FORMAT.getFrameSize());
            if (ms < MIN_RECORDING_MS) {
                return;
            }
            try {
                base64EncodedAudio = Base64.getEncoder().encodeToString(toWav(pcm));
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /** Rå PCM -> WAV-fil i minnet (med korrekt längd i huvudet). */
    private static byte[] toWav(byte[] pcm) throws IOException {
        long frames = pcm.length / FORMAT.getFrameSize();
        try (AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(pcm), FORMAT, frames)) {
            ByteArrayOutputStream wav = new ByteArrayOutputStream(pcm.length + 64);
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, wav);
            return wav.toByteArray();
        }
    }

    // ---------------------------------------------------------------- uppspelning

    /** Spelar upp det inspelade ljudet. Startar om från början om det redan spelas. */
    public void playFromBase64() {
        synchronized (lock) {
            if (base64EncodedAudio == null || recording) {
                return;
            }
            stopPlaying();

            AudioInputStream ais;
            SourceDataLine line;
            try {
                byte[] audio = Base64.getDecoder().decode(base64EncodedAudio);
                ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(audio));
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, ais.getFormat());
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(ais.getFormat());
            }
            catch (UnsupportedAudioFileException | IOException | LineUnavailableException
                   | IllegalArgumentException e) {
                e.printStackTrace();
                return;
            }
            line.start();
            playingLine = line;
            playing = true;

            Thread t = new Thread(() -> {
                byte[] buffer = new byte[4096];
                try (AudioInputStream in = ais) {
                    int n;
                    while (playing && (n = in.read(buffer)) != -1) {
                        line.write(buffer, 0, n);
                    }
                    if (playing) {
                        line.drain();   // spela klart det som ligger i bufferten
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                finally {
                    synchronized (lock) {
                        if (playingLine == line) {
                            line.close();
                            playingLine = null;
                            playing = false;
                        }
                    }
                }
            }, "audio-player");
            t.setDaemon(true);
            t.start();
        }
    }

    /** Avbryter uppspelningen (om något spelas). */
    public void stopPlaying() {
        synchronized (lock) {
            playing = false;
            if (playingLine != null) {
                playingLine.stop();
                playingLine.flush();
                playingLine.close();   // släpper en write som väntar
                playingLine = null;
            }
        }
    }

    // ---------------------------------------------------------------- övrigt

    public void deleteAudio() {
        synchronized (lock) {
            if (recording) {
                // Avbryt inspelningen utan att spara den
                recording = false;
                recordingLine.stop();
                recordingLine.close();
                recordingLine = null;
                recordingThread = null;
                recorded = null;
            }
            stopPlaying();
            base64EncodedAudio = null;
        }
    }

    public String getWavData() {
        return base64EncodedAudio;
    }

    public void setWavData(String base64EncodedAudio) {
        this.base64EncodedAudio = base64EncodedAudio;
    }
}
