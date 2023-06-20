package se.camabomedia.ekorr.audio.playback;

import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

import org.junit.Test;


/**
 * MP3PlayerTest
 */
public class MP3PlayerTest {

    @Test
    public void test_Case() {

        String f = "CA60564_Box_PUBLISHER_fixgen/Daisy_202/06_MARS.mp3";
        URL url = getClass().getClassLoader().getResource(f);

        // 5346303.0 ms

        try {
            String file = Paths.get(url.toURI()).toString();
            file = "file:///" + file.replace("\\", "/");
            // file = "file:///" + file;
            System.out.println(file);

            MP3Player p = new MP3Player();
            p.play(file, 155 , 1, 1);

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

        }
        catch (URISyntaxException e1) {
            e1.printStackTrace();
        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }

}
