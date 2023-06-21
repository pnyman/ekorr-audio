package se.camabomedia.ekorr.audio.playback;

import static org.junit.Assert.fail;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

import org.junit.Test;

import javafx.util.Duration;
import se.camabomedia.ekorr.FullText;
import se.camabomedia.ekorr.Ncc;
import se.camabomedia.ekorr.Smil;
import se.camabomedia.ekorr.SmilPar;
import se.camabomedia.ekorr.SmilAudio;


/**
 * MP3PlayerTest
 */
public class MP3PlayerTest {

    @Test
    public void test_Case() {
        String f = "CA60564_Box_PUBLISHER_fixgen/Daisy_202/06_MARS.mp3";
        URL url = getClass().getClassLoader().getResource(f);

        try {
            String file = Paths.get(url.toURI()).toString();
            System.out.println(file);

            MP3Player p = new MP3Player();
            p.play(file, 155, 1, 1);

            // Wait for some time to allow playback
            try {
                Thread.sleep(5000); // 5 seconds
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            p.stop();

            p.play(file, 20000, 2, 1/2.0);
            // Wait for some time to allow playback
            try {
                Thread.sleep(5000); // 5 seconds
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            p.stop();


        }
        catch (URISyntaxException e1) {
            e1.printStackTrace();
        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void test_WithInputFromSmil() {
// "C:/Users/qwfwq/programmering/ekorr/src/test/resources/CA60564_Box_PUBLISHER_fixgen/Daisy_202/ncc.html"
        String f = "CA60564_Box_PUBLISHER_fixgen/Daisy_202/ncc.html";
        URL url = getClass().getClassLoader().getResource(f);

        try {
            String file = Paths.get(url.toURI()).toString();
            Ncc ncc = new Ncc(file);
            FullText fullText = new FullText(ncc.getDirectory(), ncc.getContentFile());
            Smil smil = new Smil(ncc, fullText);

            // String tag1 = "dol_1_15_id_fixgen_9842";
            // String tag2 = "dol_1_15_id_fixgen_9843";
            // String tag3 = "dol_1_15_id_fixgen_9844";
            // String tag4 = "dol_1_15_id_fixgen_9845";
            // String tag5 = "dol_1_15_id_fixgen_9846";
            // String tag6 = "dol_1_15_id_fixgen_9847";

            String[] syncpoints = {
                "dol_1_15_id_fixgen_9842",
                "dol_1_15_id_fixgen_9843",
                "dol_1_15_id_fixgen_9844",
                "dol_1_15_id_fixgen_9845",
                "dol_1_15_id_fixgen_9846",
                "dol_1_15_id_fixgen_9847"
            };

            // String[] syncpoints = {
            //     "dol_1_15_id_fixgen_9843",
            //     "dol_1_15_id_fixgen_9844",
            //     "dol_1_15_id_fixgen_9845"
            // };


            MP3Player p = new MP3Player();

            for (String syncpoint : syncpoints) {
                SmilPar par = smil.getParFromSourceFileTagID(syncpoint);
                for (SmilAudio smilAudio : par.getSmilAudio()) {
                    String src = smilAudio.getSrc();
                    float begin = smilAudio.getClipBegin();
                    float end = smilAudio.getClipEnd();
                    String mediaFile = new File(ncc.getDirectory(), src).toString();

                    // p.play(mediaFile, begin, end, 1, 1);

                    // // Continuously retrieve and utilize the current position
                    // while (p.getCurrentPosition().lessThanOrEqualTo(Duration.millis(end))) {
                    //     // Do something with the current position, e.g., update UI, perform
                    //     // calculations, etc.
                    //     Duration currentPosition = p.getCurrentPosition();
                    //     // System.out.println("Current Position: " + currentPosition);

                    //     // Sleep or delay if needed to control the frequency of position updates
                    //     try {
                    //         Thread.sleep(100);
                    //     } catch (InterruptedException e) {
                    //         e.printStackTrace();
                    //     }
                    // }

                    // p.waitUntilFinished();
                    // p.pause();

                    // long sleepTime = (long) (end - begin);
                    // System.out.println(sleepTime);

                    // try {
                    //     Thread.sleep(sleepTime); // 5 seconds
                    // } catch (InterruptedException e) {
                    //     e.printStackTrace();
                    // }
                }
            }



        }
        // catch (URISyntaxException e1) {
        //     e1.printStackTrace();
        // }
        catch (Exception e) {
            fail(e.getMessage());
        }

    }

}
