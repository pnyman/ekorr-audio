package se.camabomedia.ekorr;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioRecorderPlayer {
    private TargetDataLine recordingLine;
    private SourceDataLine playingLine;
    private volatile boolean recording = false;
    private volatile boolean playing = false;
    private File soundFile;
    String base64EncodedAudio = null;

    AudioRecorderPlayer(File reportDirectory) {
        try {
            Path tempFilePath = Files.createTempFile("ekorr_audio", ".wav");
            soundFile = new File(tempFilePath.toString());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    boolean isMicrophoneAvailable() {
        // Get the available mixer devices
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        for (Mixer.Info info : mixerInfos) {
            Mixer mixer = AudioSystem.getMixer(info);
            Line.Info[] lineInfos = mixer.getTargetLineInfo();
            for (Line.Info lineInfo : lineInfos) {
                if (lineInfo instanceof TargetDataLine.Info) {
                    // Microphone is available
                    return true;
                }
            }
        }
        // No microphone found
        return false;
    }

    public void startRecording() {
        if (!isMicrophoneAvailable()) {
            System.out.println("No microphone available on this system.");
            return;
        }

        if (recording) {
            return;
        }

        Thread recordingThread = new Thread(() -> {
            try {
                stopPlaying();

                // Define audio format
                AudioFormat format = new AudioFormat(
                        11025, // sampleRate
                        16,    // sampleSizeInBits
                        1,     // channels
                        true,  // signed
                        false  // bigEndian
                );

                // Get target data line for recording
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                recordingLine = (TargetDataLine) AudioSystem.getLine(info);
                recordingLine.open(format);
                recordingLine.start();

                // Create audio input stream
                AudioInputStream ais = new AudioInputStream(recordingLine);

                // Start recording
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, soundFile);
            }
            catch (LineUnavailableException | IOException ex) {
                ex.printStackTrace();
            }
        });

        recordingThread.start();
        recording = true;
    }

    public void playRecording() {

        if (!soundFile.exists()) {
            return;
        }

        if (playing) {
            return;
        }

        Thread playThread = new Thread(() -> {
            try {
                stopRecording();

                // Get audio input stream from the recorded file
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundFile);

                // Get audio format
                AudioFormat format = audioInputStream.getFormat();

                // Set up data line info
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

                // Open the data line
                playingLine = (SourceDataLine) AudioSystem.getLine(info);
                playingLine.open(format);
                playingLine.start();

                // Create buffer for audio data
                byte[] buffer = new byte[4096];
                int bytesRead;

                // Play audio data
                while ((bytesRead = audioInputStream.read(buffer)) != -1) {
                    playingLine.write(buffer, 0, bytesRead);
                }

                // Close the data line and audio input stream
                playingLine.drain();
                playingLine.close();
                audioInputStream.close();
            }

            catch (LineUnavailableException | IOException | UnsupportedAudioFileException ex) {
                ex.printStackTrace();
            }

        });

        playThread.start();
        playing = true;
    }

    public void playFromBase64() {

        if (base64EncodedAudio == null) {
            return;
        }

        if (playing) {
            return;
        }

        Thread playThread = new Thread(() -> {
            try {
                // Decode base64 string to byte array
                byte[] audioData = Base64.getDecoder().decode(base64EncodedAudio);

                // Create audio input stream from byte array
                AudioInputStream ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(audioData));

                // Get audio format
                AudioFormat format = ais.getFormat();

                // Open data line for playback
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                playingLine = (SourceDataLine) AudioSystem.getLine(info);
                playingLine.open(format);
                playingLine.start();

                // Write audio data to data line for playback
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = ais.read(buffer)) != -1) {
                    playingLine.write(buffer, 0, bytesRead);
                }

                // Close data line
                playingLine.drain();
                playingLine.close();
                ais.close();
            }
            catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
                e.printStackTrace();
            }

        });

        playThread.start();
        playing = true;
    }

    public void encodeWavAsBase64() {
        try {
            byte[] fileContent = Files.readAllBytes(soundFile.toPath());
            base64EncodedAudio = Base64.getEncoder().encodeToString(fileContent);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopRecording() {
        if (recording) {
            recordingLine.stop();
            recordingLine.close();
            recording = false;
            encodeWavAsBase64();
        }
    }

    public void stopPlaying() {
        if (playing) {
            playingLine.stop();
            playingLine.close();
            playing = false;
        }
    }

    public void deleteAudio() {
        stopRecording();
        stopPlaying();
        base64EncodedAudio = null;
    }

    public String getWavData() {
        return base64EncodedAudio;
    }

    public void setWavData(String base64EncodedAudio) {
        this.base64EncodedAudio = base64EncodedAudio;
    }

}
