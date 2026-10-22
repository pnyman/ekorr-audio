package se.camabomedia.ekorraudio.ui;

import se.camabomedia.ekorraudio.Settings;
import se.camabomedia.ekorraudio.daisy.Content;
import se.camabomedia.ekorraudio.daisy.MpvPlayer;
import se.camabomedia.ekorraudio.daisy.Ncc;
import se.camabomedia.ekorraudio.daisy.Smil;
import se.camabomedia.ekorraudio.daisy.Timeline;
import se.camabomedia.ekorraudio.report.Comment;
import se.camabomedia.ekorraudio.report.Report;
import se.camabomedia.ekorraudio.report.ReportCopy;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.WindowEvent;

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
import javafx.scene.layout.HBox;
import javafx.geometry.Orientation;
import javafx.scene.control.Separator;
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
    @FXML private HBox toolbar;

    // Verktygsradens knappar – skapas i buildToolbar()
    private Button btnOpen;
    private Button btnClose;
    private Button btnShowReport;
    private Button btnReportFolder;
    private Button btnBack;
    private Button btnPlay;
    private Button btnPause;
    private Button btnForward;
    private Button btnSlower;
    private Button btnFaster;
    private Label speedLabel;
    private Button btnNewComment;
    private Button btnEditComment;
    private Button btnDeleteComment;
    @FXML private MenuItem menuItemOpen;
    @FXML private javafx.scene.control.Menu menuTheme;
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

    private CommentTable commentTable;
    private Report report;

    private static final String MAIN_CSS = "/se/camabomedia/ekorraudio/main.css";
    private Theme theme = Theme.STANDARD;

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
        buildToolbar();
        updateButtons();
    }

    /**
     * Verktygsraden: ikonknappar i grupper. Ikonnamnen kommer från Ikonlis
     * Material Design-paket (mdi2…). Ett namn som inte finns ger en
     * frågetecken-ikon och ett meddelande i konsolen.
     */
    private void buildToolbar() {
        toolbar.getStyleClass().add("toolbar");

        btnOpen = IconButton.create("mdi2f-folder-open-outline", "Öppna talbok", this::selectProject);
        btnClose = IconButton.create("mdi2c-close-box-outline", "Stäng talboken", this::closeProject);

        btnShowReport = IconButton.create("mdi2f-file-document-outline", "Visa rapporten", this::showReport);
        btnReportFolder = IconButton.create("mdi2f-folder-text-outline", "Öppna rapportmappen", this::openReportFolder);

        btnBack = IconButton.create("mdi2s-skip-previous", "Föregående fras (↑)",
                                    () -> step(false, Timeline.Level.PHRASE));
        btnPlay = IconButton.create("mdi2p-play", "Spela (Mellanslag)", () -> {
            if (timeline != null) {
                play();
            }
        });
        btnPause = IconButton.create("mdi2p-pause", "Pausa (Mellanslag)", mpv::pause);
        btnForward = IconButton.create("mdi2s-skip-next", "Nästa fras (↓)",
                                       () -> step(true, Timeline.Level.PHRASE));

        btnSlower = IconButton.create("mdi2m-minus-circle-outline", "Långsammare (,)",
                                      () -> changeSpeed(-SPEED_STEP));
        speedLabel = new Label();
        speedLabel.getStyleClass().add("speed-label");
        btnFaster = IconButton.create("mdi2p-plus-circle-outline", "Snabbare (.)",
                                      () -> changeSpeed(+SPEED_STEP));

        btnNewComment = IconButton.create("mdi2c-comment-plus-outline", "Ny kommentar (Enter)",
                                          this::createComment);
        btnEditComment = IconButton.create("mdi2c-comment-edit-outline", "Redigera kommentar (dubbelklick)",
                                           () -> {
                                               Comment c = commentTable.getSelected();
                                               if (c != null) {
                                                   editComment(c);
                                               }
                                           });
        btnDeleteComment = IconButton.create("mdi2c-comment-remove-outline", "Ta bort kommentar (Delete)",
                                             () -> {
                                                 Comment c = commentTable.getSelected();
                                                 if (c != null) {
                                                     deleteComment(c);
                                                 }
                                             });

        toolbar.getChildren().setAll(
            btnOpen, btnClose,
            separator(),
            btnShowReport, btnReportFolder,
            separator(),
            btnBack, btnPlay, btnPause, btnForward,
            separator(),
            btnSlower, speedLabel, btnFaster,
            separator(),
            btnNewComment, btnEditComment, btnDeleteComment);
        updateSpeedLabel();
    }

    private static Separator separator() {
        return new Separator(Orientation.VERTICAL);
    }

    private void updateSpeedLabel() {
        if (speedLabel != null) {
            speedLabel.setText(speed + " %");
        }
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

        // Högerkolumnen: kommentarstabellen. Kolumnerna i FXML har fast bredd
        // (min = max = 250) – lås upp dem så att avdelarna går att dra.
        AnchorPane commentPane = (AnchorPane) splitPane.getItems().get(2);
        commentPane.getChildren().clear();   // etiketten "Comments" från FXML
        commentTable = new CommentTable();
        commentPane.getChildren().add(commentTable.getNode());
        anchorToAllSides(commentTable.getNode());
        commentTable.setOnEdit(this::editComment);
        commentTable.setOnDelete(this::deleteComment);
        commentTable.setOnReordered(this::reorderComments);
        // Redigera/Ta bort-knapparna gäller markerad kommentar
        commentTable.getNode().getSelectionModel().selectedItemProperty()
            .addListener((obs, old, selected) -> updateButtons());

        stage.getScene().getStylesheets().add(resource(MAIN_CSS));

        for (AnchorPane side : new AnchorPane[] { nccPane, commentPane }) {
            side.setMinWidth(120);
            side.setMaxWidth(Double.MAX_VALUE);
        }
        contentPane.setMinWidth(200);

        contentView.setFontFamily(settings.getFontFamily());
        contentView.setFontScale(clampScale(settings.getFontScale()));

        buildThemeMenu();
        applyTheme(Theme.fromName(settings.getTheme()));
        // Tomma dokument i vyerna, så att temats bakgrund syns innan en bok öppnas
        contentView.clear();
        nccView.clear();

        restoreLayout();
        // Spara layouten när fönstret stängs. addEventHandler körs före App:s
        // setOnCloseRequest, som avslutar med System.exit.
        stage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, e -> saveLayout());

        // Filter (inte handler) så att tangenterna fungerar även när en WebView
        // eller knapp har fokus
        stage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);
        // WebView scrollar på tecknet " " (KEY_TYPED), inte på själva
        // tangenttrycket – stoppa det också, liksom släppet av våra tangenter
        // Hastighetstangenterna läses som tecken (",", ".", "-"), så att de
        // fungerar oberoende av tangentbordslayout – även minus på numeriska
        // tangentbordet
        stage.getScene().addEventFilter(KeyEvent.KEY_TYPED, e -> {
            if (timeline == null || e.isShortcutDown()) {
                return;   // Ctrl+- är textstorlek, inte hastighet
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
        loadReport();

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
        // Textstorlek fungerar även utan öppnad bok
        if (event.isShortcutDown() && handleZoomKey(event)) {
            event.consume();
            return;
        }
        if (timeline == null) {
            return;
        }
        if (event.isAltDown()) {
            return;   // Alt+pil flyttar kommentarer i tabellen
        }
        boolean shift = event.isShiftDown();
        boolean handled = true;
        switch (event.getCode()) {
        case ENTER:
            // Öppna formuläret efter att tangenthändelsen är klar, så att
            // Enter inte också hamnar i det nya fönstret
            Platform.runLater(this::createComment);
            break;
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
        if (e.isAltDown()) {
            return false;
        }
        switch (e.getCode()) {
        case ENTER:
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

    // ------------------------------------------------------------ text

    private static final double MIN_SCALE = 0.5;
    private static final double MAX_SCALE = 3.0;
    private static final double SCALE_STEP = 0.1;

    /**
     * Ctrl++ / Ctrl+- / Ctrl+0. Tangentkoderna skiljer sig mellan layouter
     * (på svenskt tangentbord kan +-tangenten rapporteras som PLUS eller
     * EQUALS), så även tangentens text kontrolleras.
     */
    private boolean handleZoomKey(KeyEvent e) {
        String text = e.getText() == null ? "" : e.getText();
        switch (e.getCode()) {
        case PLUS:
        case ADD:
        case EQUALS:
            changeFontScale(+SCALE_STEP);
            return true;
        case MINUS:
        case SUBTRACT:
            changeFontScale(-SCALE_STEP);
            return true;
        case DIGIT0:
        case NUMPAD0:
            setFontScale(1.0);
            return true;
        default:
            if (text.equals("+")) {
                changeFontScale(+SCALE_STEP);
                return true;
            }
            if (text.equals("-")) {
                changeFontScale(-SCALE_STEP);
                return true;
            }
            return false;
        }
    }

    private void changeFontScale(double delta) {
        setFontScale(contentView.getFontScale() + delta);
    }

    private void setFontScale(double scale) {
        // Avrunda till hela tiondelar, så att upprepade steg inte driver iväg
        double s = clampScale(Math.round(scale * 10) / 10.0);
        contentView.setFontScale(s);
        settings.addFontScale(s);
    }

    private static double clampScale(double s) {
        if (s <= 0 || Double.isNaN(s)) {
            return 1.0;   // inget sparat värde
        }
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, s));
    }

    /** Låter användaren välja typsnitt bland de installerade. */
    private void chooseFont() {
        final String bookFont = "(Bokens typsnitt)";
        java.util.List<String> choices = new java.util.ArrayList<>();
        choices.add(bookFont);
        choices.addAll(javafx.scene.text.Font.getFamilies());

        String current = contentView.getFontFamily();
        javafx.scene.control.ChoiceDialog<String> dialog =
            new javafx.scene.control.ChoiceDialog<>(current == null ? bookFont : current, choices);
        dialog.initOwner(stage);
        styleDialog(dialog.getDialogPane());
        dialog.setTitle("Typsnitt");
        dialog.setHeaderText("Typsnitt för texten");
        dialog.setContentText("Typsnitt:");

        dialog.showAndWait().ifPresent(choice -> {
            String family = choice.equals(bookFont) ? null : choice;
            contentView.setFontFamily(family);
            settings.addFontFamily(family);
        });
    }

    // ------------------------------------------------------------ tema

    /** Inställningar → Tema: ett radioval per tema. */
    private void buildThemeMenu() {
        if (menuTheme == null) {
            return;   // menyn saknas i FXML
        }
        javafx.scene.control.ToggleGroup group = new javafx.scene.control.ToggleGroup();
        menuTheme.getItems().clear();
        for (Theme t : Theme.values()) {
            javafx.scene.control.RadioMenuItem item = new javafx.scene.control.RadioMenuItem(t.toString());
            item.setToggleGroup(group);
            item.setUserData(t);
            item.setOnAction(e -> {
                applyTheme(t);
                settings.addTheme(t.name());
            });
            menuTheme.getItems().add(item);
        }
    }

    /** Byter tema i fönstret, i texten och i NCC-listan. */
    private void applyTheme(Theme newTheme) {
        theme = newTheme;

        // main.css först, sedan temats stilark som skriver över färgvariablerna
        List<String> sheets = stage.getScene().getStylesheets();
        sheets.removeIf(s -> s.contains("/themes/"));
        if (theme.getStylesheet() != null) {
            sheets.add(resource(theme.getStylesheet()));
        }

        contentView.setTheme(theme);
        nccView.setTheme(theme);

        if (menuTheme != null) {
            for (MenuItem item : menuTheme.getItems()) {
                if (item instanceof javafx.scene.control.RadioMenuItem) {
                    ((javafx.scene.control.RadioMenuItem) item).setSelected(item.getUserData() == theme);
                }
            }
        }
    }

    /** Dialogrutor har en egen scen – ge dem samma stilark som huvudfönstret. */
    private void styleDialog(javafx.scene.control.DialogPane pane) {
        if (stage != null && stage.getScene() != null) {
            pane.getStylesheets().setAll(stage.getScene().getStylesheets());
        }
    }

    private String resource(String path) {
        return getClass().getResource(path).toExternalForm();
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
        updateSpeedLabel();
        updateButtons();
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
        boolean open = timeline != null;
        boolean ready = open && mpv.isOpen();
        boolean playing = ready && !mpv.isPaused();
        boolean hasReport = report != null && report.exists();
        boolean selected = commentTable != null && commentTable.getSelected() != null;

        btnClose.setDisable(!open);
        btnShowReport.setDisable(!hasReport);
        btnReportFolder.setDisable(!hasReport);
        btnPlay.setDisable(!ready || playing);
        btnPause.setDisable(!playing);
        btnBack.setDisable(!open);
        btnForward.setDisable(!open);
        btnSlower.setDisable(!ready || speed <= MIN_SPEED);
        btnFaster.setDisable(!ready || speed >= MAX_SPEED);
        btnNewComment.setDisable(report == null);
        btnEditComment.setDisable(report == null || !selected);
        btnDeleteComment.setDisable(report == null || !selected);
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
        styleDialog(alert.getDialogPane());
        alert.setTitle("Fel");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ------------------------------------------------------------ kommentarer

    /** Läser bokens rapport, om den finns. Skriver ingenting till disk. */
    private void loadReport() {
        report = null;
        commentTable.clear();
        try {
            report = new Report(ncc);
            commentTable.setComments(report.getComments(), null);
        }
        catch (Exception e) {
            e.printStackTrace();
            showError("Kunde inte läsa rapporten", e.getMessage());
        }
    }

    /** Enter: ny kommentar med sidan som spelas/är markerad. Stoppar uppspelningen. */
    private void createComment() {
        if (report == null) {
            return;
        }
        mpv.pause();
        String page = currentIndex >= 0 ? timeline.get(currentIndex).getPage() : null;
        if (openCommentDialog(null, page)) {
            List<Comment> comments = report.getComments();
            afterChange(comments.isEmpty() ? null : comments.get(comments.size() - 1));
        }
    }

    /** Dubbelklick: redigera kommentaren. */
    private void editComment(Comment comment) {
        if (report == null) {
            return;
        }
        mpv.pause();
        if (openCommentDialog(comment, null)) {
            // Redigeringen ersätter objektet i listan – markera det nya på samma plats
            int index = Math.min(report.getComments().size() - 1,
                                 Math.max(0, commentIndexBefore));
            afterChange(report.getComments().isEmpty() ? null : report.getComments().get(index));
        }
    }

    private int commentIndexBefore = -1;

    /**
     * Visar kommentarsformuläret (CommentView3). comment == null betyder ny.
     * @return true om kommentaren sparades
     */
    private boolean openCommentDialog(Comment comment, String page) {
        if (settings.getProofreader() == null) {
            setProofReaderName();
        }
        report.setProofreader(settings.getProofreader());
        commentIndexBefore = comment == null ? -1 : report.getComments().indexOf(comment);

        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/se/camabomedia/ekorraudio/CommentView3.fxml"));
            Stage commentStage = new Stage();
            commentStage.initModality(Modality.APPLICATION_MODAL);
            commentStage.initOwner(stage);
            commentStage.setScene(new Scene(loader.load()));
            commentStage.getScene().getStylesheets().setAll(stage.getScene().getStylesheets());
            commentStage.setTitle(comment == null ? "Ekorr – Skapa kommentar" : "Ekorr – Redigera kommentar");
            commentStage.getIcons().add(
                new Image(getClass().getResourceAsStream("/se/camabomedia/ekorraudio/icon.png")));
            commentStage.setResizable(false);

            CommentView3Controller ctrl = loader.getController();
            if (comment == null) {
                ctrl.initData(commentStage, report, ncc, page);
            }
            else {
                ctrl.initData(commentStage, report, comment, ncc);
            }
            commentStage.showAndWait();
            return ctrl.isSaved();
        }
        catch (IOException e) {
            e.printStackTrace();
            showError("Kunde inte öppna kommentarsformuläret", e.getMessage());
            return false;
        }
    }

    /** Delete: ta bort markerad kommentar. */
    private void deleteComment(Comment comment) {
        int index = report.getComments().indexOf(comment);
        report.removeComment(comment);
        if (writeReport()) {
            List<Comment> comments = report.getComments();
            afterChange(comments.isEmpty() ? null : comments.get(Math.min(index, comments.size() - 1)));
        }
        else {
            report.getComments().add(Math.max(0, index), comment);   // ångra
            commentTable.setComments(report.getComments(), comment);
        }
    }

    /** Alt+pil: ny ordning från tabellen. */
    private void reorderComments(List<Comment> newOrder) {
        Comment selected = commentTable.getSelected();
        report.setComments(new java.util.ArrayList<>(newOrder));
        if (writeReport()) {
            afterChange(selected);
        }
    }

    /** Efter sparad ändring: kopiera rapporten och visa tabellen (med nya nummer). */
    private void afterChange(Comment select) {
        copyReport();
        commentTable.setComments(report.getComments(), select);
        updateButtons();   // rapporten finns nu på disk
    }

    /** Stänger talboken: stoppar ljudet och tömmer alla vyer. */
    private void closeProject() {
        mpv.close();
        timeline = null;
        smil = null;
        ncc = null;
        report = null;
        currentIndex = -1;
        seekTarget = -1;
        contentView.clear();
        nccView.clear();
        commentTable.clear();
        updateStatus(0);
        updateButtons();
    }

    /** Öppnar rapporten i systemets webbläsare. */
    private void showReport() {
        if (report != null && report.exists()) {
            openWithSystem(report.getReportFile());
        }
    }

    /** Öppnar bokens rapportmapp (___Korrektur-rapport) i Utforskaren. */
    private void openReportFolder() {
        if (report != null && report.getReportDirectory().isDirectory()) {
            openWithSystem(report.getReportDirectory());
        }
    }

    /**
     * Öppnar en fil eller mapp med det program Windows har kopplat till den.
     * java.awt.Desktop körs i en egen tråd – anropet kan ta en stund och ska
     * inte låsa JavaFX-tråden.
     */
    private void openWithSystem(File file) {
        Thread t = new Thread(() -> {
            try {
                java.awt.Desktop.getDesktop().open(file);
            }
            catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showError("Kunde inte öppna " + file.getName(), e.getMessage()));
            }
        }, "open-with-system");
        t.setDaemon(true);
        t.start();
    }

    private boolean writeReport() {
        try {
            report.setProofreader(settings.getProofreader());
            report.createAndWriteReport();   // numrerar också om kommentarerna
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            showError("Kunde inte spara rapporten", e.getMessage());
            return false;
        }
    }

    /** Kopierar rapporten till rapportmappen i inställningarna, om en sådan är vald. */
    private void copyReport() {
        String dir = settings.getPrivateDefaultLocation();
        if (dir == null || !new File(dir).isDirectory() || !report.exists()) {
            return;
        }
        try {
            new ReportCopy(report, new File(dir)).copy();
        }
        catch (IOException e) {
            e.printStackTrace();
            showError("Kunde inte kopiera rapporten till " + dir, e.getMessage());
        }
    }

    // ------------------------------------------------------------ layout

    /** Fönstrets läge och storlek samt avdelarnas positioner från settings.json. */
    private void restoreLayout() {
        double[] b = settings.getWindowBounds();
        if (b != null && b.length == 4 && b[2] > 200 && b[3] > 150
            && !Screen.getScreensForRectangle(new Rectangle2D(b[0], b[1], b[2], b[3])).isEmpty()) {
            stage.setX(b[0]);
            stage.setY(b[1]);
            stage.setWidth(b[2]);
            stage.setHeight(b[3]);
        }
        stage.setMaximized(settings.getWindowMaximized());

        double[] d = settings.getDividerPositions();
        if (d != null && d.length == splitPane.getDividers().size()) {
            // Efter att fönstret visats, annars skriver första layouten över dem
            Platform.runLater(() -> splitPane.setDividerPositions(d));
        }
    }

    private void saveLayout() {
        double[] dividers = splitPane.getDividerPositions();
        double[] bounds = null;
        if (!stage.isMaximized() && !stage.isIconified()) {
            bounds = new double[] { stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight() };
        }
        settings.addLayout(dividers, bounds, stage.isMaximized());
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
        styleDialog(dialog.getDialogPane());
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
        styleDialog(dialog.getDialogPane());
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
    void handleMenuItemOpen(ActionEvent event) {
        selectProject();
    }

    @FXML
    void handleMenuItemClose(ActionEvent event) {
        closeProject();
    }





    @FXML
    void handleMenuItemName(ActionEvent event) {
        setProofReaderName();
    }

    @FXML
    void handleMenuItemLocations(ActionEvent event) {
        setLocations();
    }

    @FXML
    void handleMenuItemFont(ActionEvent event) {
        chooseFont();
    }

}
