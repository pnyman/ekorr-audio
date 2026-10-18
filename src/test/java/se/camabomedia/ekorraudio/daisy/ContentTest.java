package se.camabomedia.ekorraudio.daisy;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

import org.junit.Test;


/**
 * ContentTest
 */
public class ContentTest {

    @Test
    public void test_Constructor() {
        String f = "CA60564_Box_PUBLISHER_fixgen/Daisy_202/ncc.html";
        URL url = getClass().getClassLoader().getResource(f);
        String contentID = "dol_1_15_id_fixgen_9847";
        String expected = "Det var satan till att vara högljudd.";

        try {
            String file = Paths.get(url.toURI()).toString();
            Ncc ncc = new Ncc(file);
            Content content = new Content(ncc.getDirectory(), ncc.getContentFile());
            // Smil smil = new Smil(ncc, content);
            assertTrue(content.getTagText(contentID).equals(expected));
        }
        catch (URISyntaxException e1) {
            e1.printStackTrace();
        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
