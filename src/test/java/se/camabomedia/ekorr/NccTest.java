package se.camabomedia.ekorr;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Unit test for ParseNcc.
 */
public class NccTest
{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {
        assertTrue( true );
    }


    @Test
    public void test_Constructor() {
        String file = "c:/Users/qwfwq/filfixaren/workspace/vagen_till_c/V005414_fixgen/ncc.html";
        try {
            Ncc ncc = new Ncc(file);

            System.out.println(ncc.title + " " + ncc.creators + " " + ncc.identifier);


            // for (NccItem item : ncc.nccItems) {
            //     System.out.println(item);
            // }

        }
        catch (Exception e) {
            fail(e.getMessage());
        }

    }

}
