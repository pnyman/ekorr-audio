package se.camabomedia.ekorr;

import java.util.ArrayList;

/**
 * SmilPar
 */
public class SmilPar {

    private String parElementID;
    private String textElementID;
    private String sourceFile;
    private String sourceFileTagID;
    private ArrayList<SmilAudio> smilAudio;
    private ArrayList<String> breadCrumbs;

    public SmilPar(String parElementID,
                   String textElementID,
                   String sourceFile,
                   String sourceFileTagID,
                   ArrayList<SmilAudio> smilAudio,
                   ArrayList<String> breadCrumbs) {
        this.parElementID = parElementID;
        this.textElementID = textElementID;
        this.sourceFile = sourceFile;
        this.sourceFileTagID = sourceFileTagID;
        this.smilAudio = smilAudio;
        this.breadCrumbs = breadCrumbs;
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

    @Override
    public String toString() {
        return "SmilPar [parElementID=" + parElementID
            + ", textElementID=" + textElementID
            + ", sourceFile=" + sourceFile
            + ", sourceFileTagID=" + sourceFileTagID
            + ", smilAudio=" + smilAudio
            + "]";
    }

}
