package se.camabomedia.ekorr;

/**
 * nccItem
 */
public class NccItem {
      String name;
      String cssClass;
      String text;
      String smil;
      String parId;

    public NccItem(String name, String cssClass, String text, String smil, String parId) {
        this.name = name;
        this.cssClass = cssClass;
        this.text = text;
        this.smil = smil;
        this.parId = parId;
    }

    @Override
    public String toString() {
        return "NccItem ["
            + "name=" + name + ", "
            + "cssClass=" + cssClass + ", "
            + "text=" + text + ", "
            + "smil=" + smil + ", "
            + "parId=" + parId
            + "]";
    }


}
