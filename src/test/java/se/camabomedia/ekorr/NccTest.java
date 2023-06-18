package se.camabomedia.ekorr;

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
    public void test_Constructor() {
        String f = "V005414_fixgen/ncc.html";
        URL url = getClass().getClassLoader().getResource(f);

        try {
            String file = Paths.get(url.toURI()).toString();
            Ncc ncc = new Ncc(file);

            System.out.println(ncc.getTitle()
                               + " " + ncc.getCreators()
                               + " " + ncc.getIdentifier());
            System.out.println();

            for (NccItem item : ncc.getNccItems()) {
                System.out.println(item);
                System.out.println();
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
