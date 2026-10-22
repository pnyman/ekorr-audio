package se.camabomedia.ekorraudio.ui;

import java.io.File;
import java.util.function.Consumer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Orientation;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

/**
 * Visar innehållsfilen (fulltexten) och markerar den synkpunkt som spelas.
 */
public class ContentView {

    /** Klass som sätts på den synkpunkt som spelas. */
    private static final String CURRENT_CLASS = "ekorr-current";

    /** Minsta höjd på rullningslistens reglage, som andel av listen. */
    private static final double MIN_THUMB_FRACTION = 0.06;

    /*
     * Programmets grund-CSS för texten. Därefter läggs temats färger, ev.
     * typsnitt och sist markeringen (buildCss). Markeringen görs med en klass
     * och !important och gäller även barnelementen (em, strong …), annars kan
     * bokens egen CSS – t.ex. en bakgrund på blockquote, em eller p – dölja den.
     */
    private static final String CSS =
        // WebView:s egen rullningslist döljs (den tonades bort vid inaktivitet).
        // I stället används en JavaFX-ScrollBar bredvid, se scrollBar nedan.
        // Programmatisk scrollning (scrollTo) fungerar ändå.
        "html {"
        + "    overflow: hidden !important; "
        + "}"
        + "body {"
        + "    margin-left: 3%; "
        + "    margin-right: 3%; "
        + "}"
        // Större radavstånd för läsbarhet. !important och body * eftersom
        // bokens CSS kan sätta egen radhöjd på p, li m.m.
        + "body, body * {"
        + "    line-height: 1.6 !important; "
        + "}";

    /**
     * Markeringen. Färgen kommer från temat (se highlightCss). Läggs sist,
     * efter temats CSS, så att den alltid syns.
     *
     * Bakgrunden på ett inline-element täcker bara texthöjden, inte hela
     * radhöjden. En skugga 0.3em uppåt och nedåt fyller glappen mellan
     * raderna. OBS: inte padding – på element som är block (t.ex. sidnummer
     * med display:block) ändrar padding layouten, texten nedanför flyttas och
     * WebView lämnar kvar gamla bilder (dubbla rader, kvarglömd markering).
     */
    private static String highlightCss(Theme theme) {
        String color = theme.getHighlight();
        String css = "." + CURRENT_CLASS + ", ." + CURRENT_CLASS + " * {"
            + "    background-color: " + color + " !important; "
            + (theme.getHighlightText() != null
               ? "    color: " + theme.getHighlightText() + " !important; " : "")
            + "}"
            + "." + CURRENT_CLASS + " {"
            + "    box-shadow: 0 0.3em 0 " + color + ", 0 -0.3em 0 " + color + " !important; "
            + "}";
        return css;
    }

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

    /**
     * Rapporterar scrollposition, dokumenthöjd och fönsterhöjd till Java
     * när sidan scrollas, ändrar storlek eller (bilder) laddats klart.
     */
    private static final String SCROLL_SCRIPT =
        "(function() {"
        + "  function report() {"
        + "    java.onScroll(window.pageYOffset,"
        + "                  document.documentElement.scrollHeight,"
        + "                  window.innerHeight);"
        + "  }"
        + "  window.addEventListener('scroll', report);"
        + "  window.addEventListener('resize', report);"
        + "  window.addEventListener('load', report);"
        + "  window.ekorrReportScroll = report;"
        + "  report();"
        + "})();";

    public final WebView webView;
    private final WebEngine webEngine;

    /** Egen rullningslist (alltid synlig) och behållaren med WebView + list. */
    private final ScrollBar scrollBar = new ScrollBar();
    private final BorderPane container = new BorderPane();
    /** Sant medan listen uppdateras från sidan – då ska den inte scrolla sidan. */
    private boolean updatingScrollBar = false;

    // Måste hållas som fält: WebView har bara en svag referens till bryggan
    private final JavaBridge javaBridge = new JavaBridge();

    private JSObject currentElement = null;
    private String currentElementID = null;
    private String firstElementID;

    /** id att markera när sidan laddats klart (om highlight anropas för tidigt). */
    private String pendingElementID = null;
    private boolean loading = false;

    /** Programmets style-element i det laddade dokumentet. */
    private Element styleNode = null;
    /** Valt typsnitt, eller null för bokens eget. */
    private String fontFamily = null;
    /** Färgtema för texten och markeringen. */
    private Theme theme = Theme.STANDARD;

    private Consumer<String> onSyncPointClicked = id -> {};

    public ContentView() {
        webView = new WebView();
        webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);

