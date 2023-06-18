package se.camabomedia.ekorr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class Smil {

    private Ncc ncc;
    private Hashtable<String, SmilPar> parElementIDTable = new Hashtable<>();
    private Hashtable<String, SmilPar> textElementIDTable = new Hashtable<>();
    private Hashtable<String, SmilPar> sourceFileTagIDTable = new Hashtable<>();

    public Smil(Ncc ncc) {
        this.ncc = ncc;
        File directory = ncc.getDirectory();
        Hashtable<String, ArrayList<String>> breadCrumbsTable = ncc.getBreadCrumbsTable();
        for (String smilFile : ncc.getSmilFiles()) {
            addSmil(new File(directory, smilFile), breadCrumbsTable.get(smilFile));
        }
    }

    private void addSmil(File smilFile, ArrayList<String> breadCrumbs) {
        try {
            Document doc = Jsoup.parse(smilFile, "UTF-8");
            for (Element parElement : doc.getElementsByTag("par")) {
                addSmilPar(parElement, breadCrumbs);
            }
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void addSmilPar(Element parElement, ArrayList<String> breadCrumbs) {
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
                                      getAudio(parElement),
                                      breadCrumbs);

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

    public ArrayList<String> getBreadCrumbs(String sourceFileTagID) {
        ArrayList<String> breadCrumbs = new ArrayList<>();
        SmilPar par = getParFromSourceFileTagID(sourceFileTagID);
        for (String bc : par.getBreadCrumbs()) {
            NccItem nccItem = ncc.getNccItem(bc);
            breadCrumbs.add(nccItem.getText());
        }
        return breadCrumbs;
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
