package se.camabomedia.ekorr;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;

import javax.script.SimpleScriptContext;

import org.junit.Test;

/**
 * SmilTest
 */
public class SmilTest {

    @Test
    public void test_Constructor() {
        String f = "CA60564_Box_PUBLISHER_fixgen/Daisy_202/ncc.html";
        URL url = getClass().getClassLoader().getResource(f);

        try {
            String file = Paths.get(url.toURI()).toString();
            Ncc ncc = new Ncc(file);
            Smil smil = new Smil(ncc);

            SmilPar par1 = smil.getParFromParID("par_fixgen_11");
            // System.out.println(par1);

            SmilPar par2 = smil.getParFromTextID("txt_fixgen_11");
            SmilPar par3 = smil.getParFromTextID(par1.getTextElementID());
            SmilPar par4 = smil.getParFromContentTagID(par1.getSourceFileTagID());

            assertTrue(Objects.equals(par1, par2));
            assertTrue(Objects.equals(par1, par3));
            assertTrue(Objects.equals(par1, par4));
        }
        catch (URISyntaxException e1) {
            e1.printStackTrace();
        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void test_nccItemsTextfileIDs() {
        String f = "CA60564_Box_PUBLISHER_fixgen/Daisy_202/ncc.html";
        URL url = getClass().getClassLoader().getResource(f);

        try {
            String file = Paths.get(url.toURI()).toString();
            Ncc ncc = new Ncc(file);
            Smil smil = new Smil(ncc);

            for (NccItem item : ncc.getNccItems()) {
                SmilPar par = smil.getParFromParID(item.parID);
                String textID = par.getSourceFileTagID();
                // System.out.println(item.text + " " + textID);
            }
        }
        catch (URISyntaxException e1) {
            e1.printStackTrace();
        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void test_breadCrumbs() {
        String f = "V005414_fixgen/ncc.html";
        URL url = getClass().getClassLoader().getResource(f);

        try {
            String file = Paths.get(url.toURI()).toString();
            Ncc ncc = new Ncc(file);
            Smil smil = new Smil(ncc);
            String id = "id_fixgen_5326";
            ArrayList<String> breadCrumbs = smil.getBreadCrumbs("id_fixgen_5326");
            // System.out.println();
            // System.out.println(breadCrumbs);
        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void test_PreviousAndNext() {
        String f = "V005414_fixgen/ncc.html";
        URL url = getClass().getClassLoader().getResource(f);
        String file;
        Ncc ncc;
        Smil smil;
        SmilPar thisPar;
        SmilPar previousPar;
        SmilPar nextPar;

        try {
            file = Paths.get(url.toURI()).toString();
            ncc = new Ncc(file);
            smil = new Smil(ncc);

            // First par in the talking book
            thisPar = smil.getParFromParID("par_fixgen_2");
            previousPar = thisPar.getPreviousSmilPar();
            nextPar = thisPar.getNextSmilPar();
            assertTrue(previousPar == null);
            assertTrue(nextPar.getParElementID().equals("par_fixgen_3"));

            // Last par in the talking book
            thisPar = smil.getParFromParID("par_fixgen_9677");
            previousPar = thisPar.getPreviousSmilPar();
            nextPar = thisPar.getNextSmilPar();
            assertTrue(previousPar.getParElementID().equals("par_fixgen_9676"));
            assertTrue(nextPar == null);

            // First par in a smil file
            thisPar = smil.getParFromParID("par_fixgen_255");
            previousPar = thisPar.getPreviousSmilPar();
            nextPar = thisPar.getNextSmilPar();
            assertTrue(previousPar.getParElementID().equals("par_fixgen_254"));
            assertTrue(nextPar.getParElementID().equals("par_fixgen_256"));

            // Last par in a smil file
            thisPar = smil.getParFromParID("par_fixgen_274");
            previousPar = thisPar.getPreviousSmilPar();
            nextPar = thisPar.getNextSmilPar();
            assertTrue(previousPar.getParElementID().equals("par_fixgen_273"));
            assertTrue(nextPar.getParElementID().equals("par_fixgen_275"));
        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void test_PageNumber() {
        String f = "V005414_fixgen/ncc.html";
        URL url = getClass().getClassLoader().getResource(f);
        String file;
        Ncc ncc;
        Smil smil;
        SmilPar thisPar;
        Content content;

        try {
            file = Paths.get(url.toURI()).toString();
            ncc = new Ncc(file);
            content = new Content(ncc.getDirectory(), ncc.getContentFile());
            smil = new Smil(ncc, content);

            // First par in the talking book
            thisPar = smil.getParFromParID("par_fixgen_2");
            assertTrue(thisPar.getPage() == null);


            thisPar = smil.getParFromParID("par_fixgen_255");
            System.out.println(thisPar);

            // // Last par in the talking book
            // thisPar = smil.getParFromParID("par_fixgen_9677");
            // previousPar = thisPar.getPreviousSmilPar();
            // nextPar = thisPar.getNextSmilPar();
            // assertTrue(previousPar.getParElementID().equals("par_fixgen_9676"));
            // assertTrue(nextPar == null);

            // // First par in a smil file
            // thisPar = smil.getParFromParID("par_fixgen_255");
            // previousPar = thisPar.getPreviousSmilPar();
            // nextPar = thisPar.getNextSmilPar();
            // assertTrue(previousPar.getParElementID().equals("par_fixgen_254"));
            // assertTrue(nextPar.getParElementID().equals("par_fixgen_256"));

            // // Last par in a smil file
            // thisPar = smil.getParFromParID("par_fixgen_274");
            // previousPar = thisPar.getPreviousSmilPar();
            // nextPar = thisPar.getNextSmilPar();
            // assertTrue(previousPar.getParElementID().equals("par_fixgen_273"));
            // assertTrue(nextPar.getParElementID().equals("par_fixgen_275"));
        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }

}
