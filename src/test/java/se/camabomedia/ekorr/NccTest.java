package se.camabomedia.ekorr;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Unit test for ParseNcc.
 */
public class NccTest {

    @Test
    public void test_Constructor() {
        String file = "c:/Users/qwfwq/filfixaren/workspace/vagen_till_c/V005414_fixgen/ncc.html";
        try {
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
        catch (Exception e) {
            fail(e.getMessage());
        }

    }

}
