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

    public final WebView webView;
    private final WebEngine webEngine;

    // Måste hållas som fält: WebView har bara en svag referens till bryggan
    private final JavaBridge javaBridge = new JavaBridge();

    private Consumer<String> onItemClicked = parID -> {};

    /** Färgtema och dess style-element i det laddade dokumentet. */
    private Theme theme = Theme.STANDARD;
    private Element styleNode = null;

    public NccView() {
        webView = new WebView();
        webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);
        webEngine.setUserStyleSheetLocation(
            getClass().getResource("/se/camabomedia/ekorraudio/ncc.css").toString());

        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == Worker.State.SUCCEEDED) {
                applyThemeCss();
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
        webEngine.loadContent("");
    }

    /**
     * Byter färgtema. Temats CSS har !important och går därför före ncc.css
     * (som är ett användarstilark). Gäller direkt, utan omladdning.
     */
    public void setTheme(Theme newTheme) {
        theme = newTheme == null ? Theme.STANDARD : newTheme;
        if (styleNode != null) {
            styleNode.setTextContent(FILTER_CSS + theme.getNccCss());
        }
    }

    /** Lägger temats style-element sist i dokumentet. */
    private void applyThemeCss() {
        Document doc = webEngine.getDocument();
        if (doc == null || doc.getDocumentElement() == null) {
            return;
        }
        styleNode = doc.createElement("style");
        styleNode.appendChild(doc.createTextNode(FILTER_CSS + theme.getNccCss()));
        org.w3c.dom.Node head = doc.getDocumentElement().getElementsByTagName("head").item(0);
        // Ett tomt dokument (efter clear) kan sakna head
        (head != null ? head : doc.getDocumentElement()).appendChild(styleNode);
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
