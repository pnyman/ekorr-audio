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

    /**
     * Key:   id of syncpoint in content file
     * Value: corresponding SmilPar
     */
    private Hashtable<String, SmilPar> contentIDTable = new Hashtable<>();

    /**
     * Key: mp3file
     * Value: list of the starting times of each par-element
     *        that refers to that mp3file.
     */
    public Hashtable<String, ArrayList<Integer>> mp3StartTimesTable = new Hashtable<>();

    /**
     * Key: mp3file
     * Value: a hashtable with
     *        Key: start time of par element
     *        Value: id of the corresponding element in content file
     */
    private Hashtable<String, Hashtable<Integer, String>> mp3ContentIdTable = new Hashtable<>();
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
                populateMp3ContentIdTable(thisSmilPar);
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
        if (!mp3StartTimesTable.containsKey(mp3File)) {
            mp3StartTimesTable.put(mp3File, new ArrayList<Integer>());
        }
        mp3StartTimesTable.get(mp3File).add(smilPar.getSmilAudio().get(0).getClipBegin());
    }

    private void populateMp3ContentIdTable(SmilPar smilPar) {
        String mp3File = smilPar.getSmilAudio().get(0).getSrc();
        if (!mp3ContentIdTable.containsKey(mp3File)) {
            mp3ContentIdTable.put(mp3File, new Hashtable<Integer, String>());
        }
        int startTime = smilPar.getSmilAudio().get(0).getClipBegin();
        String contentId = smilPar.getContentTagID();
        mp3ContentIdTable.get(mp3File).put(startTime, contentId);
    }

    private SmilPar addSmilPar(Element parElement, ArrayList<String> breadCrumbs) {
        String parElementID = parElement.attr("id");
        String systemRequired = parElement.attr("system-required");
        Element textElement = parElement.select("text").first();
        String textElementID = textElement.attr("id");
        String[] src = textElement.attr("src").split("#");
        contentFile = src[0];
        String contentID = src[1];

        if (systemRequired.contains("pagenumber")) {
            thisPage = content.getTagText(contentID);
        }

        SmilPar smilPar = new SmilPar(parElementID,
                                      textElementID,
                                      contentFile,
                                      contentID,
                                      getAudio(parElement),
                                      breadCrumbs,
                                      thisPage);


        parElementIDTable.put(parElementID, smilPar);
        textElementIDTable.put(textElementID, smilPar);
        contentIDTable.put(contentID, smilPar);

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

    /**
     * @param mp3file name of the mp3file
     * @param time    the current time in the mp3file
     * @return        the content @id that corresponds to time
     */
    public String contentIdFromTime(String mp3file, int time) {
        return mp3ContentIdTable.get(mp3file).get(findStartTime(mp3file, time));
    }

    /**
     * @param  contentId id of a syncpoint in the content file
     * @return the time when that clip begins in the corresponding mp3file
     */
    public int startTimeFromContentId(String contentId) {
        return contentIDTable.get(contentId).getSmilAudio().get(0).getClipBegin();
    }

    /**
     * @param  contentId id of a syncpoint in the content file
     * @return the time when that clip ends in the corresponding mp3file
     */
    public int endTimeFromContentId(String contentId) {
        return contentIDTable.get(contentId).getSmilAudio().get(0).getClipEnd();
    }

    /**
     * @param  contentId id of a syncpoint in the content file
     * @return absoulute path of the corresponding mp3file
     */
    public String mp3FileFromContentId(String contentId) {
        String mp3File = contentIDTable.get(contentId).getSmilAudio().get(0).getSrc();
        return new File(ncc.getDirectory(), mp3File).toString();
    }

    private int findStartTime(String mp3File, int needle) {
        ArrayList<Integer> startTimes = mp3StartTimesTable.get(mp3File);
        int index = Collections.binarySearch(startTimes, needle);
        if (index >= 0) {
            return startTimes.get(index);
        }
        return startTimes.get(-index - 2);
    }

    /**
     * @param  contentID the ID of a syncpoint in the content document
     * @return an ArrayList of headings
     */
    public ArrayList<String> makeBreadCrumbs(String contentID) {
        ArrayList<String> breadCrumbs = new ArrayList<>();
        SmilPar par = getParFromContentTagID(contentID);
        for (String bc : par.getBreadCrumbs()) {
            NccItem nccItem = ncc.getNccItem(bc);
            breadCrumbs.add(nccItem.getText());
        }
        return breadCrumbs;
    }

    public Hashtable<String, SmilPar> getContentTagIDTable() {
        return contentIDTable;
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
        return contentIDTable.get(srcID);
    }

    public String getContentFile() {
        return contentFile;
    }

    public Hashtable<String, SmilPar> getParElementIDTable() {
        return parElementIDTable;
    }

    public Hashtable<String, SmilPar> getTextElementIDTable() {
        return textElementIDTable;
    }

    public Hashtable<String, ArrayList<Integer>> getMp3StartTimesTable() {
        return mp3StartTimesTable;
    }

    public Hashtable<String, Hashtable<Integer, String>> getMp3ContentIdTable() {
        return mp3ContentIdTable;
    }


}
