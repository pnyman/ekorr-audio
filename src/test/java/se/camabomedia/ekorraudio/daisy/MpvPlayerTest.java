package se.camabomedia.ekorraudio.daisy;

import static org.junit.Assert.fail;

import java.net.URL;
import java.nio.file.Paths;

import org.junit.Test;

import javafx.beans.property.IntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/**
 * MpvPlayerTest
 */
public class MpvPlayerTest {

    @Test
    public void test_PlayAndRetrievePosition() {
        // select talking book
        String file = "c:/Users/qwfwq/camabo-workspace/leverans/CA60564_Box_PUBLISHER_fixgen/Daisy_202/ncc.html";

        // starting point
        String startId = "dol_1_15_id_fixgen_9770";

        try {
            Ncc ncc = new Ncc(file);
            Content content = new Content(ncc.getDirectory(), ncc.getContentFile());
            Smil smil = new Smil(ncc, content);
            MpvPlayer p = new MpvPlayer();

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
                        System.out.println("oldValue: "+ oldValue + ", newValue: " + newValue);
                        // System.out.println(smil.contentIdFromTime(mp3FileName, (int)newValue));
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
