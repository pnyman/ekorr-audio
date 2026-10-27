package se.camabomedia.ekorraudio.ui;

import java.io.File;
import java.util.function.Consumer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

/**
 * Visar NCC:n (rubriker och sidor). Ett klick på en post skickar par-id:t
 * från länken (t.ex. "par_fixgen_16" ur "gram0003.smil#par_fixgen_16").
 */
public class NccView {

    /**
     * Klick: gå uppåt till NCC-posten (elementet med id, t.ex. h1 eller
     * span.page-normal), ta länken i den, stoppa navigeringen och skicka
     * delen efter # till Java.
     */
    private static final String CLICK_SCRIPT =
        "document.addEventListener('click', function(event) {"
        + "    event.preventDefault();"
        + "    var el = event.target;"
        + "    while (el && !el.id) { el = el.parentNode; }"
        + "    if (!el || !el.querySelector) { return; }"
        + "    var a = el.querySelector('a');"
        + "    if (!a) { return; }"
        + "    var href = a.getAttribute('href') || '';"
        + "    var hash = href.indexOf('#');"
        + "    if (hash >= 0) { java.handleClick(href.substring(hash + 1)); }"
        + "});";

    /**
     * Visa bara rubriker och sidnummer. Övriga poster i NCC:n (t.ex.
     * producentanteckningar, sidopaneler, noter) döljs – de finns kvar i
     * filen och påverkar inte navigeringen.
     */
    private static final String FILTER_CSS =
        "body > :not(h1):not(h2):not(h3):not(h4):not(h5):not(h6)"
        + ":not(.page-normal):not(.page-front):not(.page-special) {"
        + "    display: none !important; "
        + "}";


    /** Klass på NCC-posten för sidan man befinner sig på. */
    private static final String CURRENT_CLASS = "ekorr-ncc-current";

    /**
     * Markerar posten och scrollar så att den hamnar mitt i vyn (utan
     * animering). Överst och nederst i listan går det inte att centrera –
     * då hamnar den så nära mitten det går.
     */
    private static final String CURRENT_SCRIPT =
        "(function(id) {"
        + "  var old = document.getElementsByClassName('" + CURRENT_CLASS + "');"
        + "  for (var i = old.length - 1; i >= 0; i--) { old[i].classList.remove('" + CURRENT_CLASS + "'); }"
        + "  if (!id) { return; }"
        + "  var links = document.getElementsByTagName('a');"
        + "  for (var j = 0; j < links.length; j++) {"
        + "    var href = links[j].getAttribute('href') || '';"
        + "    if (href.substring(href.indexOf('#') + 1) !== id) { continue; }"
        + "    var item = links[j];"
        + "    while (item && !item.id) { item = item.parentNode; }"
        + "    if (!item || !item.classList) { return; }"
        + "    item.classList.add('" + CURRENT_CLASS + "');"
        // Scrolla först när markeringen har ritats om. Scrollas det direkt
        // flyttar WebView den gamla bilden innan den hunnit rita bort den
        // tidigare markeringen – den blir då kvar på skärmen.
        + "    setTimeout(function() {"
        + "      item.scrollIntoView({ behavior: 'auto', block: 'center' });"
        + "    }, 40);"
        + "    return;"
        + "  }"
        + "})";

    public final WebView webView;
    private final WebEngine webEngine;

    // Måste hållas som fält: WebView har bara en svag referens till bryggan
    private final JavaBridge javaBridge = new JavaBridge();

    private Consumer<String> onItemClicked = parID -> {};

    /** Färgtema och dess style-element i det laddade dokumentet. */
    private Theme theme = Theme.STANDARD;
    private Element styleNode = null;
    /** Par-id för sidan som ska vara markerad, eller null. */
    private String currentParID = null;

    public NccView() {
        webView = new WebView();
        webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);
        webEngine.setUserStyleSheetLocation(
            getClass().getResource("/se/camabomedia/ekorraudio/ncc.css").toString());

        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == Worker.State.SUCCEEDED) {
                applyThemeCss();
                applyCurrent();
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("java", javaBridge);
                webEngine.executeScript(CLICK_SCRIPT);
            }
        });
    }

    public void openNcc(File nccFile) {
        styleNode = null;
        webEngine.load(nccFile.toURI().toString());
    }

    /** Tömmer vyn (när talboken stängs). */
    public void clear() {
        styleNode = null;
        currentParID = null;
        webEngine.loadContent("");
    }

    /**
     * Byter färgtema. Temats CSS har !important och går därför före ncc.css
     * (som är ett användarstilark). Gäller direkt, utan omladdning.
     */
    public void setTheme(Theme newTheme) {
        theme = newTheme == null ? Theme.STANDARD : newTheme;
        if (styleNode != null) {
            styleNode.setTextContent(css());
        }
    }

    /** Filtret, temats färger och markeringen av aktuell sida. */
    private String css() {
        String current = "." + CURRENT_CLASS + ", ." + CURRENT_CLASS + " * {"
            + "    background-color: " + theme.getHighlight() + " !important; "
            + (theme.getHighlightText() != null
               ? "    color: " + theme.getHighlightText() + " !important; " : "")
            + "}";
        return uiFontCss() + FILTER_CSS + theme.getNccCss() + current;
    }

    /**
     * Markerar NCC-posten (sidnumret) med detta par-id, eller ingen om null.
     * Listan scrollas bara om posten inte syns.
     */
    public void setCurrentPage(String parID) {
        currentParID = parID;
        applyCurrent();
    }

    private void applyCurrent() {
        if (webEngine.getDocument() == null) {
            return;
        }
        String id = currentParID == null ? "" : currentParID.replace("\\", "").replace("'", "");
        webEngine.executeScript(CURRENT_SCRIPT + "('" + id + "')");
    }

    /** Lägger temats style-element sist i dokumentet. */
    private void applyThemeCss() {
        Document doc = webEngine.getDocument();
        if (doc == null || doc.getDocumentElement() == null) {
            return;
        }
        styleNode = doc.createElement("style");
        styleNode.appendChild(doc.createTextNode(css()));
        org.w3c.dom.Node head = doc.getDocumentElement().getElementsByTagName("head").item(0);
        // Ett tomt dokument (efter clear) kan sakna head
        (head != null ? head : doc.getDocumentElement()).appendChild(styleNode);
    }

    /**
     * Samma typsnitt och storlek som JavaFX använder i resten av fönstret
     * (t.ex. kommentarstabellen). JavaFX kallar standardtypsnittet "System",
     * vilket WebView inte känner till – därför Segoe UI och sans-serif som reserv.
     */
    private static String uiFontCss() {
        javafx.scene.text.Font f = javafx.scene.text.Font.getDefault();
        String family = f.getFamily();
        String families = (family == null || family.equalsIgnoreCase("System") ? "" : "\"" + family + "\", ")
            + "\"Segoe UI\", sans-serif";
        return "body { font-family: " + families + " !important; "
            + "font-size: " + Math.round(f.getSize()) + "px !important; } ";
    }

    /** Anropas (på JavaFX-tråden) med par-id:t för den klickade posten. */
    public void setOnItemClicked(Consumer<String> handler) {
        onItemClicked = handler == null ? parID -> {} : handler;
    }

    /** JavaScript -> Java. Måste vara public för att WebView ska se den. */
    public class JavaBridge {
        public void handleClick(String parID) {
            onItemClicked.accept(parID);
        }
    }

    public WebView getNccView() {
        return webView;
    }
}
