package se.camabomedia.ekorraudio.ui;

/**
 * Färgteman. Ett tema består av
 *  - ett JavaFX-stilark (menyer, verktygsrad, tabell …) som skriver över
 *    färgvariablerna i main.css – null för standard,
 *  - CSS för bokens text och för NCC-listan (läggs in i WebView med
 *    !important, så att den går före bokens egen CSS) – tom för standard,
 *  - markeringens färger.
 *
 * Solarized: https://ethanschoonover.com/solarized/
 */
public enum Theme {

    STANDARD("Standard", null, "", "", "yellow", null,
        false, null, null),

    SOLARIZED_LIGHT("Solarized ljus",
        "/se/camabomedia/ekorraudio/themes/solarized-light.css",
        textCss("#fdf6e3", "#657b83", "#586e75", "#93a1a1", "#268bd2", "#eee8d5"),
        nccCss("#fdf6e3", "#657b83", "#268bd2", "#eee8d5", "#586e75"),
        "#f3e2a9", null,
        false, "#eee8d5", "#586e75"),

    SOLARIZED_DARK("Solarized mörk",
        "/se/camabomedia/ekorraudio/themes/solarized-dark.css",
        textCss("#002b36", "#93a1a1", "#eee8d5", "#586e75", "#268bd2", "#073642"),
        nccCss("#002b36", "#93a1a1", "#268bd2", "#073642", "#eee8d5"),
               "#124f5c", "#fdf6e3",
        true, "#073642", "#93a1a1");

    // "#5c4d06", "#fdf6e3",
    // "#0b4452", "#eee8d5",
    // "#124f5c", "#fdf6e3",
    // #b58900 // guldgul, för text i markering


    private final String label;
    private final String stylesheet;
    private final String contentCss;
    private final String nccCss;
    private final String highlight;
    private final String highlightText;
    private final boolean darkTitleBar;
    private final String titleBarColor;
    private final String titleBarText;

    Theme(String label, String stylesheet, String contentCss, String nccCss,
          String highlight, String highlightText,
          boolean darkTitleBar, String titleBarColor, String titleBarText) {
        this.label = label;
        this.stylesheet = stylesheet;
        this.contentCss = contentCss;
        this.nccCss = nccCss;
        this.highlight = highlight;
        this.highlightText = highlightText;
        this.darkTitleBar = darkTitleBar;
        this.titleBarColor = titleBarColor;
        this.titleBarText = titleBarText;
    }

    @Override
    public String toString() {
        return label;
    }

    /** JavaFX-stilark (resurssökväg), eller null för standard. */
    public String getStylesheet() {
        return stylesheet;
    }

    /** CSS för bokens text (utan markeringen). */
    public String getContentCss() {
        return contentCss;
    }

    /** CSS för NCC-listan. */
    public String getNccCss() {
        return nccCss;
    }

    /** Bakgrundsfärg för den mening som spelas. */
    public String getHighlight() {
        return highlight;
    }

    /** Textfärg i markeringen, eller null för att behålla textens färg. */
    public String getHighlightText() {
        return highlightText;
    }

    /** Mörk namnlist (Windows 10 och 11). */
    public boolean isDarkTitleBar() {
        return darkTitleBar;
    }

    /** Namnlistens färg (#rrggbb, bara Windows 11), eller null för systemets. */
    public String getTitleBarColor() {
        return titleBarColor;
    }

    /** Namnlistens textfärg (#rrggbb, bara Windows 11), eller null för systemets. */
    public String getTitleBarText() {
        return titleBarText;
    }

    /** Tema från sparat namn, standard om namnet saknas eller är okänt. */
    public static Theme fromName(String name) {
        if (name != null) {
            for (Theme t : values()) {
                if (t.name().equals(name)) {
                    return t;
                }
            }
        }
        return STANDARD;
    }

    /**
     * Bakgrund och textfärg för hela texten. "body *" gör alla element
     * genomskinliga med ärvd färg, så att bokens egna färger inte bryter
     * temat; rubriker, sidnummer och länkar får sedan egna färger.
     */
    private static String textCss(String background, String text, String heading,
                                  String pageNumber, String link, String rule) {
        return "html, body { background-color: " + background + " !important; color: " + text + " !important; } "
            + "body * { color: inherit !important; background-color: transparent !important; "
            + "border-color: " + rule + " !important; } "
            + "h1, h2, h3, h4, h5, h6 { color: " + heading + " !important; } "
            + ".page-normal, .page-front, .page-special, .pagenum { color: " + pageNumber + " !important; } "
            + "a, a:link, a:visited { color: " + link + " !important; } ";
    }

    /** Färger för NCC-listan (ncc.css har svarta länkar och blå sidnummer). */
    private static String nccCss(String background, String text, String pageNumber,
                                 String hoverBackground, String hoverText) {
        return "html, body { background-color: " + background + " !important; color: " + text + " !important; } "
            + "a, a:link, a:visited, a:active { color: " + text + " !important; } "
            + "span, span a { color: " + pageNumber + " !important; } "
            + "a:hover { color: " + hoverText + " !important; background-color: " + hoverBackground + " !important; } ";
    }
}
