package se.camabomedia.ekorr;

import se.camabomedia.ekorr.audio.playback.MpvPlayer;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.FileChooser.ExtensionFilter;

public class MainViewController implements Initializable {

    @FXML
    Stage stage;

    @FXML // fx:id="splitPane"
    private SplitPane splitPane;

    @FXML // fx:id="contentPane"
    private AnchorPane contentPane;

    @FXML // fx:id="nccPane"
    private AnchorPane nccPane;

    @FXML // fx:d="btnOpen"
    private Button btnOpen; // Value injected by FXMLLoader

    @FXML // fx:d="btnBack"
    private Button btnBack; // Value injected by FXMLLoader

    @FXML // fx:id="btnForward"
    private Button btnForward; // Value injected by FXMLLoader

    @FXML // fx:id="btnPause"
    private Button btnPause; // Value injected by FXMLLoader

    @FXML // fx:id="btnPlay"
    private Button btnPlay; // Value injected by FXMLLoader

    @FXML // fx:id="menuItemOpen"
    private MenuItem menuItemOpen; // Value injected by FXMLLoader

    @FXML // fx:id="x1"
    private Font x1; // Value injected by FXMLLoader

    @FXML // fx:id="x2"
    private Color x2; // Value injected by FXMLLoader

    @FXML // fx:id="x3"
    private Font x3; // Value injected by FXMLLoader

    @FXML // fx:id="x4"
    private Color x4; // Value injected by FXMLLoader

    public ContentView contentView;
    public NccView nccView;

    private Ncc ncc;
    private Content content;
    private Smil smil;
    MpvPlayer mpv = new MpvPlayer();

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

    }

    // private void adjustLayoutForFullscreen(AnchorPane contentPane) {
    //     System.out.println("ENTERING FULLSCREEN");
    //     splitPane.setDividerPositions(0.1, 0.8);

    //     defaultContentPaneWidth = contentView.webView.getWidth();
    //     defaultContentPaneHeight = contentView.webView.getHeight();

    //     // contentPane.setPrefSize(contentPane.getWidth(), contentPane.getHeight());
    //     contentView.webView.setPrefSize(contentPane.getWidth(), contentPane.getHeight());
    //     splitPane.requestLayout();
    // }

    // private void adjustLayoutForExitFullscreen(AnchorPane contentPane) {
    //     System.out.println("EXITING FULLSCREEN");
    //     splitPane.setDividerPositions(0.15, 0.85);

    //     // contentPane.setPrefSize(defaultContentPaneWidth, defaultContentPaneHeight);
    //     // contentPane.setPrefSize(contentPane.getWidth(), contentPane.getHeight());
    //     contentView.webView.setPrefSize(defaultContentPaneWidth, defaultContentPaneHeight);
    //     splitPane.requestLayout();
    // }

    void initData(Stage stage) {
        this.stage = stage;

        contentView = new ContentView();
        contentPane.getChildren().add(contentView.getContentView());

        AnchorPane.setTopAnchor(contentView.webView, 0.0);
        AnchorPane.setRightAnchor(contentView.webView, 0.0);
        AnchorPane.setBottomAnchor(contentView.webView, 0.0);
        AnchorPane.setLeftAnchor(contentView.webView, 0.0);

        nccView = new NccView();
        // nccView.webView.prefWidthProperty().bind(nccPane.widthProperty());
        // nccView.webView.prefHeightProperty().bind(nccPane.heightProperty());
        nccPane.getChildren().add(nccView.getNccView());

        AnchorPane.setTopAnchor(nccView.webView, 0.0);
        AnchorPane.setRightAnchor(nccView.webView, 0.0);
        AnchorPane.setBottomAnchor(nccView.webView, 0.0);
        AnchorPane.setLeftAnchor(nccView.webView, 0.0);


        // Listen for changes in fullscreen mode
        // contentView.webView.sceneProperty().addListener((observable, oldScene, newScene) -> {
        //     if (newScene != null) {
                // Retrieve the Stage when the WebView is added to a Scene
        // Stage splitPaneStage = (Stage) splitPane.getScene().getWindow();
        // stage.fullScreenProperty().addListener((ChangeListener<? super Boolean>) (fullscreenObservable, wasFullscreen, isFullscreen) -> {
        //             if (isFullscreen) {
        //                 adjustLayoutForFullscreen(contentPane);
        //             } else {
        //                 // Restore WebView size after exiting fullscreen
        //                 adjustLayoutForExitFullscreen(contentPane);
        //             }
        //         });
        //     }
        // });
    }

    private void selectProject() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        fileChooser.getExtensionFilters().addAll(new ExtensionFilter("Ncc file", "ncc.html"));
        File selectedFile = fileChooser.showOpenDialog(stage);
        openProject(selectedFile);
    }

    public void openProject(File selectedFile) {
        ncc = new Ncc(selectedFile);
        content = new Content(ncc.getDirectory(), ncc.getContentFile());
        smil = new Smil(ncc, content);
        contentView.openContent(ncc.getContentFilePath());
        nccView.openNcc(selectedFile);
        setFirstElement();
    }

    private void setFirstElement() {
        String firstElementID = smil.getFirstSmilPar().getContentID();
        contentView.setFirstElementID(firstElementID);
        contentView.scrollToElement(firstElementID);
    }

    // TODO gör private sen
    public void setFirstElementxxx() {
        Thread taskThread = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("GOT CALLED");
                System.out.println(ncc);
                content = new Content(ncc.getDirectory(), ncc.getContentFile());
                smil = new Smil(ncc, content);
                SmilPar firstSmilPar = smil.getFirstSmilPar();
                System.out.println(firstSmilPar);
                String contentID = firstSmilPar.getContentID();

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        contentView.scrollToElement(contentID);
                    }
                });
            }
        });
        taskThread.start();
    }

    @FXML
    void handleBtnOpen(ActionEvent event) {
        selectProject();
    }

    @FXML
    void handleBtnBack(ActionEvent event) {
        String currentElementID = contentView.getCurrentElementID();
        if (currentElementID != contentView.getFirstElementID()) {
            SmilPar smilPar = smil.getParFromContentID(currentElementID);
            SmilPar nextPar = smilPar.getPreviousSmilPar();
            String previousElementID = nextPar.getContentID();
            contentView.scrollToElement(previousElementID);
        }
    }

    @FXML
    void handleButtonForward(ActionEvent event) {
        String currentElementID = contentView.getCurrentElementID();
        SmilPar smilPar = smil.getParFromContentID(currentElementID);
        SmilPar nextPar = smilPar.getNextSmilPar();
        String nextElementID = nextPar.getContentID();
        if (nextElementID != null) {
            contentView.scrollToElement(nextElementID);
        }
    }


    @FXML
    void handleBtnPause(ActionEvent event) {

    }

    @FXML
    void handleBtnPlay(ActionEvent event) {

    }

    @FXML
    void handleMenuItemOpen(ActionEvent event) {
        selectProject();
    }
}