        setUpScrollBar();

        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == Worker.State.FAILED || newValue == Worker.State.CANCELLED) {
                loading = false;
                return;
            }
            if (newValue != Worker.State.SUCCEEDED) {
                return;
            }
            // Körs för varje ny laddning – det nya dokumentet saknar allt från det förra
            loading = false;
            currentElement = null;
            applyCSS();
            JSObject window = (JSObject) webEngine.executeScript("window");
            window.setMember("java", javaBridge);
            webEngine.executeScript(CLICK_SCRIPT);
            webEngine.executeScript(SCROLL_SCRIPT);

            if (pendingElementID != null) {
                String id = pendingElementID;
                pendingElementID = null;
                highlight(id);
            }
        });
    }

    /**
     * Rullningslisten till höger om WebView. Hålls synkad åt båda håll:
     * sidan rapporterar via JavaBridge.onScroll, och när användaren drar
     * i listen anropas window.scrollTo. Mushjulet skickas vidare till sidan,
     * eftersom sidans egen scrollning är avstängd (overflow: hidden).
     */
    private void setUpScrollBar() {
        scrollBar.setOrientation(Orientation.VERTICAL);
        scrollBar.setMin(0);
        scrollBar.setMax(0);
        scrollBar.setUnitIncrement(40);
        scrollBar.setFocusTraversable(false);

        scrollBar.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (!updatingScrollBar && !loading && webEngine.getDocument() != null) {
                webEngine.executeScript("window.scrollTo(0, " + newValue.doubleValue() + ")");
            }
        });

        webView.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (e.isControlDown() || webEngine.getDocument() == null) {
                return;
            }
            webEngine.executeScript("window.scrollBy(0, " + (-e.getDeltaY()) + ")");
            e.consume();
        });

        // Tangenterna för sidvis scrollning fungerar inte längre av sig själva
        // (overflow: hidden) – gör dem här. Pilar och mellanslag hanteras av
        // huvudfönstrets uppspelningstangenter.
        webView.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (webEngine.getDocument() == null || e.isControlDown() || e.isAltDown()) {
                return;
            }
            String script;
            switch (e.getCode()) {
            case PAGE_UP:
                script = "window.scrollBy(0, -window.innerHeight * 0.9)";
                break;
            case PAGE_DOWN:
                script = "window.scrollBy(0, window.innerHeight * 0.9)";
                break;
            case HOME:
                script = "window.scrollTo(0, 0)";
                break;
            case END:
                script = "window.scrollTo(0, document.documentElement.scrollHeight)";
                break;
            default:
                return;
            }
            webEngine.executeScript(script);
            e.consume();
        });

        container.setCenter(webView);
        container.setRight(scrollBar);
        // Behållaren fyller mittkolumnen (AnchorPane) utan ändring i kontrollern
        AnchorPane.setTopAnchor(container, 0.0);
        AnchorPane.setRightAnchor(container, 0.0);
        AnchorPane.setBottomAnchor(container, 0.0);
        AnchorPane.setLeftAnchor(container, 0.0);
    }

    /** Anropas från sidan: uppdatera listens läge och storlek. */
    private void updateScrollBar(double y, double contentHeight, double viewHeight) {
        double max = Math.max(0, contentHeight - viewHeight);
        updatingScrollBar = true;
        try {
            scrollBar.setMax(max);
            // Reglagets andel av listen = synlig del av dokumentet
            // scrollBar.setVisibleAmount(contentHeight > 0 ? max * viewHeight / contentHeight : 0);

            // Reglaget ska vara minst 6 % av listens höjd, annars blir det pyttelitet i en hel bok
            double fraction = contentHeight > 0 ? Math.max(viewHeight / contentHeight, MIN_THUMB_FRACTION) : 0;
            scrollBar.setVisibleAmount(max * fraction);

            scrollBar.setBlockIncrement(viewHeight * 0.9);
            scrollBar.setValue(Math.max(0, Math.min(max, y)));
            scrollBar.setDisable(max <= 0);
        }
        finally {
            updatingScrollBar = false;
        }
    }

    /** Läs om dokumentets höjd, t.ex. efter byte av typsnitt eller textstorlek. */
    private void refreshScrollBar() {
        Platform.runLater(() -> {
            if (!loading && webEngine.getDocument() != null) {
                webEngine.executeScript("if (window.ekorrReportScroll) window.ekorrReportScroll();");
            }
        });
    }

    public void openContent(File contentFile) {
        currentElement = null;
        currentElementID = null;
        pendingElementID = null;
        styleNode = null;
        // Egen flagga: direkt efter load() kan motorns tillstånd fortfarande
        // vara SUCCEEDED från förra dokumentet
        loading = true;
        webEngine.load(contentFile.toURI().toString());
    }

    /** Tömmer vyn (när talboken stängs). */
    public void clear() {
        currentElement = null;
        currentElementID = null;
        pendingElementID = null;
        styleNode = null;
        loading = false;
        webEngine.loadContent("");
        updateScrollBar(0, 0, 0);
    }

    /** Anropas (på JavaFX-tråden) med id:t för det element man klickade på. */
    public void setOnSyncPointClicked(Consumer<String> handler) {
        onSyncPointClicked = handler == null ? id -> {} : handler;
    }

    /**
     * Lägger in programmets egen stil sist i head, så att den går före bokens
     * CSS. Elementet sparas så att typsnittet kan bytas utan omladdning.
     */
    // private void applyCSS() {
    //     Document doc = webEngine.getDocument();
    //     styleNode = doc.createElement("style");
    //     styleNode.appendChild(doc.createTextNode(buildCss()));
    //     doc.getDocumentElement().getElementsByTagName("head").item(0).appendChild(styleNode);
    // }

    private void applyCSS() {
        Document doc = webEngine.getDocument();
        if (doc == null || doc.getDocumentElement() == null) {
            return;
        }
        styleNode = doc.createElement("style");
        styleNode.appendChild(doc.createTextNode(buildCss()));
        org.w3c.dom.Node head = doc.getDocumentElement().getElementsByTagName("head").item(0);
        // Ett tomt dokument (efter clear) kan sakna head
        (head != null ? head : doc.getDocumentElement()).appendChild(styleNode);
    }

    /** Grund-CSS, temats färger, ev. typsnitt och sist markeringen. */
    private String buildCss() {
        StringBuilder css = new StringBuilder(CSS);
        css.append(theme.getContentCss());
        if (fontFamily != null && !fontFamily.isBlank()) {
            // Gäller alla element, även de som bokens CSS ger ett eget typsnitt
            String family = fontFamily.replace("\\", "").replace("\"", "");
            css.append("body, body * { font-family: \"").append(family).append("\" !important; }");
        }
        css.append(highlightCss(theme));
        return css.toString();
    }

    /** Byter färgtema. Gäller direkt, utan att boken laddas om. */
    public void setTheme(Theme newTheme) {
        theme = newTheme == null ? Theme.STANDARD : newTheme;
        updateCss();   // vid laddning läggs det in av applyCSS
    }

    private void updateCss() {
        if (styleNode != null && !loading) {
            styleNode.setTextContent(buildCss());
        }
    }

    /**
     * Typsnitt för texten, eller null för bokens eget. Måste vara installerat
     * i systemet – annars väljer WebView ett standardtypsnitt.
     */
    public void setFontFamily(String family) {
        fontFamily = family;
        updateCss();   // vid laddning läggs det in av applyCSS
        refreshScrollBar();
    }

    public String getFontFamily() {
        return fontFamily;
    }

    /** Textstorlek, 1.0 = normal. Påverkar bara texten, inte bilder och marginaler. */
    public void setFontScale(double scale) {
        webView.setFontScale(scale);
        refreshScrollBar();
    }

    public double getFontScale() {
        return webView.getFontScale();
    }

    /** Markerar elementet och scrollar dit. */
    public void highlight(String elementID) {
        if (loading || webEngine.getLoadWorker().getState() != Worker.State.SUCCEEDED) {
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

        // ((JSObject) element).call("scrollIntoView",
            // webEngine.executeScript("({ behavior: 'smooth', block: 'center' })"));

        scrollToFraction((JSObject) element, 0.25);
    }

    /** Behålls för befintlig kod: samma som highlight. */
    public void scrollToElement(String elementID) {
        highlight(elementID);
    }

    /**
     * Scrollar mjukt så att elementets överkant hamnar fraction av fönsterhöjden
     * från toppen (0.25 = en fjärdedel ned).
     */
    private void scrollToFraction(JSObject element, double fraction) {
        JSObject rect = (JSObject) element.call("getBoundingClientRect");
        JSObject window = (JSObject) webEngine.executeScript("window");

        double top = ((Number) rect.getMember("top")).doubleValue();             // relativt fönstret
        double scrollY = ((Number) window.getMember("pageYOffset")).doubleValue();
        double height = ((Number) window.getMember("innerHeight")).doubleValue();

        double target = Math.max(0, scrollY + top - height * fraction);
        webEngine.executeScript("window.scrollTo({ top: " + target + ", behavior: 'smooth' })");
    }

    private void setCurrentElement(JSObject element, String elementID) {
        clearCurrentElement();
        ((JSObject) element.getMember("classList")).call("add", CURRENT_CLASS);
        currentElement = element;
        currentElementID = elementID;
    }

    /**
     * Tar bort markeringen från alla element i dokumentet – inte bara från
     * det vi senast markerade – så att en gammal markering aldrig kan bli
     * kvar (t.ex. vid sidbrytningar).
     */
    private void clearCurrentElement() {
        currentElement = null;
        if (webEngine.getDocument() == null) {
            return;
        }
        webEngine.executeScript(
            "(function() {"
            + "  var marked = document.getElementsByClassName('" + CURRENT_CLASS + "');"
            // live-lista: ta bort bakifrån tills den är tom
            + "  for (var i = marked.length - 1; i >= 0; i--) {"
            + "    marked[i].classList.remove('" + CURRENT_CLASS + "');"
            + "  }"
            + "})()");
    }

    /** JavaScript -> Java. Måste vara public för att WebView ska se den. */
    public class JavaBridge {
        public void handleClick(String elementID) {
            onSyncPointClicked.accept(elementID);
        }

        /** Från SCROLL_SCRIPT. JavaScript-tal kommer som Integer eller Double. */
        public void onScroll(Object y, Object contentHeight, Object viewHeight) {
            updateScrollBar(number(y), number(contentHeight), number(viewHeight));
        }

        private double number(Object o) {
            return o instanceof Number ? ((Number) o).doubleValue() : 0;
        }
    }

    /** WebView med rullningslist – det som ska läggas in i fönstret. */
    public Region getContentView() {
        return container;
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
