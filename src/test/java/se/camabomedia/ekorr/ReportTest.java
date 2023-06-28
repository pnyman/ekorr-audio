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

    @Test
    public void test_Case() {
        Report r = new Report(new File("C://Users/qwfwq/programmering/ekorr/devel"));

        // System.out.println(r.getDoc());

        r.addRemark("12", "Felläsning", "den första gång jag såg dig", "sög", "såg");
        r.addRemark("12", "Felläsning", "den första gång jag såg dig", "sög", "såg");
        r.addRemark("12", "Felläsning", "den första gång jag såg dig", "sög", "såg");

        r.writeReport();
    }

}
