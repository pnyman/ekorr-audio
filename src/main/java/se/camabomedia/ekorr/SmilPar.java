package se.camabomedia.ekorr;

import java.util.ArrayList;

/**
 * SmilPar
 */
public class SmilPar {

    private String parElementId;
    private String textElementId;
    private String sourceFile;
    private String sourceFileTagId;
    private ArrayList<SmilAudio> smilAudio;

    public SmilPar(String parElementId,
                   String textElementId,
                   String sourceFile,
                   String sourceFileTagId,
                   ArrayList<SmilAudio> smilAudio) {
        this.parElementId = parElementId;
        this.textElementId = textElementId;
        this.sourceFile = sourceFile;
        this.sourceFileTagId = sourceFileTagId;
        this.smilAudio = smilAudio;
    }

    public String getParElementId() {
        return parElementId;
    }

    public String getTextElementId() {
        return textElementId;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public String getSourceFileTagId() {
        return sourceFileTagId;
    }

    public ArrayList<SmilAudio> getSmilAudio() {
        return smilAudio;
    }

    @Override
    public String toString() {
        return "SmilPar [parElementId=" + parElementId
            + ", textElementId=" + textElementId
            + ", sourceFile=" + sourceFile
            + ", sourceFileTagId=" + sourceFileTagId
            + ", smilAudio=" + smilAudio
            + "]";
    }

}
