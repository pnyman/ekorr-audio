package se.camabomedia.ekorraudio.ui;

import se.camabomedia.ekorraudio.daisy.Content;
import se.camabomedia.ekorraudio.daisy.MpvPlayer;
import se.camabomedia.ekorraudio.daisy.Ncc;
import se.camabomedia.ekorraudio.daisy.Smil;
import se.camabomedia.ekorraudio.daisy.SmilPar;
import se.camabomedia.ekorraudio.daisy.Timeline;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class MainViewController implements Initializable {

    @FXML private SplitPane splitPane;
    @FXML private AnchorPane contentPane;
    @FXML private AnchorPane nccPane;
    @FXML private Button btnOpen;
    @FXML private Button btnBack;
    @FXML private Button btnForward;
    @FXML private Button btnPause;
    @FXML private Button btnPlay;
    @FXML private MenuItem menuItemOpen;
    @FXML private ChoiceBox<Timeline.Level> choiceLevel;
    @FXML private Label labelStatus;

    private Stage stage;
    private ContentView contentView;
    private NccView nccView;

    private Ncc ncc;
    private Smil smil;
    private Timeline timeline;
    private Path edlFile;
    private final MpvPlayer mpv = new MpvPlayer();

    /** Index (i tidslinjen) för det par som är markerat/spelas, eller -1. */
    private int currentIndex = -1;
    /** Mål för pågående hopp (ms), eller -1. */
    private long seekTarget = -1;
    private long seekStarted = 0;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        choiceLevel.getItems().setAll(Timeline.Level.values());
        choiceLevel.setValue(Timeline.Level.PHRASE);

        // mpv rapporterar från en bakgrundstråd – flytta till JavaFX-tråden
        mpv.setListener(new MpvPlayer.Listener() {
            @Override
            public void positionChanged(long ms) {
                Platform.runLater(() -> onPosition(ms));
            }

            @Override
            public void pausedChanged(boolean paused) {
                Platform.runLater(() -> updateButtons());
            }
        });
        updateButtons();
    }

    void initData(Stage stage) {
        this.stage = stage;

        contentView = new ContentView();
        contentPane.getChildren().add(contentView.getContentView());
        anchorToAllSides(contentView.webView);

        nccView = new NccView();
        nccPane.getChildren().add(nccView.getNccView());
        anchorToAllSides(nccView.webView);

        contentView.setOnSyncPointClicked(this::onContentClicked);
        nccView.setOnItemClicked(this::onNccClicked);

        // Filter (inte handler) så att tangenterna fungerar även när en WebView
        // eller knapp har fokus
        stage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);
        // WebView scrollar på tecknet " " (KEY_TYPED), inte på själva
        // tangenttrycket – stoppa det också, liksom släppet av våra tangenter
        stage.getScene().addEventFilter(KeyEvent.KEY_TYPED, e -> {
            if (timeline != null && " ".equals(e.getCharacter())) {
                e.consume();
            }
        });
        stage.getScene().addEventFilter(KeyEvent.KEY_RELEASED, e -> {
            if (timeline != null && isPlayerKey(e)) {
                e.consume();
            }
        });
        stage.setOnHidden(e -> mpv.close());
    }

    private static void anchorToAllSides(javafx.scene.Node node) {
        AnchorPane.setTopAnchor(node, 0.0);
        AnchorPane.setRightAnchor(node, 0.0);
        AnchorPane.setBottomAnchor(node, 0.0);
        AnchorPane.setLeftAnchor(node, 0.0);
    }

    // ------------------------------------------------------------ projekt

    private void selectProject() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Öppna talbok");
        fileChooser.getExtensionFilters().add(new ExtensionFilter("NCC-fil", "ncc.html"));
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            openProject(selectedFile);
        }
    }

    public void openProject(File nccFile) {
        mpv.close();
        currentIndex = -1;

        ncc = new Ncc(nccFile);
        Content content = new Content(ncc.getDirectory(), ncc.getContentFile());
        smil = new Smil(ncc, content);
        timeline = new Timeline(ncc, smil);
        seekTarget = -1;
        reportMissingSyncPoints(content);

        contentView.openContent(ncc.getContentFilePath());
        nccView.openNcc(nccFile);

        if (timeline.size() == 0) {
            showError("Kan inte spela upp", "Hittade inga synkpunkter i smil-filerna.");
            updateButtons();
            return;
        }

        goTo(0, false);

        try {
            if (edlFile == null) {
                edlFile = Files.createTempFile("ekorr-", ".edl");
                edlFile.toFile().deleteOnExit();
            }
            timeline.writeEdl(edlFile);
            mpv.open(edlFile, timeline.startOf(0));
        }
        catch (IOException e) {
            e.printStackTrace();
            showError("Kan inte spela upp ljudet", e.getMessage());
        }
        updateButtons();
    }

    /** Skriver ut synkpunkter som smil-filerna pekar på men som saknas i texten. */
    private void reportMissingSyncPoints(Content content) {
        if (content.getDoc() == null) {
            return;
        }
        int missing = 0;
        for (int i = 0; i < timeline.size(); i++) {
            String id = timeline.get(i).getContentID();
            if (content.getDoc().getElementById(id) == null) {
                if (missing < 10) {
                    System.err.println("Synkpunkt saknas i innehållsfilen: " + id
                                       + " (" + timeline.get(i).getParElementID() + ")");
                }
                missing++;
            }
        }
        if (missing > 0) {
            System.err.println(missing + " av " + timeline.size() + " synkpunkter saknas i innehållsfilen");
        }
    }

    // ------------------------------------------------------------ navigering

    /**
     * Markerar par nummer index och (om seek) flyttar ljudet dit.
     * Pausläget behålls: spelar det så fortsätter det spela från nya stället.
     */
    private void goTo(int index, boolean seek) {
        if (timeline == null || index < 0 || index >= timeline.size()) {
            return;
        }
        currentIndex = index;
        contentView.highlight(timeline.get(index).getContentID());
        if (seek) {
            // Tills mpv rapporterar en position vid målet är rapporterna
            // inaktuella (ett hopp till en annan ljudfil kan ta en stund)
            long target = timeline.startOf(index);
            seekTarget = target;
            seekStarted = System.nanoTime();
            mpv.seek(target);
        }
        updateStatus(timeline.startOf(index));
    }

    private void step(boolean forward, Timeline.Level level) {
        if (timeline == null) {
            return;
        }
        int from = Math.max(currentIndex, 0);
        int target = forward ? timeline.next(from, level) : timeline.previous(from, level);
        if (target >= 0) {
            goTo(target, true);
        }
    }

    /** Positionen från mpv – flytta markeringen när ett nytt par börjar. */
    private void onPosition(long ms) {
        if (timeline == null || isStaleAfterSeek(ms)) {
            return;
        }
        int index = timeline.indexAt(ms);
        if (index != currentIndex) {
            currentIndex = index;
            contentView.highlight(timeline.get(index).getContentID());
        }
        updateStatus(ms);
    }

    /**
     * Efter ett hopp: släpp igenom positioner först när de ligger vid målet
     * (från strax före till några sekunder efter). Efter 3 s ges upp, så att
     * markeringen aldrig fastnar om mpv av någon anledning inte hamnar där.
     */
    private boolean isStaleAfterSeek(long ms) {
        if (seekTarget < 0) {
            return false;
        }
        boolean arrived = ms >= seekTarget - 200 && ms <= seekTarget + 3000;
        boolean gaveUp = System.nanoTime() - seekStarted > 3_000_000_000L;
        if (arrived || gaveUp) {
            seekTarget = -1;
            return false;
        }
        return true;
    }

    private void onContentClicked(String contentID) {
        if (smil == null) {
            return;
        }
        int index = timeline.indexOf(smil.getParFromContentID(contentID));
        if (index >= 0) {
            goTo(index, true);
        }
    }

    private void onNccClicked(String parID) {
        if (smil == null) {
            return;
        }
        int index = timeline.indexOf(smil.getParFromParID(parID));
        if (index >= 0) {
            goTo(index, true);
        }
    }

    // ------------------------------------------------------------ tangenter

    /**
     *   Mellanslag        spela/pausa
     *   Vänster/Höger     föregående/nästa på vald nivå
     *   Ctrl+Vänster/Höger föregående/nästa rubrik
     *   PgUp/PgDn         föregående/nästa sida
     */
    private void onKeyPressed(KeyEvent event) {
        if (timeline == null) {
            return;
        }
        boolean handled = true;
        switch (event.getCode()) {
        case SPACE:
            togglePlay();
            break;
        case LEFT:
            step(false, event.isControlDown() ? Timeline.Level.HEADING : choiceLevel.getValue());
            break;
        case RIGHT:
            step(true, event.isControlDown() ? Timeline.Level.HEADING : choiceLevel.getValue());
            break;
        case PAGE_UP:
            step(false, Timeline.Level.PAGE);
            break;
        case PAGE_DOWN:
            step(true, Timeline.Level.PAGE);
            break;
        default:
            handled = false;
        }
        if (handled) {
            event.consume();
        }
    }

    private static boolean isPlayerKey(KeyEvent e) {
        switch (e.getCode()) {
        case SPACE:
        case LEFT:
        case RIGHT:
        case PAGE_UP:
        case PAGE_DOWN:
            return true;
        default:
            return false;
        }
    }

    private void togglePlay() {
        if (mpv.isPaused()) {
            play();
        }
        else {
            mpv.pause();
        }
    }

    private void play() {
        // Står markeringen på ett annat ställe än ljudet (t.ex. efter att
        // boken spelats klart), börja från markeringen
        if (currentIndex >= 0 && timeline.indexAt(mpv.getPosition()) != currentIndex) {
            mpv.seek(timeline.startOf(currentIndex));
        }
        mpv.play();
    }

    // ------------------------------------------------------------ visning

    private void updateButtons() {
        boolean ready = timeline != null && mpv.isOpen();
        boolean playing = ready && !mpv.isPaused();
        btnPlay.setDisable(!ready || playing);
        btnPause.setDisable(!playing);
        btnBack.setDisable(timeline == null);
        btnForward.setDisable(timeline == null);
    }

    private void updateStatus(long ms) {
        if (timeline == null || currentIndex < 0) {
            labelStatus.setText("");
            return;
        }
        String page = timeline.get(currentIndex).getPage();
        labelStatus.setText((page != null ? "Sida " + page + "   " : "")
                            + Timeline.format(ms) + " / " + Timeline.format(timeline.duration()));
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.initOwner(stage);
        alert.setTitle("Fel");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ------------------------------------------------------------ FXML

    @FXML
    void handleBtnOpen(ActionEvent event) {
        selectProject();
    }

    @FXML
    void handleMenuItemOpen(ActionEvent event) {
        selectProject();
    }

    @FXML
    void handleBtnBack(ActionEvent event) {
        step(false, choiceLevel.getValue());
    }

    @FXML
    void handleButtonForward(ActionEvent event) {
        step(true, choiceLevel.getValue());
    }

    @FXML
    void handleBtnPlay(ActionEvent event) {
        if (timeline != null) {
            play();
        }
    }

    @FXML
    void handleBtnPause(ActionEvent event) {
        mpv.pause();
    }
}
