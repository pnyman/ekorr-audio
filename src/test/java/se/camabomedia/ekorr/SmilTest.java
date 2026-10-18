package se.camabomedia.ekorr;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;

import org.junit.Test;

/**
 * SmilTest
 */
public class SmilTest {
    String file = "c:/Users/qwfwq/camabo-workspace/leverans/CA60564_Box_PUBLISHER_fixgen/Daisy_202/ncc.html";

    @Test
    public void test_Constructor() {
        try {
            Ncc ncc = new Ncc(file);
            Content content = new Content(ncc.getDirectory(), ncc.getContentFile());
            Smil smil = new Smil(ncc, content);

            SmilPar par1 = smil.getParFromParID("par_fixgen_11");
            // System.out.println(par1);

            SmilPar par2 = smil.getParFromTextID("txt_fixgen_11");
            SmilPar par3 = smil.getParFromTextID(par1.getTextElementID());
            SmilPar par4 = smil.getParFromContentID(par1.getContentID());

            assertTrue(Objects.equals(par1, par2));
            assertTrue(Objects.equals(par1, par3));
            assertTrue(Objects.equals(par1, par4));
        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void test_Case() {
        try {
            Ncc ncc = new Ncc(file);
            Content content = new Content(ncc.getDirectory(), ncc.getContentFile());
            System.out.println(">>>>>>>>>>>>>>>>");
            System.out.println(content.contentFile);
            Smil smil = new Smil(ncc, content);

        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }


    @Test
    public void test_breadCrumbs() {
        String id = "dol_1_7_id_fixgen_1901";
        ArrayList<String> expected = new ArrayList<String>();
        expected.add("MARS");
        expected.add("Kvibille 1982");

        try {
            Ncc ncc = new Ncc(file);
            Content content = new Content(ncc.getDirectory(), ncc.getContentFile());
            Smil smil = new Smil(ncc, content);
            ArrayList<String> breadCrumbs = smil.makeBreadCrumbs(id);
            assertTrue(breadCrumbs.equals(expected));
        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void test_PreviousAndNextPar() {
        Ncc ncc;
        Smil smil;
        SmilPar thisPar;
        SmilPar nextPar;
        SmilPar previousPar;
        Content content;

        try {
            ncc = new Ncc(file);
            content = new Content(ncc.getDirectory(), ncc.getContentFile());
            smil = new Smil(ncc, content);

            // First par in the talking book
            thisPar = smil.getParFromParID("par_fixgen_2");
            assertTrue(thisPar.getPage() == null);

            // Last par in the talking book
            thisPar = smil.getParFromParID("par_fixgen_19000");
            previousPar = thisPar.getPreviousSmilPar();
            nextPar = thisPar.getNextSmilPar();
            assertTrue(previousPar.getParElementID().equals("par_fixgen_18999"));
            assertTrue(nextPar == null);

            // First par in a smil file
            thisPar = smil.getParFromParID("par_fixgen_18924");
            previousPar = thisPar.getPreviousSmilPar();
            nextPar = thisPar.getNextSmilPar();
            assertTrue(previousPar.getParElementID().equals("par_fixgen_18921"));
            assertTrue(nextPar.getParElementID().equals("par_fixgen_18925"));

            // Last par in a smil file
            thisPar = smil.getParFromParID("par_fixgen_18921");
            previousPar = thisPar.getPreviousSmilPar();
            nextPar = thisPar.getNextSmilPar();
            assertTrue(previousPar.getParElementID().equals("par_fixgen_18920"));
            assertTrue(nextPar.getParElementID().equals("par_fixgen_18924"));
        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void test_Timestamps() {
        try {
            Ncc ncc = new Ncc(file);
            Content content = new Content(ncc.getDirectory(), ncc.getContentFile());
            Smil smil = new Smil(ncc, content);

            // gram0015.smil
            // <audio src="15_JUNI.mp3" clip-begin="npt=239.616s" clip-end="npt=243.875s" id="audio_0080"/>
            String contentId = "dol_1_15_id_fixgen_9843";
            String mp3 = "15_JUNI.mp3";
            int time = 241616;

            assertTrue(smil.contentIdFromTime(mp3, time).equals("dol_1_15_id_fixgen_9843"));
            assertTrue(smil.startTimeFromContentId(contentId) == 239616);
            assertTrue(smil.startTimeFromContentId(smil.contentIdFromTime(mp3, time)) == 239616);
        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void test_getContentIdFromTime() {
        // starting point
        String startId = "dol_1_15_id_fixgen_9770";

        try {
            Ncc ncc = new Ncc(file);
            Content content = new Content(ncc.getDirectory(), ncc.getContentFile());
            Smil smil = new Smil(ncc, content);

            // get mp3file absolute path and start time
            String mp3File = smil.mp3FileFromContentId(startId);
            System.out.println(mp3File);
            int startTime = smil.startTimeFromContentId(startId);
            System.out.println(startTime);

            // mp3File is the absolute path, we need to get the file name only
            String mp3FileName = Paths.get(mp3File).getFileName().toString();

            // 9057
            System.out.println(smil.contentIdFromTime(mp3FileName, 9057));
        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }


}
