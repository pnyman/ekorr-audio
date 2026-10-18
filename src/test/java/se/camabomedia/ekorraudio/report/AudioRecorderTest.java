package se.camabomedia.ekorraudio.report;

import static org.junit.Assert.fail;

import java.net.URL;
import java.nio.file.Paths;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
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


/**
 * AudioRecorderTest
 */
public class AudioRecorderTest {

    @Test
    public void test_Mixer() {

        try {
            Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();    //get available mixers
            System.out.println("Available mixers:");
            Mixer mixer = null;
            for (int cnt = 0; cnt < mixerInfo.length; cnt++) {
                System.out.println(cnt + " " + mixerInfo[cnt].getName());
                mixer = AudioSystem.getMixer(mixerInfo[cnt]);

                Line.Info[] lineInfos = mixer.getTargetLineInfo();
                if (lineInfos.length >= 1 && lineInfos[0].getLineClass().equals(TargetDataLine.class)) {
                    System.out.println(cnt + " Mic is supported!");
                    break;
                }
            }

        }

        catch (Exception e) {
            fail(e.getMessage());
        }

    }


    // https://docs.oracle.com/javase/8/docs/api/javax/sound/sampled/AudioFormat.html


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

    @Test
    public void test_Recording() {
        try {

            if (!isMicrophoneAvailable()) {
                System.out.println("No microphone available on this system.");
                return;
            }

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
            TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();

            // Create audio input stream
            AudioInputStream ais = new AudioInputStream(line);

            // Specify output file
            File outputFile = new File("recorded_audio.wav");

            // Start recording
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, outputFile);

            // Stop recording
            line.stop();
            line.close();
        }

        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
