package se.camabomedia.ekorraudio.ui;

import se.camabomedia.ekorraudio.Settings;
import se.camabomedia.ekorraudio.daisy.Content;
import se.camabomedia.ekorraudio.daisy.MpvPlayer;
import se.camabomedia.ekorraudio.daisy.Ncc;
import se.camabomedia.ekorraudio.daisy.Smil;
import se.camabomedia.ekorraudio.daisy.Timeline;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
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
    @FXML private Label labelStatus;

    private Stage stage;
    private ContentView contentView;
    private NccView nccView;

    private Ncc ncc;
    private Smil smil;
    private Timeline timeline;
    private Path edlFile;
    private final MpvPlayer mpv = new MpvPlayer();
    private Settings settings;

    /** Index (i tidslinjen) för det par som är markerat/spelas, eller -1. */
    private int currentIndex = -1;
    /** Mål för pågående hopp (ms), eller -1. */
    private long seekTarget = -1;
    private long seekStarted = 0;

    /** Hopp med vänster-/högerpil, ms. */
    private static final long SKIP_MS = 5000;

    private static final int MIN_SPEED = 100;
    private static final int MAX_SPEED = 400;
    private static final int SPEED_STEP = 10;
    private static final int DEFAULT_FAST_SPEED = 150;

    /** Aktuell hastighet i procent. */
    private int speed = 100;
    /** Den högre hastighet som "-" växlar till (senast inställda över 100 %). */
    private int fastSpeed = DEFAULT_FAST_SPEED;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
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

    void initData(Stage stage, Settings settings) {
        this.stage = stage;
        this.settings = settings;

        int saved = settings.getFastSpeed();
        if (saved > MIN_SPEED && saved <= MAX_SPEED) {
            fastSpeed = saved;
        }

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
        // Hastighetstangenterna läses som tecken (",", ".", "-"), så att de
        // fungerar oberoende av tangentbordslayout – även minus på numeriska
        // tangentbordet
        stage.getScene().addEventFilter(KeyEvent.KEY_TYPED, e -> {
            if (timeline == null) {
                return;
            }
            switch (e.getCharacter()) {
            case " ":
                e.consume();
                break;
            case ",":
                changeSpeed(-SPEED_STEP);
                e.consume();
                break;
            case ".":
                changeSpeed(+SPEED_STEP);
                e.consume();
                break;
            case "-":
                toggleFastSpeed();
                e.consume();
                break;
            default:
                break;
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
        String books = settings.getDefaultLocation();
        if (books != null && new File(books).isDirectory()) {
            fileChooser.setInitialDirectory(new File(books));
        }
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
            seekTo(timeline.startOf(index));
        }
        updateStatus(timeline.startOf(index));
    }

    /** Hoppar deltaMs framåt (positivt) eller bakåt (negativt) i ljudet. */
    private void skip(long deltaMs) {
        if (timeline == null) {
            return;
        }
        // Under ett pågående hopp är mpv:s position inaktuell – utgå från målet,
        // så att flera snabba tryck läggs ihop
        long from = seekTarget >= 0 ? seekTarget : mpv.getPosition();
        long target = Math.max(0, Math.min(from + deltaMs, timeline.duration() - 1));

        int index = timeline.indexAt(target);
        if (index != currentIndex) {
            currentIndex = index;
            contentView.highlight(timeline.get(index).getContentID());
        }
        seekTo(target);
        updateStatus(target);
    }

    private void seekTo(long target) {
        // Tills mpv rapporterar en position vid målet är rapporterna
        // inaktuella (ett hopp till en annan ljudfil kan ta en stund)
        seekTarget = target;
        seekStarted = System.nanoTime();
        mpv.seek(target);
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
     *   Mellanslag      spela/pausa
     *   Vänster/Höger   5 sekunder bakåt/framåt
     *   Upp/Ned         föregående/nästa fras
     *   Skift+V/H       föregående/nästa sida
     *   Skift+Upp/Ned   föregående/nästa rubrik
     *   , och .         sänk/höj hastigheten 10 % (100–400 %)
     *   -               växla mellan 100 % och inställd högre hastighet
     * (hastighetstangenterna hanteras som tecken, se initData)
     */
    private void onKeyPressed(KeyEvent event) {
        if (timeline == null) {
            return;
        }
        boolean shift = event.isShiftDown();
        boolean handled = true;
        switch (event.getCode()) {
        case SPACE:
            togglePlay();
            break;
        case LEFT:
            if (shift) {
                step(false, Timeline.Level.PAGE);
            }
            else {
                skip(-SKIP_MS);
            }
            break;
        case RIGHT:
            if (shift) {
                step(true, Timeline.Level.PAGE);
            }
            else {
                skip(SKIP_MS);
            }
            break;
        case UP:
            step(false, shift ? Timeline.Level.HEADING : Timeline.Level.PHRASE);
            break;
        case DOWN:
            step(true, shift ? Timeline.Level.HEADING : Timeline.Level.PHRASE);
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
        case UP:
        case DOWN:
            return true;
        default:
            return false;
        }
    }

    // ------------------------------------------------------------ hastighet

    /** , och . : ändra hastigheten i steg om 10 %, inom 100–400 %. */
    private void changeSpeed(int delta) {
        int s = Math.max(MIN_SPEED, Math.min(MAX_SPEED, speed + delta));
        setSpeed(s);
        if (s > MIN_SPEED) {
            rememberFastSpeed(s);
        }
    }

    /** - : växla mellan 100 % och den senast inställda högre hastigheten. */
    private void toggleFastSpeed() {
        setSpeed(speed == MIN_SPEED ? fastSpeed : MIN_SPEED);
    }

    private void setSpeed(int percent) {
        speed = percent;
        mpv.setSpeed(percent / 100.0);
        updateStatus(mpv.getPosition());
    }

    private void rememberFastSpeed(int percent) {
        if (percent != fastSpeed) {
            fastSpeed = percent;
            settings.addFastSpeed(percent);
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
                            + Timeline.format(ms) + " / " + Timeline.format(timeline.duration())
                            + "   " + speed + " %");
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.initOwner(stage);
        alert.setTitle("Fel");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ------------------------------------------------------------ settings

    // private void setDefaultLocation() {
    //     DirectoryChooser directoryChooser = new DirectoryChooser();
    //     directoryChooser.setTitle("Välj mapp för talböcker");
    //     File selectedDirectory = directoryChooser.showDialog(stage);
    //     settings.addDefaultLocation(selectedDirectory.getPath());
    //     settings.addWantsDefaultLocation(true);
    // }

    // private void setPrivateDefaultLocation() {
    //     DirectoryChooser directoryChooser = new DirectoryChooser();
    //     directoryChooser.setTitle("Välj mapp för rapporter");
    //     File selectedDirectory = directoryChooser.showDialog(stage);
    //     settings.addPrivateDefaultLocation(selectedDirectory.getPath());
    // }

    private void setLocations() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(stage);
        dialog.setTitle("Ange mappar");
        dialog.setHeaderText("Var ligger talböckerna, och var ska kopior av rapporterna sparas?");

        TextField bookField = new TextField(nullToEmpty(settings.getDefaultLocation()));
        TextField reportField = new TextField(nullToEmpty(settings.getPrivateDefaultLocation()));
        for (TextField f : new TextField[] { bookField, reportField }) {
            f.setEditable(false);
            f.setPrefColumnCount(40);
        }

        Button bookButton = new Button("Välj …");
        bookButton.setOnAction(e -> chooseDirectory("Välj mapp för talböcker", bookField));
        Button reportButton = new Button("Välj …");
        reportButton.setOnAction(e -> chooseDirectory("Välj mapp för rapporter", reportField));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 10, 0, 10));
        grid.addRow(0, new Label("Talböcker:"), bookField, bookButton);
        grid.addRow(1, new Label("Rapporter:"), reportField, reportButton);
        GridPane.setHgrow(bookField, Priority.ALWAYS);
        GridPane.setHgrow(reportField, Priority.ALWAYS);
        dialog.getDialogPane().setContent(grid);

        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Avbryt", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okButton, cancelButton);

        dialog.showAndWait()
            .filter(response -> response == okButton)
            .ifPresent(response -> {
                    if (!bookField.getText().isBlank()) {
                        settings.addDefaultLocation(bookField.getText());
                        settings.addWantsDefaultLocation(true);
                    }
                    if (!reportField.getText().isBlank()) {
                        settings.addPrivateDefaultLocation(reportField.getText());
                    }
                });
    }

    /** Öppnar en mappväljare som börjar i fältets nuvarande mapp och skriver in valet. */
    private void chooseDirectory(String title, TextField field) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(title);
        File current = new File(field.getText());
        if (current.isDirectory()) {
            chooser.setInitialDirectory(current);
        }
        // Använd dialogens eget fönster som ägare, så att väljaren hamnar ovanpå den
        File selected = chooser.showDialog(field.getScene().getWindow());
        if (selected != null) {          // null = användaren avbröt
            field.setText(selected.getPath());
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }



    private void setProofReaderName() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(stage);
        dialog.setTitle("Korrekturläsare");
        dialog.setHeaderText("Ange ditt namn");

        TextField firstNameField = new TextField();
        TextField secondNameField = new TextField();

        // Fyll i det namn som redan finns sparat (förnamn = första ordet)
        String current = settings.getProofreader();
        if (current != null && !current.isBlank()) {
            String[] parts = current.strip().split("\\s+", 2);
            firstNameField.setText(parts[0]);
            if (parts.length > 1) {
                secondNameField.setText(parts[1]);
            }
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 10, 0, 10));
        grid.addRow(0, new Label("Förnamn:"), firstNameField);
        grid.addRow(1, new Label("Efternamn:"), secondNameField);
        dialog.getDialogPane().setContent(grid);

        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Avbryt", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okButton, cancelButton);

        // OK går bara att klicka när båda fälten är ifyllda
        Node ok = dialog.getDialogPane().lookupButton(okButton);
        ok.disableProperty().bind(Bindings.createBooleanBinding(
          () -> firstNameField.getText().isBlank() || secondNameField.getText().isBlank(),
          firstNameField.textProperty(), secondNameField.textProperty()));
        Platform.runLater(firstNameField::requestFocus);

        dialog.showAndWait()
            .filter(response -> response == okButton)
            .ifPresent(response -> settings.addProofreader(firstNameField.getText().strip()
                                                           + " " +
                                                           secondNameField.getText().strip()));
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
        step(false, Timeline.Level.PHRASE);
    }

    @FXML
    void handleButtonForward(ActionEvent event) {
        step(true, Timeline.Level.PHRASE);
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

    @FXML
    void handleMenuItemName(ActionEvent event) {
        setProofReaderName();
    }

    @FXML
    void handleMenuItemLocations(ActionEvent event) {
        setLocations();
    }

}
