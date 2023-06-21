package se.camabomedia.ekorr;

import java.util.ArrayList;

/**
 * Represents a par-element in a smil-file.
 *
 * @author Patrik Nyman
 */
public class SmilPar {

    private String parElementID;
    private String textElementID;
    private String sourceFile;
    private String sourceFileTagID;
    private ArrayList<SmilAudio> smilAudio;
    private ArrayList<String> breadCrumbs;
    private SmilPar previousSmilPar;
    private SmilPar nextSmilPar;
    private String page;

    /**
     * Class constructor.
     *
     * @param parElementID     the @id of the par-element
     * @param textElementID    the @id of the text-element
     * @param sourceFileTagID  the @id of the syncpoint in the fulltext file
     * @param smilAudio        list of the audio events in the par
     * @param breadCrumbs      the hierarcy of document headings this per belongs to
     */
    public SmilPar(String parElementID,
                   String textElementID,
                   String sourceFile,
                   String sourceFileTagID,
                   ArrayList<SmilAudio> smilAudio,
                   ArrayList<String> breadCrumbs,
                   String page) {
        this.parElementID = parElementID;
        this.textElementID = textElementID;
        this.sourceFile = sourceFile;
        this.sourceFileTagID = sourceFileTagID;
        this.smilAudio = smilAudio;
        this.breadCrumbs = breadCrumbs;
        this.page = page;
    }

    public String getParElementID() {
        return parElementID;
    }

    public String getTextElementID() {
        return textElementID;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public String getSourceFileTagID() {
        return sourceFileTagID;
    }

    public ArrayList<SmilAudio> getSmilAudio() {
        return smilAudio;
    }

    public ArrayList<String> getBreadCrumbs() {
        return breadCrumbs;
    }

    public String getPage() {
        return page;
    }

    public SmilPar getPreviousSmilPar() {
        return previousSmilPar;
    }

    public void setPreviousSmilPar(SmilPar smilPar) {
        previousSmilPar = smilPar;
    }

    public SmilPar getNextSmilPar() {
        return nextSmilPar;
    }

    public void setNextSmilPar(SmilPar nextSmilPar) {
        this.nextSmilPar = nextSmilPar;
    }

    @Override
    public String toString() {
        return "SmilPar [parElementID=" + parElementID
            + ", textElementID=" + textElementID
            + ", sourceFile=" + sourceFile
            + ", sourceFileTagID=" + sourceFileTagID
            + ", smilAudio=" + smilAudio
            + ", page=" + page
            + "]";
    }

}
