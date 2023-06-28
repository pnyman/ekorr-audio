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
    String parID;
    ArrayList<String> breadCrumb;
    int level;
    String page;

    public NccItem(String name,
                   String cssClass,
                   String text,
                   String smil,
                   String parID,
                   ArrayList<String> breadCrumb,
                   int level,
                   String page) {
        this.name = name;
        this.cssClass = cssClass;
        this.text = text;
        this.smil = smil;
        this.parID = parID;
        this.breadCrumb = breadCrumb;
        this.level = level;
        this.page = page;
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

    public String getParID() {
        return parID;
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
            + "parID=" + parID + ", "
            + "breadCrumb=" + breadCrumb
            + "level=" + level
            + "page=" + page
            + "]";
    }


}
