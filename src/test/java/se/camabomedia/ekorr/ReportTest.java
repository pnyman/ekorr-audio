package se.camabomedia.ekorr;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;

import org.junit.Test;

/**
 * ReportTest
 */
public class ReportTest {
    // String nccFile = "CA60564_Box_PUBLISHER_fixgen/Daisy_202/ncc.html";
    String nccFile = "V005414_fixgen/ncc.html";
    URL url = getClass().getClassLoader().getResource(nccFile);

    @Test
    public void test_Create() {
        try {
            String file = Paths.get(url.toURI()).toString();
            Ncc ncc = new Ncc(file);
            Report r = new Report(ncc);

            r.addRemark("12", "Felläsning", "den första gång jag såg dig", "sög", "såg");
            r.addRemark("12", "Felläsning", "den första gång jag såg dig", "sög", "såg");
            r.addRemark("12", "Felläsning", "den första gång jag såg dig", "sög", "såg");

            r.createAndWriteReport();
        }
        catch (URISyntaxException e1) {
            e1.printStackTrace();
        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void test_Read() {
        try {
            String file = Paths.get(url.toURI()).toString();
            Ncc ncc = new Ncc(file);
            Report r = new Report(ncc);
            r.readReport();
            r.addRemark("15", "Felläsning", "att vara eller inte vara", "fara", "vara");
            r.createAndWriteReport();
        }
        catch (URISyntaxException e1) {
            e1.printStackTrace();
        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }

}
