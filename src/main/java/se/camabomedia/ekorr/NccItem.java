package se.camabomedia.ekorr;

import java.util.ArrayList;

/**
 * nccItem
 */
public class NccItem {
    String name;
    String cssClass;
    String text;
    String smil;
    String parId;
    ArrayList<String> breadCrumb;
    int level;

    public NccItem(String name,
                   String cssClass,
                   String text,
                   String smil,
                   String parId,
                   ArrayList<String> breadCrumb,
                   int level) {
        this.name = name;
        this.cssClass = cssClass;
        this.text = text;
        this.smil = smil;
        this.parId = parId;
        this.breadCrumb = breadCrumb;
        this.level = level;
    }

    public String getName() {
        return name;
    }

    public String getCssClass() {
        return cssClass;
    }

    public String getText() {
        return text;
    }

    public String getSmil() {
        return smil;
    }

    public String getParId() {
        return parId;
    }

    public ArrayList<String> getBreadCrumb() {
        return breadCrumb;
    }

    @Override
    public String toString() {
        return "NccItem ["
            + "name=" + name + ", "
            + "cssClass=" + cssClass + ", "
            + "text=" + text + ", "
            + "smil=" + smil + ", "
            + "parId=" + parId + ", "
            + "breadCrumb=" + breadCrumb
            + "]";
    }


}
