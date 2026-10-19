package se.camabomedia.ekorraudio.ui;

import java.io.File;
import java.util.function.Consumer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

/**
 * Visar innehållsfilen (fulltexten) och markerar den synkpunkt som spelas.
 */
public class ContentView {

    /** Klass som sätts på den synkpunkt som spelas. */
    private static final String CURRENT_CLASS = "ekorr-current";

    /*
     * Markeringen görs med en klass och !important, och gäller även
     * barnelementen (em, strong …). Annars kan bokens egen CSS – t.ex. en
     * bakgrund på blockquote, em eller p – dölja den gula färgen.
     */
    private static final String CSS =
        "body {"
        + "    margin-left: 3%; "
        + "    margin-right: 3%; "
        + "}"
        + "." + CURRENT_CLASS + ", ." + CURRENT_CLASS + " * {"
        + "    background-color: yellow !important; "
        + "}";

    /**
     * Klick: leta upp närmaste element med id (uppåt i trädet) och skicka
     * det till Java. preventDefault så att länkar inte navigerar bort.
     * Om man har markerat text (t.ex. för att kopiera till en kommentar)
     * räknas det inte som ett klick.
     */
    private static final String CLICK_SCRIPT =
        "document.addEventListener('click', function(event) {"
        + "    if (String(window.getSelection()).length > 0) { return; }"
        + "    var el = event.target;"
        + "    while (el && !(el.id)) { el = el.parentNode; }"
        + "    event.preventDefault();"
        + "    if (el && el.id) { java.handleClick(el.id); }"
        + "});";

    public final WebView webView;
    private final WebEngine webEngine;

    // Måste hållas som fält: WebView har bara en svag referens till bryggan
    private final JavaBridge javaBridge = new JavaBridge();

    private JSObject currentElement = null;
    private String currentElementID = null;
    private String firstElementID;

    /** id att markera när sidan laddats klart (om highlight anropas för tidigt). */
    private String pendingElementID = null;

    private Consumer<String> onSyncPointClicked = id -> {};

    public ContentView() {
        webView = new WebView();
        webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);

        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != Worker.State.SUCCEEDED) {
                return;
            }
            // Körs för varje ny laddning – det nya dokumentet saknar allt från det förra
            currentElement = null;
            applyCSS();
            JSObject window = (JSObject) webEngine.executeScript("window");
            window.setMember("java", javaBridge);
            webEngine.executeScript(CLICK_SCRIPT);

            if (pendingElementID != null) {
                String id = pendingElementID;
                pendingElementID = null;
                highlight(id);
            }
        });
    }

    public void openContent(File contentFile) {
        currentElement = null;
        currentElementID = null;
        pendingElementID = null;
        webEngine.load(contentFile.toURI().toString());
    }

    /** Anropas (på JavaFX-tråden) med id:t för det element man klickade på. */
    public void setOnSyncPointClicked(Consumer<String> handler) {
        onSyncPointClicked = handler == null ? id -> {} : handler;
    }

    private void applyCSS() {
        Document doc = webEngine.getDocument();
        Element styleNode = doc.createElement("style");
        Text styleContent = doc.createTextNode(CSS);
        styleNode.appendChild(styleContent);
        doc.getDocumentElement().getElementsByTagName("head").item(0).appendChild(styleNode);
    }

    /** Markerar elementet och scrollar dit. */
    public void highlight(String elementID) {
        if (webEngine.getLoadWorker().getState() != Worker.State.SUCCEEDED) {
            pendingElementID = elementID;   // ersätter ev. tidigare – bara det senaste gäller
            return;
        }
        Object element = webEngine.getDocument() == null ? null
            : ((JSObject) webEngine.executeScript("document")).call("getElementById", elementID);
        if (!(element instanceof JSObject)) {
            // Låt inte förra meningen stå kvar som markerad – det ser ut som fel ställe
            System.err.println("Hittar inte synkpunkten i innehållsfilen: " + elementID);
            clearCurrentElement();
            currentElementID = elementID;
            return;
        }
        setCurrentElement((JSObject) element, elementID);
        ((JSObject) element).call("scrollIntoView",
            webEngine.executeScript("({ behavior: 'smooth', block: 'center' })"));
    }

    /** Behålls för befintlig kod: samma som highlight. */
    public void scrollToElement(String elementID) {
        highlight(elementID);
    }

    private void setCurrentElement(JSObject element, String elementID) {
        clearCurrentElement();
        ((JSObject) element.getMember("classList")).call("add", CURRENT_CLASS);
        currentElement = element;
        currentElementID = elementID;
    }

    private void clearCurrentElement() {
        if (currentElement != null) {
            ((JSObject) currentElement.getMember("classList")).call("remove", CURRENT_CLASS);
            currentElement = null;
        }
    }

    /** JavaScript -> Java. Måste vara public för att WebView ska se den. */
    public class JavaBridge {
        public void handleClick(String elementID) {
            onSyncPointClicked.accept(elementID);
        }
    }

    public WebView getContentView() {
        return webView;
    }

    public String getCurrentElementID() {
        return currentElementID;
    }

    public String getFirstElementID() {
        return firstElementID;
    }

    public void setFirstElementID(String firstElementID) {
        this.firstElementID = firstElementID;
    }
}
