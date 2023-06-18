package se.camabomedia.ekorr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class Smil {

    private Hashtable<String, SmilPar> parElementIDTable = new Hashtable<>();
    private Hashtable<String, SmilPar> textElementIDTable = new Hashtable<>();
    private Hashtable<String, SmilPar> sourceFileTagIDTable = new Hashtable<>();

    public Smil(File directory, ArrayList<String> smilFiles) {
        for (String smilFile : smilFiles) {
            addSmil(new File(directory, smilFile));
        }
    }

    private void addSmil(File smilFile) {
        try {
            Document doc = Jsoup.parse(smilFile, "UTF-8");
            for (Element parElement : doc.getElementsByTag("par")) {
                addSmilPar(parElement);
            }
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void addSmilPar(Element parElement) {
        String parElementID = parElement.attr("id");
        Element textElement = parElement.select("text").first();
        String textElementID = textElement.attr("id");
        String[] src = textElement.attr("src").split("#");
        String sourceFile = src[0];
        String sourceFileTagID = src[1];

        SmilPar smilPar = new SmilPar(parElementID,
                                      textElementID,
                                      sourceFile,
                                      sourceFileTagID,
                                      getAudio(parElement));

        parElementIDTable.put(parElementID, smilPar);
        textElementIDTable.put(textElementID, smilPar);
        sourceFileTagIDTable.put(sourceFileTagID, smilPar);
    }

    private ArrayList<SmilAudio> getAudio(Element parElement) {
        ArrayList<SmilAudio> smilAudio = new ArrayList<>();
        Element seq = parElement.select("seq").first();
        if (seq != null) {
            for (Element audio : seq.getElementsByTag("audio")) {
                smilAudio.add(new SmilAudio(audio));
            }
        }
        return smilAudio;
    }

    public SmilPar getParFromParID(String parID) {
        return parElementIDTable.get(parID);
    }

    public SmilPar getParFromTextID(String textID) {
        return textElementIDTable.get(textID);
    }

    public SmilPar getParFromSourceFileTagID(String srcID) {
        return sourceFileTagIDTable.get(srcID);
    }

}
