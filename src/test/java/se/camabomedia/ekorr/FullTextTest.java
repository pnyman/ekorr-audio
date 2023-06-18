package se.camabomedia.ekorr;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Objects;

import org.junit.Test;


/**
 * FullTextTest
 */
public class FullTextTest {

    @Test
    public void test_Constructor() {
        String file = "c:/Users/qwfwq/filfixaren/workspace/vagen_till_c/V005414_fixgen/ncc.html";
        try {
            Ncc ncc = new Ncc(file);
            Smil smil = new Smil(ncc);
            FullText fullText = new FullText(ncc.getDirectory(), smil.getSourceFile());
            String tagID = "id_fixgen_5326";
            System.out.println(fullText.getTagText(tagID));

        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
