package se.camabomedia.ekorr;

import java.io.File;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import javafx.application.Application;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

public class NccView {

    public WebView webView;
    static WebEngine webEngine;
    private boolean isEventListenerAdded = false;

    public NccView() {
        webView = new WebView();
        webEngine = webView.getEngine();

        // Enable JavaScript
        webEngine.setJavaScriptEnabled(true);

        // Create an instance of the JavaBridge
        JavaBridge javaBridge = new JavaBridge(webEngine);


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


    public void openNcc(File contentFile) {
        // File file = new File(nccFile);
        webEngine.load(contentFile.toURI().toString());
    }

    private void applyCSS() {
        webEngine.setUserStyleSheetLocation(getClass().getResource("ncc.css").toString());
    }

    static void scrollWebViewToElement(String elementId) {
        webEngine.executeScript("document.getElementById('" + elementId + "').scrollIntoView({ behavior: 'smooth', block: 'center' })");
    }

    // JavaScript bridge class to communicate with Java
    public class JavaBridge {
        WebEngine webEngine;

        public JavaBridge(WebEngine webEngine) {
            this.webEngine = webEngine;
        }

        public void handleClick(String elementId) {
            System.out.println("Clicked element ID: " + elementId);

            Document doc = webEngine.getDocument();
            Element element = doc.getElementById(elementId);
            String value = element.getTextContent();

            System.out.println(value);
        }

        public void scrollToId(String elementId) {
            // Retrieve the JavaScript window object
            JSObject window = (JSObject) webEngine.executeScript("window");
            window.call("scrollToElementById", elementId);
        }
    }

    public WebView getNccView() {
        return webView;
    }

    // public static void main(String[] args) {
    //     launch(args);
    //     scrollWebViewToElement(webEngine, "id_fixgen_665");
    // }

}
