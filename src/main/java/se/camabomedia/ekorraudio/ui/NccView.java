package se.camabomedia.ekorraudio.ui;

import java.io.File;
import java.util.function.Consumer;

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

    public final WebView webView;
    private final WebEngine webEngine;

    // Måste hållas som fält: WebView har bara en svag referens till bryggan
    private final JavaBridge javaBridge = new JavaBridge();

    private Consumer<String> onItemClicked = parID -> {};

    public NccView() {
        webView = new WebView();
        webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);
        webEngine.setUserStyleSheetLocation(
            getClass().getResource("/se/camabomedia/ekorraudio/ncc.css").toString());

        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("java", javaBridge);
                webEngine.executeScript(CLICK_SCRIPT);
            }
        });
    }

    public void openNcc(File nccFile) {
        webEngine.load(nccFile.toURI().toString());
    }

    /** Tömmer vyn (när talboken stängs). */
    public void clear() {
        webEngine.loadContent("");
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
