package se.camabomedia.ekorr;

/**
 * SmilPar
 */
public class SmilPar {

    public String parElementId;
    String textElementId;
    String sourceFile;
    String sourceFileTagId;

    public SmilPar(String parElementId, String textElementId, String sourceFile, String sourceFileTagId) {
        this.parElementId = parElementId;
        this.textElementId = textElementId;
        this.sourceFile = sourceFile;
        this.sourceFileTagId = sourceFileTagId;
    }

    @Override
    public String toString() {
        return "SmilPar [parElementId=" + parElementId
            + ", textElementId=" + textElementId
            + ", sourceFile=" + sourceFile
            + ", sourceFileTagId=" + sourceFileTagId
            + "]";
    }

}
