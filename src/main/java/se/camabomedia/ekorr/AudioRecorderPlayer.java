package se.camabomedia.ekorr;

import java.io.File;
import java.io.IOException;

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
    private String soundFileName;
    private File soundFile;

    AudioRecorderPlayer(File reportDirectory) {
        soundFileName = "recorded_audio.wav";
        soundFile = new File(reportDirectory, soundFileName);
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
                        22050, // sampleRate
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

    public void stopRecording() {
        if (recording) {
            recordingLine.stop();
            recordingLine.close();
            recording = false;
        }
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

    public void stopPlaying() {
        if (playing) {
            playingLine.stop();
            playingLine.close();
            playing = false;
        }
    }

    public void deleteSoundFile() {
        stopRecording();
        stopPlaying();
        if (soundFile.exists()) {
            soundFile.delete();
        }
    }

    public String getSoundFileName() {
        if (soundFile.exists()) {
            return soundFileName;
        }
        else {
            return null;
        }
    }

}
