package se.camabomedia.ekorr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class Smil {

    Hashtable<String, SmilPar> parElementIdTable = new Hashtable<>();
    Hashtable<String, SmilPar> textElementIdTable = new Hashtable<>();

    public Smil(File directory, ArrayList<String> smilFiles) {
        for (String smilFile : smilFiles) {
            addSmil(new File(directory, smilFile));
        }
    }

    private void addSmil(File smilFile) {
        try {
            Document doc = Jsoup.parse(smilFile, "UTF-8");
            for (Element parElement : doc.getElementsByTag("par")) {
                String parElementId = parElement.attr("id");
                Element textElement = parElement.select("text").first();
                String textElementId = textElement.attr("id");
                String[] src = textElement.attr("src").split("#");
                String sourceFile = src[0];
                String sourceFileTagId = src[1];

                SmilPar smilPar = new SmilPar(parElementId,
                                              textElementId,
                                              sourceFile,
                                              sourceFileTagId);

                parElementIdTable.put(parElementId, smilPar);
                textElementIdTable.put(textElementId, smilPar);

            }
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
