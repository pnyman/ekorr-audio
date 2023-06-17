package se.camabomedia.ekorr;

/**
 * nccItem
 */
public class NccItem {
      String name;
      String css_class;
      String text;
      String smil;
      String fragment;

    public NccItem(String name, String css_class, String text, String smil, String fragment) {
        this.name = name;
        this.css_class = css_class;
        this.text = text;
        this.smil = smil;
        this.fragment = fragment;
    }

    @Override
    public String toString() {
        return "NccItem ["
            + "name=" + name + ", "
            + "css_class=" + css_class + ", "
            + "text=" + text + ", "
            + "smil=" + smil + ", "
            + "fragment=" + fragment
            + "]";
    }


}
