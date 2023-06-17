package se.camabomedia.ekorr;

/**
 * nccItem
 */
public class NccItem {
      String name;
      String css_class;
      String text;
      String smil;
      String par_id;

    public NccItem(String name, String css_class, String text, String smil, String par_id) {
        this.name = name;
        this.css_class = css_class;
        this.text = text;
        this.smil = smil;
        this.par_id = par_id;
    }

    @Override
    public String toString() {
        return "NccItem ["
            + "name=" + name + ", "
            + "css_class=" + css_class + ", "
            + "text=" + text + ", "
            + "smil=" + smil + ", "
            + "par_id=" + par_id
            + "]";
    }


}
