package se.camabomedia.ekorr.audio.playback;

import static org.junit.Assert.fail;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;


import org.junit.Test;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;
import se.camabomedia.ekorr.Content;
import se.camabomedia.ekorr.Ncc;
import se.camabomedia.ekorr.Smil;
import se.camabomedia.ekorr.SmilPar;
import se.camabomedia.ekorr.SmilAudio;


/**
 * MP3PlayerTest
 */
public class MP3PlayerTest {

    @Test
    public void test_PlayAndRetrievePosition() {
        // select talking book
        String f = "CA60564_Box_PUBLISHER_fixgen/Daisy_202/ncc.html";
        URL url = getClass().getClassLoader().getResource(f);

        // starting point
        String startId = "dol_1_15_id_fixgen_9770";

        try {
            String file = Paths.get(url.toURI()).toString();
            Ncc ncc = new Ncc(file);
            Content content = new Content(ncc.getDirectory(), ncc.getContentFile());
            Smil smil = new Smil(ncc, content);
            MP3Player p = new MP3Player();


            // get mp3file and start time
            String mp3File = smil.mp3FileFromContentId(startId);
            System.out.println(mp3File);
            String mp3FileName = Paths.get(mp3File).getFileName().toString();

            int startTime = smil.startTimeFromContentId(startId);
            System.out.println(startTime);

            int endTime = smil.endTimeFromContentId(startId);
            System.out.println(endTime);

            final ChangeListener<Number> changeListener = new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> observable,
                                        Number oldValue,
                                        Number newValue) {
                        // System.out.println("oldValue: "+ oldValue + ", newValue: " + newValue);
                        System.out.println(smil.contentIdFromTime(mp3FileName, (int)newValue));
                    }
                };

            IntegerProperty currentTimeProperty = p.currentTimeProperty();
            currentTimeProperty.addListener(changeListener);

            p.play(mp3File, startTime, endTime, 1, 1);
            p.waitUntilFinished();
            p.stop();


        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }

}
