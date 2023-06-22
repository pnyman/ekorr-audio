package se.camabomedia.ekorr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class Smil {

    private Ncc ncc;
    // TODO denna variabel onödig
    private String contentFile;
    private Content content;
    File directory;
    private Hashtable<String, SmilPar> parElementIDTable = new Hashtable<>();
    private Hashtable<String, SmilPar> textElementIDTable = new Hashtable<>();
    private Hashtable<String, SmilPar> contentTagIDTable = new Hashtable<>();
    private Hashtable<String, ArrayList<Integer>> mp3StartTimesTable = new Hashtable<>();

    SmilPar previousSmilPar = null;
    String thisPage = null;

    public Smil(Ncc ncc, Content content) {
        this.ncc = ncc;
        this.content = content;
        directory = ncc.getDirectory();

        for (String smilFile : ncc.getSmilFiles()) {
            addSmil(smilFile, ncc.getBreadCrumbs(smilFile));
        }

    }

    private void addSmil(String smilFile, ArrayList<String> breadCrumbs) {
        try {
            Document doc = Jsoup.parse(new File(directory, smilFile), "UTF-8");
            SmilPar thisSmilPar;

            for (Element parElement : doc.getElementsByTag("par")) {
                thisSmilPar = addSmilPar(parElement, breadCrumbs);

                if (previousSmilPar != null) {
                    thisSmilPar.setPreviousSmilPar(previousSmilPar);
                    previousSmilPar.setNextSmilPar(thisSmilPar);
                }

                populateMp3StartTimesTable(thisSmilPar);
                previousSmilPar = thisSmilPar;
            }
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void populateMp3StartTimesTable(SmilPar smilPar) {
        String mp3File = smilPar.getSmilAudio().get(0).getSrc();
        if (!mp3StartTimesTable.contains(mp3File)) {
            mp3StartTimesTable.put(mp3File, new ArrayList<Integer>());
        }
        mp3StartTimesTable.get(mp3File).add(smilPar.getSmilAudio().get(0).getClipBegin());
    }

    private SmilPar addSmilPar(Element parElement, ArrayList<String> breadCrumbs) {
        String parElementID = parElement.attr("id");
        String systemRequired = parElement.attr("system-required");
        Element textElement = parElement.select("text").first();
        String textElementID = textElement.attr("id");
        String[] src = textElement.attr("src").split("#");
        contentFile = src[0];
        String contentTagID = src[1];

        if (systemRequired.contains("pagenumber")) {
            thisPage = content.getTagText(contentTagID);
        }

        SmilPar smilPar = new SmilPar(parElementID,
                                      textElementID,
                                      contentFile,
                                      contentTagID,
                                      getAudio(parElement),
                                      breadCrumbs,
                                      thisPage);

        parElementIDTable.put(parElementID, smilPar);
        textElementIDTable.put(textElementID, smilPar);
        contentTagIDTable.put(contentTagID, smilPar);

        return smilPar;
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

    public int findStartTime(String mp3File, int needle) {
        ArrayList<Integer> startTimes = mp3StartTimesTable.get(mp3File);
        int index = Collections.binarySearch(startTimes, needle);
        if (index >= 0) {
            return startTimes.get(index);
        }
        return (-index - 1) - 1;
    }

    public ArrayList<String> getBreadCrumbs(String contentTagID) {
        ArrayList<String> breadCrumbs = new ArrayList<>();
        SmilPar par = getParFromContentTagID(contentTagID);
        for (String bc : par.getBreadCrumbs()) {
            NccItem nccItem = ncc.getNccItem(bc);
            breadCrumbs.add(nccItem.getText());
        }
        return breadCrumbs;
    }

    public Hashtable<String, SmilPar> getContentTagIDTable() {
        return contentTagIDTable;
    }

    public Hashtable<String, ArrayList<Integer>> getSmilStartTimesTable() {
        return mp3StartTimesTable;
    }

    public SmilPar getParFromParID(String parID) {
        return parElementIDTable.get(parID);
    }

    public SmilPar getParFromTextID(String textID) {
        return textElementIDTable.get(textID);
    }

    public SmilPar getParFromContentTagID(String srcID) {
        return contentTagIDTable.get(srcID);
    }

    public String getContentFile() {
        return contentFile;
    }

}
