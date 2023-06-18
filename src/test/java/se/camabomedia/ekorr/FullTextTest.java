package se.camabomedia.ekorr;

import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

import org.junit.Test;


/**
 * FullTextTest
 */
public class FullTextTest {

    @Test
    public void test_Constructor() {
        String f = "V005414_fixgen/ncc.html";
        URL url = getClass().getClassLoader().getResource(f);

        try {
            String file = Paths.get(url.toURI()).toString();
            Ncc ncc = new Ncc(file);
            Smil smil = new Smil(ncc);
            FullText fullText = new FullText(ncc.getDirectory(), smil.getSourceFile());
            String tagID = "id_fixgen_5326";
            System.out.println(fullText.getTagText(tagID));
        }
        catch (URISyntaxException e1) {
            e1.printStackTrace();
        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
