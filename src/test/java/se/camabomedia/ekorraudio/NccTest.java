package se.camabomedia.ekorraudio;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

import org.junit.Test;

/**
 * Unit test for ParseNcc.
 */
public class NccTest {

    @Test
    public void test_nccItemContentID() {
        String f = "CA60564_Box_PUBLISHER_fixgen/Daisy_202/ncc.html";
        URL url = getClass().getClassLoader().getResource(f);
        String expected = "Kvibille 1982";
        String parID = "par_fixgen_1901";

        try {
            String file = Paths.get(url.toURI()).toString();
            Ncc ncc = new Ncc(file);
            // Content content = new Content(ncc.getDirectory(), ncc.getContentFile());
            // Smil smil = new Smil(ncc, content);

            NccItem item = ncc.getNccItem(parID);
            assertTrue(item.getText().equals(expected));

        }
        catch (URISyntaxException e1) {
            e1.printStackTrace();
        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }

}
