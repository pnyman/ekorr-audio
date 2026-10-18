package se.camabomedia.ekorraudio.ui;

import java.io.File;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;
import netscape.javascript.JSObject;

public class ContentView {

    public WebView webView;
    static WebEngine webEngine;
    private boolean isEventListenerAdded = false;

    JavaBridge javaBridge;

    private JSObject currentElement = null;
    private String currentElementID = null;

    private String firstElementID;

    // body {font-family: arial, helvetica, verdana, sans-serif; margin-left: 10%; margin-right: 10%;}

    private static final String CSS =
        "body {"
        + "    margin-left: 3%; "
        + "    margin-right: 3%; "
        + "}" ;

    public ContentView() {
        webView = new WebView();
        webEngine = webView.getEngine();

        // Enable JavaScript
        webEngine.setJavaScriptEnabled(true);

        // Create an instance of the JavaBridge
        javaBridge = new JavaBridge(webEngine);

        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == Worker.State.SUCCEEDED && !isEventListenerAdded) {

                applyCSS();

                JSObject jsobj = (JSObject) webEngine.executeScript("window");
                jsobj.setMember("java", javaBridge);

                // Execute JavaScript code to handle the click event
                webEngine.executeScript(
                        "document.addEventListener('click', function(event) {"
                                + "    var syncpoint;"
                                + "    if (event.target.hasAttribute('id')) {"
                                + "        syncpoint = event.target;"
                                + "    }"
                                + "    else if (event.target.parentNode.hasAttribute('id')) {"
                                + "        syncpoint = event.target.parentNode;"
                                + "    }"
                                + "    else if (event.target.parentNode.parentNode.hasAttribute('id')) {"
                                + "        syncpoint = event.target.parentNode.parentNode;"
                                + "    }"
                                + "    java.handleClick(syncpoint.id);"
                                + "});");

                isEventListenerAdded = true;

            }
        });

    }

    public void openContent(File contentFile) {
        // File file = new File(nccFile);
        webEngine.load(contentFile.toURI().toString());
    }

    private void applyCSS() {
        Document doc = webEngine.getDocument() ;
        Element styleNode = doc.createElement("style");
        Text styleContent = doc.createTextNode(CSS);
        styleNode.appendChild(styleContent);
        doc.getDocumentElement().getElementsByTagName("head").item(0).appendChild(styleNode);
    }

    public void scrollToElement(String elementID) {
        // Check if the page is already loaded
        if (webEngine.getLoadWorker().getState() == Worker.State.SUCCEEDED) {
            // Page already loaded, directly scroll to the element
            scroll(elementID);
        } else {
            // Wait for the page to finish loading
            webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == Worker.State.SUCCEEDED) {
                    // Page loaded, scroll to the element
                    scroll(elementID);
                }
            });
        }
    }

    private void scroll(String elementID) {
        // Get the document object of the loaded page
        JSObject document = (JSObject) webEngine.executeScript("window.document");

        // Get the element by its ID
        JSObject element = (JSObject) document.call("getElementById", elementID);

        // Scroll to the element
        if (element != null) {
            // Object result = element.call("getBoundingClientRect");
            // JSObject rect = (JSObject) result;

            // double elementTop = Double.parseDouble(rect.getMember("top").toString());
            // double elementHeight = Double.parseDouble(rect.getMember("height").toString());
            // double windowHeight = Double.parseDouble(webEngine.executeScript("window.innerHeight").toString());

            // double scrollY = elementTop + elementHeight / 2 - windowHeight / 2;
            // webEngine.executeScript("window.scrollTo(0, " + scrollY + ")");

            webEngine.executeScript("document.getElementById('" + elementID + "').scrollIntoView({ behavior: 'smooth', block: 'center' })");
            setCurrentElement(element, elementID);

        }
    }

    private void setCurrentElement(JSObject element, String elementID) {
        if (currentElement != null) {
            currentElement.setMember("style", "background-color: " + "white");
        }

        element.setMember("style", "background-color: " + "yellow");

        currentElement = element;
        currentElementID = elementID;
    }

    // JavaScript bridge class to communicate with Java
    public class JavaBridge {
        WebEngine webEngine;

        public JavaBridge(WebEngine webEngine) {
            this.webEngine = webEngine;
        }

        public void handleClick(String elementID) {
            System.out.println("Clicked element ID: " + elementID);

            Document doc = webEngine.getDocument();
            Element element = doc.getElementById(elementID);
            String value = element.getTextContent();

            System.out.println(value);

            ContentView.this.setCurrentElement((JSObject) element, elementID);
        }

        public void scrollToId(String elementID) {
            System.out.println(elementID);
            // Retrieve the JavaScript window object
            JSObject window = (JSObject) webEngine.executeScript("window");
            window.call("scrollToElementById", elementID);
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


    // public static void main(String[] args) {
    //     launch(args);
    //     scrollWebViewToElement(webEngine, "id_fixgen_665");
    // }

}
