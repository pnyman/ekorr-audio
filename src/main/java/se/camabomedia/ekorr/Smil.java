package se.camabomedia.ekorr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class Smil {

    private Hashtable<String, SmilPar> parElementIdTable = new Hashtable<>();
    private Hashtable<String, SmilPar> textElementIdTable = new Hashtable<>();
    private Hashtable<String, SmilPar> sourceFileTagIdTable = new Hashtable<>();

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
        String parElementId = parElement.attr("id");
        Element textElement = parElement.select("text").first();
        String textElementId = textElement.attr("id");
        String[] src = textElement.attr("src").split("#");
        String sourceFile = src[0];
        String sourceFileTagId = src[1];

        SmilPar smilPar = new SmilPar(parElementId,
                                      textElementId,
                                      sourceFile,
                                      sourceFileTagId,
                                      getAudio(parElement));

        parElementIdTable.put(parElementId, smilPar);
        textElementIdTable.put(textElementId, smilPar);
        sourceFileTagIdTable.put(sourceFileTagId, smilPar);
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

    public SmilPar getParFromParId(String parId) {
        return parElementIdTable.get(parId);
    }

    public SmilPar getParFromTextId(String textId) {
        return textElementIdTable.get(textId);
    }

    public SmilPar getParFromSourceFileTagId(String srcId) {
        return sourceFileTagIdTable.get(srcId);
    }

}
