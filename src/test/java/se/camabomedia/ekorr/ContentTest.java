package se.camabomedia.ekorr;

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
        String f = "V005414_fixgen/ncc.html";
        URL url = getClass().getClassLoader().getResource(f);

        try {
            String file = Paths.get(url.toURI()).toString();
            Ncc ncc = new Ncc(file);
            Content content = new Content(ncc.getDirectory(), ncc.getContentFile());
            Smil smil = new Smil(ncc, content);
            String tagID = "id_fixgen_5326";
            // System.out.println(content.getTagText(tagID));
        }
        catch (URISyntaxException e1) {
            e1.printStackTrace();
        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
