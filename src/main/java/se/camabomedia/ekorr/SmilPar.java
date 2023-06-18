package se.camabomedia.ekorr;

import java.util.ArrayList;

/**
 * SmilPar
 */
public class SmilPar {

    String parElementId;
    String textElementId;
    String sourceFile;
    String sourceFileTagId;
    ArrayList<SmilAudio> smilAudio;

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
