package se.camabomedia.ekorr;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Objects;

import org.junit.Test;

import junit.framework.TestResult;

/**
 * SmilTest
 */
public class SmilTest {

    @Test
    public void test_Constructor() {
        String file = "c:/Users/qwfwq/filfixaren/workspace/vagen_till_c/V005414_fixgen/ncc.html";
        try {
            Ncc ncc = new Ncc(file);
            Smil smil = new Smil(ncc.directory, ncc.smilFiles);

            SmilPar par = smil.parElementIdTable.get("par_fixgen_2");
            System.out.println(par);

            SmilPar text = smil.textElementIdTable.get("txt_fixgen_2");
            SmilPar txt = smil.textElementIdTable.get(par.textElementId);

            assertTrue(Objects.equals(par, text));
            assertTrue(Objects.equals(par, txt));
        }
        catch (Exception e) {
            fail(e.getMessage());
        }

    }

}
