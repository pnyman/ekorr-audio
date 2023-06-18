package se.camabomedia.ekorr;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Objects;

import org.junit.Test;

/**
 * SmilTest
 */
public class SmilTest {

    @Test
    public void test_Constructor() {
        // String file = "c:/Users/qwfwq/filfixaren/workspace/vagen_till_c/V005414_fixgen/ncc.html";

        String file = "c:/Users/qwfwq/filfixaren/workspace/leverans/CA60564_Box_PUBLISHER_fixgen/Daisy_202/ncc.html";

        try {
            Ncc ncc = new Ncc(file);
            Smil smil = new Smil(ncc.directory, ncc.smilFiles);

            SmilPar par1 = smil.getParFromParId("par_fixgen_11");
            System.out.println(par1);

            SmilPar par2 = smil.getParFromTextId("txt_fixgen_11");
            SmilPar par3 = smil.getParFromTextId(par1.getTextElementId());
            SmilPar par4 = smil.getParFromSourceFileTagId(par1.getSourceFileTagId());

            assertTrue(Objects.equals(par1, par2));
            assertTrue(Objects.equals(par1, par3));
            assertTrue(Objects.equals(par1, par4));
        }
        catch (Exception e) {
            fail(e.getMessage());
        }

    }


    @Test
    public void test_nccItemsTextfileIds() {
        String file = "c:/Users/qwfwq/filfixaren/workspace/leverans/CA60564_Box_PUBLISHER_fixgen/Daisy_202/ncc.html";

        try {
            Ncc ncc = new Ncc(file);
            Smil smil = new Smil(ncc.directory, ncc.smilFiles);

            for (NccItem item : ncc.nccItems) {
                SmilPar par = smil.getParFromParId(item.parId);
                String textId = par.getSourceFileTagId();
                System.out.println(item.text + " " + textId);
            }

        }
        catch (Exception e) {
            fail(e.getMessage());
        }

    }

}
