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

import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
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
import javafx.scene.layout.VBox;
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
    @FXML private AnchorPane commentPane;
    @FXML private HBox toolbar;

    // Verktygsradens knappar – skapas i buildToolbar()
    private Button btnOpen;
    private Button btnOpenRecent;
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
    private Label volumeIcon;
    private javafx.scene.control.Slider volumeSlider;
    private Button btnNewComment;
    private Button btnEditComment;
    private Button btnDeleteComment;
    private Button btnSearch;
    private Button btnGoToPage;
    private Button btnWaveform;
    private Button btnLeftPane;
    private Button btnRightPane;

    // Ljudkurvan under verktygsraden
    private WaveformView waveformView;
    private se.camabomedia.ekorraudio.daisy.Waveform waveform;

    // Sidorna i NCC:n: tidslinjeindex (sorterat) och par-id, för markering i NCC-vyn
    private NccIndex nccHeadings = NccIndex.EMPTY;
    private NccIndex nccPages = NccIndex.EMPTY;
    private String currentHeadingParID = null;
    private String currentPageParID = null;
    @FXML private MenuItem menuItemOpen;
    @FXML private javafx.scene.control.Menu menuTheme;
    @FXML private javafx.scene.control.Menu menuRecent;
    @FXML private javafx.scene.control.Menu menuWaveform;
    @FXML private javafx.scene.control.Menu menuNavigation;
    @FXML private javafx.scene.control.CheckMenuItem menuItemShowWaveform;
    @FXML private javafx.scene.control.CheckMenuItem menuItemShowLeft;
    @FXML private javafx.scene.control.CheckMenuItem menuItemShowRight;
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
    /**
     * Föregående fras (talbok utan text): är man mer än så här långt in i
     * klippet går man till dess början, annars till klippet före.
     */
    private static final long PREVIOUS_PHRASE_GRACE_MS = 800;
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

    private static final String APP_NAME = "Ekorr+";

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

        btnOpen = IconButton.create("mdi2f-folder-open-outline", "Öppna talbok (Ctrl-O)", this::selectProject);
        btnOpenRecent = IconButton.create("mdi2h-history", "Öppna senaste bok (Ctrl+Skift+O)", this::openMostRecent);
        btnClose = IconButton.create("mdi2c-close-box-outline", "Stäng talboken (Ctrl-W)", this::closeProject);

        btnShowReport = IconButton.create("mdi2f-file-document-outline", "Visa rapporten (Ctrl+R)", this::showReport);
        btnReportFolder = IconButton.create("mdi2f-folder-text-outline", "Öppna rapportmappen (Ctrl+Skift+R)", this::openReportFolder);

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

        btnSlower = IconButton.create("mdi2m-minus-circle-outline", "Långsammare (kommatecken)",
                                      () -> changeSpeed(-SPEED_STEP));
        speedLabel = new Label();
        speedLabel.getStyleClass().add("speed-label");
        btnFaster = IconButton.create("mdi2p-plus-circle-outline", "Snabbare (punkt)",
                                      () -> changeSpeed(+SPEED_STEP));
        // Volym: en ikon som visar nivån och ett reglage
        volumeIcon = new Label();
        volumeIcon.getStyleClass().add("volume-icon");
        volumeSlider = new javafx.scene.control.Slider(0, MpvPlayer.MAX_VOLUME, 100);
        volumeSlider.getStyleClass().add("volume-slider");
        volumeSlider.setPrefWidth(90);
        volumeSlider.setFocusTraversable(false);   // piltangenterna styr uppspelningen
        volumeSlider.setTooltip(new javafx.scene.control.Tooltip("Volym (Ctrl+↑ / Ctrl+↓)"));
        volumeSlider.valueProperty().addListener((obs, old, value) -> {
                if (!updatingVolume) {
                    applyVolume((int) Math.round(value.doubleValue()), true);
                }
        });
        // Spara när man släpper reglaget (inte för varje steg under dragningen)
        volumeSlider.valueChangingProperty().addListener((obs, was, changing) -> {
            if (!changing) {
                settings.addVolume(mpv.getVolume());
            }
        });
        btnGoToPage = IconButton.create("mdi2b-book-open-page-variant", "Gå till sida (Ctrl+G)",
                                        this::goToPage);
        btnSearch = IconButton.create("mdi2m-magnify", "Sök i texten (Ctrl+F)",
                                      this::toggleSearch);
        btnWaveform = IconButton.create("mdi2w-waveform", "Visa/dölj ljudkurvan (Ctrl+K)",
                                        this::toggleWaveform);
        btnLeftPane = IconButton.create("mdi2d-dock-left", "Visa/dölj vänsterkolumnen (Ctrl+1)",
                                        () -> toggleSidePane(true));
        btnRightPane = IconButton.create("mdi2d-dock-right", "Visa/dölj högerkolumnen (Ctrl+2)",
                                         () -> toggleSidePane(false));

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
            btnOpen, btnOpenRecent, btnClose,
            separator(),
            btnShowReport, btnReportFolder,
            separator(),
            btnBack, btnPlay, btnPause, btnForward,
            separator(),
            btnSlower, speedLabel, btnFaster,
            separator(),
            volumeIcon, volumeSlider,
            separator(),
            btnNewComment, btnEditComment, btnDeleteComment,
            separator(),
            btnGoToPage, btnSearch,
            separator(),
            btnLeftPane, btnRightPane, btnWaveform);
        updateSpeedLabel();
    }

    /** Sökknappen: öppnar sökraden, eller stänger den om den redan är öppen. */
    private void toggleSearch() {
        if (contentView.isSearchVisible()) {
            contentView.hideSearch();
        }
        else {
            contentView.showSearch();
        }
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
        buildLoadingOverlay();
        contentView.setOnLoaded(() -> {
            textReady = true;
            hideLoadingWhenReady();
        });
        anchorToAllSides(contentView.webView);

        nccView = new NccView();
        nccPane.getChildren().add(nccView.getNccView());
        anchorToAllSides(nccView.webView);

        contentView.setOnSyncPointClicked(this::onContentClicked);
        nccView.setOnItemClicked(this::onNccClicked);

        // Högerkolumnen: kommentarstabellen. Kolumnerna i FXML har fast bredd
        // (min = max = 250) – lås upp dem så att avdelarna går att dra.
        commentPane.getChildren().clear();   // etiketten "Comments" från FXML
        commentTable = new CommentTable();
        commentPane.getChildren().add(commentTable.getNode());
        anchorToAllSides(commentTable.getNode());
        commentTable.setOnEdit(this::editComment);
        commentTable.setOnDelete(this::deleteComment);
        commentTable.setOnGoTo(this::goToComment);
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

        // Sparad volym
        int savedVolume = settings.getVolume();
        applyVolume(savedVolume >= 0 ? savedVolume : 100, false);

        // Ljudkurvan, längst ned – mellan kolumnerna och statusraden
        waveformView = new WaveformView();
        waveformView.setOnSeek(this::seekToTime);
        waveformView.managedProperty().bind(waveformView.visibleProperty());
        waveformView.setVisible(settings.getShowWaveform());
        waveformView.setFixed(settings.getWaveformFixed());
        buildWaveformMenu();
        buildNavigationMenu();
        VBox root = (VBox) splitPane.getParent();
        root.getChildren().add(root.getChildren().indexOf(splitPane) + 1, waveformView);

        buildThemeMenu();
        applyTheme(Theme.fromName(settings.getTheme()));
        // Tomma dokument i vyerna, så att temats bakgrund syns innan en bok öppnas
        contentView.clear();
        nccView.clear();

        restoreLayout();
        // Spara layouten när fönstret stängs. addEventHandler körs före App:s
        // setOnCloseRequest, som avslutar med System.exit.
        stage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, e -> {
                saveLayout();
                rememberBook();   // position i boken till nästa gång
            });
        buildRecentMenu();
        setUpViewMenu();

        // Filter (inte handler) så att tangenterna fungerar även när en WebView
        // eller knapp har fokus
        stage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);
        // WebView scrollar på tecknet " " (KEY_TYPED), inte på själva
        // tangenttrycket – stoppa det också, liksom släppet av våra tangenter
        // Hastighetstangenterna läses som tecken (",", ".", "-"), så att de
        // fungerar oberoende av tangentbordslayout – även minus på numeriska
        // tangentbordet
        stage.getScene().addEventFilter(KeyEvent.KEY_TYPED, e -> {
                if (timeline == null || e.isShortcutDown() || isTyping()) {
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
                if (timeline != null &&!isTyping() && isPlayerKey(e)) {
                    e.consume();
            }
        });
        stage.setOnHidden(e -> mpv.close());
    }

    /** Sant när ett textfält har fokus – då gäller inte spelartangenterna. */
    private boolean isTyping() {
        return stage.getScene().getFocusOwner() instanceof javafx.scene.control.TextInputControl;
    }

    private static void anchorToAllSides(javafx.scene.Node node) {
        AnchorPane.setTopAnchor(node, 0.0);
        AnchorPane.setRightAnchor(node, 0.0);
        AnchorPane.setBottomAnchor(node, 0.0);
        AnchorPane.setLeftAnchor(node, 0.0);
    }

    /** Fönstrets titel: bokens titel och id, eller bara programnamnet. */
    private void updateWindowTitle() {
        if (ncc == null || ncc.getTitle() == null) {
            stage.setTitle(APP_NAME);
            return;
        }
        String id = ncc.getIdentifier() != null ? " (" + ncc.getIdentifier() + ")" : "";
        stage.setTitle(APP_NAME + "     " + ncc.getTitle() + " " + id);
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
        if (!nccFile.isFile()) {
            showError("Hittar inte talboken", nccFile.getPath());
            settings.removeRecentBook(nccFile.getAbsolutePath());
            buildRecentMenu();
            return;
        }
        if (opening) {
            return;   // en bok håller redan på att öppnas
        }
        closeProject();   // sparar positionen i den bok som var öppen
        opening = true;

        // Läsningen av ncc, textfil och alla smil-filer tar tid – gör den i
        // en bakgrundstråd och visa "Öppnar …" under tiden. Fälten (ncc,
        // smil, timeline) sätts först på JavaFX-tråden när allt är klart.
        String guess = recentTitleFor(nccFile);
        javafx.concurrent.Task<OpenedBook> task = new javafx.concurrent.Task<>() {
            @Override
            protected OpenedBook call() {
                updateMessage("Öppnar " + guess);
                long t0 = System.nanoTime();
                Ncc n = new Ncc(nccFile);
                long t1 = System.nanoTime();
                // Börja ladda texten i vyn direkt – WebView arbetar på
                // JavaFX-tråden samtidigt som smil-filerna läses här
                File contentFile = n.getContentFilePath();
                boolean audioOnly = n.isAudioOnly();
                Platform.runLater(() -> {
                    contentView.openContent(contentFile, audioOnly);
                    nccView.openNcc(nccFile);
                });
                if (n.getTitle() != null && !n.getTitle().isBlank()) {
                    updateMessage("Öppnar " + n.getTitle());
                }
                Content c = new Content(n.getDirectory(), n.getContentFile());
                long t2 = System.nanoTime();
                Smil s = new Smil(n, c);
                long t3 = System.nanoTime();
                Timeline t = new Timeline(n, s);
                long t4 = System.nanoTime();
                reportMissingSyncPoints(c, t);
                long t5 = System.nanoTime();
                System.err.printf("[oppna] ncc %d ms, text %d ms, smil %d ms (%d filer), "
                                  + "tidslinje %d ms, kontroll %d ms%n",
                                  ms(t0, t1), ms(t1, t2), ms(t2, t3), n.getSmilFiles().size(),
                                  ms(t3, t4), ms(t4, t5));
                return new OpenedBook(n, s, t);
            }
        };
        task.setOnSucceeded(e -> {
            opening = false;
            finishOpening(nccFile, task.getValue());
        });
        task.setOnFailed(e -> {
            opening = false;
            hideLoading();
            Throwable error = task.getException();
            error.printStackTrace();
            showError("Kan inte öppna talboken", nccFile.getPath() + "\n\n" + error);
            updateButtons();
        });
        loadStarted = System.nanoTime();
        showLoading(task.messageProperty());
        Thread thread = new Thread(task, "open-book");
        thread.setDaemon(true);
        thread.start();
    }

    /** Det som läses in i bakgrunden när en bok öppnas. */
    private static final class OpenedBook {
        final Ncc ncc;
        final Smil smil;
        final Timeline timeline;

        OpenedBook(Ncc ncc, Smil smil, Timeline timeline) {
            this.ncc = ncc;
            this.smil = smil;
            this.timeline = timeline;
        }
    }

    /** Resten av öppnandet, på JavaFX-tråden. "Öppnar …" döljs när texten laddats. */
    private void finishOpening(File nccFile, OpenedBook book) {
        long t0 = System.nanoTime();
        ncc = book.ncc;
        smil = book.smil;
        timeline = book.timeline;
        currentIndex = -1;
        seekTarget = -1;
        updateWindowTitle();
        openWaveform();
        contentView.setMissingAudio(missingAudioIDs());
        buildNccIndex();
        loadReport();

        // Texten och NCC:n laddas redan i vyerna (startat i openProject)

        if (timeline.size() == 0) {
            hideLoading();
            showError("Kan inte spela upp", "Hittade inga synkpunkter i smil-filerna.");
            updateButtons();
            return;
        }

        // Fortsätt där man var senast i den här boken, annars från början
        long start = settings.getBookPosition(nccFile.getAbsolutePath());
        if (start <= 0 || start >= timeline.duration()) {
            start = timeline.startOf(0);
        }
        goTo(timeline.indexAt(start), false);
        updateStatus(start);
        settings.addRecentBook(nccFile.getAbsolutePath(), ncc.getTitle(), start);
        buildRecentMenu();

        try {
            if (edlFile == null) {
                edlFile = Files.createTempFile("ekorr-", ".edl");
                edlFile.toFile().deleteOnExit();
            }
            timeline.writeEdl(edlFile);
            mpv.open(edlFile, start);
        }
        catch (IOException e) {
            e.printStackTrace();
            showError("Kan inte spela upp ljudet", e.getMessage());
        }
        updateButtons();
        System.err.printf("[oppna] JavaFX-delen %d ms%n", ms(t0, System.nanoTime()));
        dataReady = true;
        hideLoadingWhenReady();
    }

    /** Tid för mätningen när en bok öppnas (tas bort när den inte behövs). */
    private long loadStarted = 0;

    private static long ms(long fromNanos, long toNanos) {
        return (toNanos - fromNanos) / 1_000_000;
    }

    /** Skriver ut synkpunkter som smil-filerna pekar på men som saknas i texten. */
    private static void reportMissingSyncPoints(Content content, Timeline timeline) {
        if (content.getDoc() == null) {
            return;
        }
        int missing = 0;
        for (int i = 0; i < timeline.size(); i++) {
            String id = timeline.get(i).getContentID();
            if (!content.hasId(id)) {
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
        seekToTime(from + deltaMs);
    }

    /** Hoppar till en tid i boken (t.ex. klick i ljudkurvan). Pausläget behålls. */
    private void seekToTime(long ms) {
        if (timeline == null) {
            return;
        }
        long target = Math.max(0, Math.min(ms, timeline.duration() - 1));

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
        // Talbok utan text: ett par är ofta ett helt avsnitt med många
        // ljudklipp, så "fras" blir nästa/föregående ljudklipp i stället
        if (level == Timeline.Level.PHRASE && ncc.isAudioOnly()) {
            long now = seekTarget >= 0 ? seekTarget : mpv.getPosition();
            long t = forward ? timeline.nextClipStart(now)
                : timeline.previousClipStart(now, PREVIOUS_PHRASE_GRACE_MS);
            if (t >= 0) {
                seekToTime(t);
            }
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
        if (keepSilentParHighlighted(ms)) {
            index = currentIndex;
        }
        if (index != currentIndex) {
            currentIndex = index;
            contentView.highlight(timeline.get(index).getContentID());
        }
        updateStatus(ms);
    }

    /**
     * Ett par utan ljud (inläsning saknas) har samma starttid som nästa par.
     * Utan det här skulle positionen genast flytta markeringen till nästa
     * par – och "föregående fras" skulle aldrig komma förbi. Markeringen får
     * stå kvar tills uppspelningen gått förbi parets starttid.
     */
    private boolean keepSilentParHighlighted(long ms) {
        if (currentIndex < 0 || currentIndex >= timeline.size()) {
            return false;
        }
        long start = timeline.startOf(currentIndex);
        boolean silent = timeline.endOf(currentIndex) == start;
        return silent && ms < start + 150;
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
        // Ctrl+F: sök i texten. F3 / Skift+F3: nästa/föregående träff.
        if (event.isShortcutDown() && event.getCode() == KeyCode.F) {
            contentView.showSearch();
            event.consume();
            return;
        }
        if (event.isShortcutDown() && event.getCode() == KeyCode.G) {
            // Efter tangenthändelsen, så att G inte hamnar i dialogens fält
            Platform.runLater(this::goToPage);
            event.consume();
            return;
        }
        if (event.isShortcutDown() && event.getCode() == KeyCode.K) {
            toggleWaveform();
            event.consume();
            return;
        }
        if (event.isShortcutDown() && !event.isShiftDown()
            && (event.getCode() == KeyCode.DIGIT1 || event.getCode() == KeyCode.NUMPAD1)) {
            toggleSidePane(true);
            event.consume();
            return;
        }
        if (event.isShortcutDown() && !event.isShiftDown()
            && (event.getCode() == KeyCode.DIGIT2 || event.getCode() == KeyCode.NUMPAD2)) {
            toggleSidePane(false);
            event.consume();
            return;
        }
        if (event.getCode() == KeyCode.F3 && contentView.isSearchVisible()) {
            contentView.searchStep(!event.isShiftDown());
            event.consume();
            return;
        }
        // I ett textfält (sökfältet) är tangenterna till för texten
        if (isTyping()) {
            // Menyernas kortkommandon ska inte heller gälla (se navAction)
            keyWhileTyping = true;
            Platform.runLater(() -> keyWhileTyping = false);
            return;
        }

        // Volym: Ctrl+↑ / Ctrl+↓ (5 % per steg)
        if (event.isShortcutDown() && !event.isShiftDown()
            && (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN)) {
            applyVolume(mpv.getVolume() + (event.getCode() == KeyCode.UP ? VOLUME_STEP : -VOLUME_STEP), true);
            event.consume();
            return;
        }

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
        case HOME:
            goToStart();
            break;
        case END:
            goToEnd();
            break;
        default:
            handled = false;
        }
        if (handled) {
            event.consume();
        }
    }

    /** Home: bokens början. */
    private void goToStart() {
        if (timeline != null && timeline.size() > 0) {
            goTo(0, true);
        }
    }

    /** End: början av bokens sista fras. */
    private void goToEnd() {
        if (timeline != null && timeline.size() > 0) {
            goTo(timeline.size() - 1, true);
        }
    }

    // ------------------------------------------------------------ menyn Navigering

    /**
     * Sant medan en tangent behandlas som hör till sökfältet. Menyernas
     * kortkommandon körs annars när fältet inte själv använder tangenten
     * (t.ex. mellanslag) – då ska de inte göra något.
     */
    private boolean keyWhileTyping = false;

    /**
     * Navigering: alla navigeringskommandon med sina kortkommandon. Tangenterna
     * hanteras i onKeyPressed (som tar hand om dem först); acceleratorerna här
     * visar kortkommandot i menyn och används bara om tangenten inte redan
     * hanterats.
     */
    private void buildNavigationMenu() {
        if (menuNavigation == null) {
            return;   // menyn saknas i FXML
        }
        menuNavigation.getItems().setAll(
            navItem("Spela / pausa", KeyCode.SPACE, false, this::togglePlayIfOpen),
            new javafx.scene.control.SeparatorMenuItem(),
            navItem("5 sekunder bakåt", KeyCode.LEFT, false, () -> skip(-SKIP_MS)),
            navItem("5 sekunder framåt", KeyCode.RIGHT, false, () -> skip(SKIP_MS)),
            new javafx.scene.control.SeparatorMenuItem(),
            navItem("Föregående fras", KeyCode.UP, false, () -> step(false, Timeline.Level.PHRASE)),
            navItem("Nästa fras", KeyCode.DOWN, false, () -> step(true, Timeline.Level.PHRASE)),
            navItem("Föregående rubrik", KeyCode.UP, true, () -> step(false, Timeline.Level.HEADING)),
            navItem("Nästa rubrik", KeyCode.DOWN, true, () -> step(true, Timeline.Level.HEADING)),
            navItem("Föregående sida", KeyCode.LEFT, true, () -> step(false, Timeline.Level.PAGE)),
            navItem("Nästa sida", KeyCode.RIGHT, true, () -> step(true, Timeline.Level.PAGE)),
            new javafx.scene.control.SeparatorMenuItem(),
            navItem("Bokens början", KeyCode.HOME, false, this::goToStart),
            navItem("Bokens slut", KeyCode.END, false, this::goToEnd),
            navItem("Gå till sida …", KeyCode.G, true, this::goToPage, true),
            navItem("Sök i texten …", KeyCode.F, true, contentView::showSearch, true),
            new javafx.scene.control.SeparatorMenuItem(),
            navItem("Gå till markerad kommentar (Ctrl+klick)", null, false, () -> {
                    if (commentTable != null) {
                        goToComment(commentTable.getSelected());
                    }
            }));
    }

    private MenuItem navItem(String text, KeyCode key, boolean shift, Runnable action) {
        return navItem(text, key, shift, action, false);
    }

    /**
     * Menyval med kortkommando. modifier = Skift i kortkommandot; med
     * shortcut = true används Ctrl i stället. key = null: inget kortkommando.
     * Valet gör inget när ingen bok är öppen.
     */
    private MenuItem navItem(String text, KeyCode key, boolean modifier, Runnable action,
                             boolean shortcut) {
        MenuItem item = new MenuItem(text);
        item.setMnemonicParsing(false);
        if (key != null) {
            item.setAccelerator(shortcut
                                ? new javafx.scene.input.KeyCodeCombination(key, javafx.scene.input.KeyCombination.SHORTCUT_DOWN)
                                : modifier
                                ? new javafx.scene.input.KeyCodeCombination(key, javafx.scene.input.KeyCombination.SHIFT_DOWN)
                                : new javafx.scene.input.KeyCodeCombination(key));
        }
        item.setOnAction(e -> {
                if (!keyWhileTyping && timeline != null) {
                    action.run();
                }
            });
        return item;
    }

    private void togglePlayIfOpen() {
        if (timeline != null && mpv.isOpen()) {
            togglePlay();
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
        case HOME:
        case END:
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

    // ------------------------------------------------------------ NCC-sida

    /** NCC-poster av ett slag (rubriker eller sidor), sorterade efter plats i tidslinjen. */
    private static final class NccIndex {
        static final NccIndex EMPTY = new NccIndex(new int[0], new String[0]);
        final int[] indices;      // index i tidslinjen, stigande
        final String[] parIDs;    // NCC-länkens id för samma post

        NccIndex(int[] indices, String[] parIDs) {
            this.indices = indices;
            this.parIDs = parIDs;
        }

        /** Sista posten som börjar på eller före index, eller null. */
        String at(int index) {
            if (index < 0 || indices.length == 0) {
                return null;
            }
            int k = java.util.Arrays.binarySearch(indices, index);
            if (k < 0) {
                k = -k - 2;
            }
            return k >= 0 ? parIDs[k] : null;
        }
    }

    /** Rubrikerna och sidnumren i NCC:n, för markeringen i NCC-vyn. */
    private void buildNccIndex() {
        nccHeadings = nccIndex(item -> item.getName().matches("h[1-6]"));
        nccPages = nccIndex(item -> item.getCssClass().startsWith("page-"));
        currentHeadingParID = null;
        currentPageParID = null;
    }

    private NccIndex nccIndex(java.util.function.Predicate<se.camabomedia.ekorraudio.daisy.NccItem> kind) {
        List<int[]> found = new java.util.ArrayList<>();   // {index, plats i NCC-listan}
        List<se.camabomedia.ekorraudio.daisy.NccItem> items = ncc.getNccItems();
        for (int k = 0; k < items.size(); k++) {
            if (!kind.test(items.get(k))) {
                continue;
            }
            int index = timeline.indexOf(smil.getParFromParID(items.get(k).getParID()));
            if (index >= 0) {
                found.add(new int[] { index, k });
            }
        }
        found.sort((a, b) -> Integer.compare(a[0], b[0]));
        int[] indices = new int[found.size()];
        String[] parIDs = new String[found.size()];
        for (int k = 0; k < found.size(); k++) {
            indices[k] = found.get(k)[0];
            parIDs[k] = items.get(found.get(k)[1]).getParID();
        }
        return new NccIndex(indices, parIDs);
    }

    /** Markerar i NCC-vyn rubriken och sidan som den aktuella meningen ligger under. */
    private void syncNccPage() {
        String heading = nccHeadings.at(currentIndex);
        String page = nccPages.at(currentIndex);
        if (!java.util.Objects.equals(heading, currentHeadingParID)
            || !java.util.Objects.equals(page, currentPageParID)) {
            currentHeadingParID = heading;
            currentPageParID = page;
            nccView.setCurrent(heading, page);
        }
    }

    /** Synkpunkterna (id i texten) för alla par som saknar ljud. */
    private List<String> missingAudioIDs() {
        List<String> ids = new java.util.ArrayList<>();
        for (int i = 0; i < timeline.size(); i++) {
            if (timeline.endOf(i) == timeline.startOf(i)) {
                ids.add(timeline.get(i).getContentID());
            }
        }
        return ids;
    }

    /** Ny ljudkurva för boken. Avkodningen sker löpande när kurvan visas. */
    private void openWaveform() {
        closeWaveform();
        waveform = new se.camabomedia.ekorraudio.daisy.Waveform(timeline, MpvPlayer.defaultMpvPath());
        // En avkodad bit kommer från en bakgrundstråd – rita om på JavaFX-tråden
        waveform.setOnChunkLoaded(() -> Platform.runLater(waveformView::redraw));
        waveformView.setSource(timeline, waveform);
    }

    private void closeWaveform() {
        if (waveform != null) {
            waveform.close();
            waveform = null;
        }
        waveformView.setSource(null, null);
    }

    private void toggleWaveform() {
        boolean show = !waveformView.isVisible();
        waveformView.setVisible(show);
        settings.addShowWaveform(show);
        updateViewMenu();
        if (show && timeline != null && currentIndex >= 0) {
            long ms = seekTarget >= 0 ? seekTarget : mpv.getPosition();
            waveformView.setPosition(ms, currentIndex);
        }
    }

    /** Inställningar → Ljudkurva: Rörlig (kurvan rör sig) eller Fast (markeringen rör sig). */
    private void buildWaveformMenu() {
        if (menuWaveform == null) {
            return;   // menyn saknas i FXML
        }
        javafx.scene.control.ToggleGroup group = new javafx.scene.control.ToggleGroup();
        javafx.scene.control.RadioMenuItem moving =
            new javafx.scene.control.RadioMenuItem("Rörlig kurva, fast markering");
        javafx.scene.control.RadioMenuItem fixed =
            new javafx.scene.control.RadioMenuItem("Fast kurva, rörlig markering");
        moving.setToggleGroup(group);
        fixed.setToggleGroup(group);
        moving.setSelected(!waveformView.isFixed());
        fixed.setSelected(waveformView.isFixed());
        moving.setOnAction(e -> setWaveformFixed(false));
        fixed.setOnAction(e -> setWaveformFixed(true));
        menuWaveform.getItems().setAll(moving, fixed);
    }

    private void setWaveformFixed(boolean fixed) {
        waveformView.setFixed(fixed);
        settings.addWaveformFixed(fixed);
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
        if (waveformView != null) {
            waveformView.setTheme(theme);
        }
        TitleBar.applyWhenShown(stage, theme);   // namnlisten ritas av Windows

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
        if (pane.getScene() != null && pane.getScene().getWindow() instanceof Stage) {
            TitleBar.applyWhenShown((Stage) pane.getScene().getWindow(), theme);
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

    // ------------------------------------------------------------ volym
    private static final int VOLUME_STEP = 5;
    /** Sant medan reglaget sätts från koden – då ska lyssnaren inte reagera. */
    private boolean updatingVolume = false;

    /**
     * Sätter volymen i mpv, reglaget och knappens ikon. fromUser: ändrad av
     * användaren – då slås ett avstängt ljud på igen, och volymen sparas
     * (under dragning i reglaget först när man släpper).
     */
    private void applyVolume(int percent, boolean fromUser) {
        int v = Math.max(0, Math.min(MpvPlayer.MAX_VOLUME, percent));
        mpv.setVolume(v);
        if (volumeSlider != null && (int) Math.round(volumeSlider.getValue()) != v) {
            updatingVolume = true;
            volumeSlider.setValue(v);
            updatingVolume = false;
        }
        if (fromUser && (volumeSlider == null || !volumeSlider.isValueChanging())) {
            settings.addVolume(v);
        }
        updateVolumeIcon();
        updateStatus(seekTarget >= 0 ? seekTarget : mpv.getPosition());
    }

    /** Ikonen framför reglaget visar ungefärlig nivå. */
    private void updateVolumeIcon() {
        if (volumeIcon == null) {
            return;
        }
        int v = mpv.getVolume();
        String icon = v == 0 ? "mdi2v-volume-off"
            : v < 40 ? "mdi2v-volume-low"
            : v < 80 ? "mdi2v-volume-medium"
            : "mdi2v-volume-high";
        volumeIcon.setGraphic(new org.kordamp.ikonli.javafx.FontIcon(icon));
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
        btnGoToPage.setDisable(!open || timeline.pageCount() == 0);
        btnSearch.setDisable(!open);
        btnWaveform.setDisable(!open);
    }

    private void updateStatus(long ms) {
        if (timeline == null || currentIndex < 0) {
            labelStatus.setText("");
            return;
        }
        // Anropas vid varje ny position (uppspelning, hopp, stegning)
        if (waveformView != null && waveformView.isVisible()) {
            waveformView.setPosition(ms, currentIndex);
        }
        syncNccPage();
        String page = timeline.get(currentIndex).getPage();
        labelStatus.setText((page != null ? "Sida " + page + "   " : "")
                            + Timeline.format(ms) + " / " + Timeline.format(timeline.duration())
                            + "   Hastighet " + speed + " %"
                            + "   Volym " + mpv.getVolume() + " %");
    }

    /**
     * Frågar efter ett sidnummer och hoppar dit. Pausläget behålls. Fältet
     * har aktuell sida ifylld (markerad), så det räcker att skriva det nya.
     */
    private void goToPage() {
        if (timeline == null || timeline.pageCount() == 0) {
            return;
        }
        String current = currentIndex >= 0 ? timeline.get(currentIndex).getPage() : null;
        javafx.scene.control.TextInputDialog dialog =
            new javafx.scene.control.TextInputDialog(current == null ? "" : current.strip());
        dialog.initOwner(stage);
        styleDialog(dialog.getDialogPane());
        dialog.setTitle("Gå till sida");
        dialog.setHeaderText(null);
        dialog.setGraphic(null);
        dialog.setContentText("Sida (" + timeline.pageLabel(0) + "–"
                              + timeline.pageLabel(timeline.pageCount() - 1) + "):");

        java.util.Optional<String> answer = dialog.showAndWait();
        if (answer.isEmpty() || answer.get().isBlank()) {
            return;
        }
        String page = answer.get().strip();
        int index = timeline.indexOfPage(page);
        if (index < 0) {
            showError("Sidan finns inte", "Hittade ingen sida " + page + " i boken.");
            return;
        }
        goTo(index, true);
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
        if (openCommentDialog(null, page, currentPosition())) {
            afterChange(savedComment);   // markera den nya, var den än hamnade
        }
    }

    /** Dubbelklick: redigera kommentaren. */
    private void editComment(Comment comment) {
        if (report == null) {
            return;
        }
        mpv.pause();
        if (openCommentDialog(comment, null, null)) {
            afterChange(savedComment);   // redigeringen ersätter objektet i listan
        }
    }

    /** Kommentaren som sparades i formuläret senast. */
    private Comment savedComment = null;
    private int commentIndexBefore = -1;

    /**
     * Var i boken man är: boktid och rubriktid, och för talböcker utan text
     * också event och rubrik med överordnade rubriker (nivå1 > nivå2 > …).
     */
    private se.camabomedia.ekorraudio.report.Position currentPosition() {
        if (timeline == null || timeline.size() == 0) {
            return null;
        }
        // Inget markerat än (uppspelningen har inte börjat): bokens början,
        // boktid 0 – kommentaren hamnar då först
        int index = Math.max(currentIndex, 0);
        long ms = currentIndex < 0 ? 0 : (seekTarget >= 0 ? seekTarget : mpv.getPosition());
        // Inom det markerade par:et (ett par utan ljud har samma start som nästa)
        ms = Math.max(timeline.startOf(index), Math.min(ms, timeline.endOf(index)));

        String event = Integer.toString(timeline.eventAt(index, ms));
        String heading = "";
        if (ncc.isAudioOnly()) {
            List<String> names = new java.util.ArrayList<>();
            List<String> crumbs = timeline.get(index).getBreadCrumbs();
            if (crumbs != null) {
                for (String parID : crumbs) {
                    se.camabomedia.ekorraudio.daisy.NccItem item = ncc.getNccItem(parID);
                    if (item != null) {
                        names.add(item.getText());
                    }
                }
            }
            heading = String.join(" > ", names);
        }
        return new se.camabomedia.ekorraudio.report.Position(
            se.camabomedia.ekorraudio.report.Position.format(ms),
            se.camabomedia.ekorraudio.report.Position.format(timeline.headingTime(index, ms)),
            event, heading);
    }

    /**
     * Visar kommentarsformuläret (CommentView). comment == null betyder ny.
     * @return true om kommentaren sparades
     */
    private boolean openCommentDialog(Comment comment, String page,
                                      se.camabomedia.ekorraudio.report.Position position) {
        if (settings.getProofreader() == null) {
            setProofReaderName();
        }
        report.setProofreader(settings.getProofreader());
        commentIndexBefore = comment == null ? -1 : report.getComments().indexOf(comment);

        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/se/camabomedia/ekorraudio/CommentView.fxml"));
            Stage commentStage = new Stage();
            commentStage.initModality(Modality.APPLICATION_MODAL);
            commentStage.initOwner(stage);
            commentStage.setScene(new Scene(loader.load()));
            commentStage.getScene().getStylesheets().setAll(stage.getScene().getStylesheets());
            TitleBar.applyWhenShown(commentStage, theme);
            commentStage.setTitle(comment == null ? "Ekorr – Skapa kommentar" : "Ekorr – Redigera kommentar");
            commentStage.getIcons().add(
                new Image(getClass().getResourceAsStream("/se/camabomedia/ekorraudio/icon.png")));
            commentStage.setResizable(false);

            CommentViewController ctrl = loader.getController();
            if (comment == null) {
                ctrl.initData(commentStage, report, ncc, page, position);
            }
            else {
                ctrl.initData(commentStage, report, comment, ncc);
            }
            commentStage.showAndWait();
            savedComment = ctrl.getSavedComment();
            return ctrl.isSaved();
        }
        catch (IOException e) {
            e.printStackTrace();
            showError("Kunde inte öppna kommentarsformuläret", e.getMessage());
            return false;
        }
    }

    /**
     * Ctrl+klick i kommentarstabellen: gå till stället i boken där
     * kommentaren gjordes (boktiden). Äldre kommentarer utan boktid: början
     * av kommentarens sida. Pausläget behålls.
     */
    private void goToComment(Comment comment) {
        if (timeline == null || comment == null) {
            return;
        }
        long ms = comment.getBookTimeMs();
        if (ms >= 0) {
            seekToTime(ms);
            return;
        }
        int index = timeline.indexOfPage(comment.getPage());
        if (index >= 0) {
            goTo(index, true);
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
        if (opening) {
            return;   // vänta tills boken som öppnas är klar
        }
        rememberBook();
        mpv.close();
        closeWaveform();
        contentView.setMissingAudio(null);
        nccHeadings = NccIndex.EMPTY;
        nccPages = NccIndex.EMPTY;
        currentHeadingParID = null;
        currentPageParID = null;
        nccView.setCurrent(null, null);
        timeline = null;
        smil = null;
        ncc = null;
        updateWindowTitle();
        report = null;
        currentIndex = -1;
        seekTarget = -1;
        contentView.clear();
        nccView.clear();
        commentTable.clear();
        updateStatus(0);
        updateButtons();
        buildRecentMenu();
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

    /** Hjälp → Manual: kopierar manualen ur jar-filen och öppnar den i webbläsaren. */
    private void viewManual() {
        try (InputStream in = getClass().getResourceAsStream("/se/camabomedia/ekorraudio/Manual.html")) {
            if (in == null) {
                showError("Hittar inte manualen", "Manual.html saknas i programmet.");
                return;
            }
            // Samma fil varje gång (skrivs över), så att inga kopior blir kvar
            Path file = Path.of(System.getProperty("java.io.tmpdir"), "Manual.html");
            Files.copy(in, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            openWithSystem(file.toFile());
        }
        catch (IOException e) {
            e.printStackTrace();
            showError("Kunde inte öppna manualen", e.getMessage());
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

        // double[] d = settings.getDividerPositions();
        // if (d != null && d.length == splitPane.getDividers().size()) {
        //     // Efter att fönstret visats, annars skriver första layouten över dem
        //     Platform.runLater(() -> splitPane.setDividerPositions(d));
        // }

        // Kolumnerna i den ordning de står i FXML (vänster, text, höger)
        leftPane = splitPane.getItems().get(0);
        rightPane = splitPane.getItems().get(splitPane.getItems().size() - 1);

        double[] d = settings.getDividerPositions();
        if (d != null && d.length == 2 && d[0] < d[1]) {
            dividers = d.clone();
        }
        else {
            double[] now = splitPane.getDividerPositions();
            if (now.length == 2) {
                dividers = now.clone();
            }
        }
        showLeft = settings.getShowLeftPane();
        showRight = settings.getShowRightPane();
        applySidePanes();


    }

    // ------------------------------------------------------------ "Öppnar …"

    /** Sant medan en bok läses in i bakgrunden. */
    private boolean opening = false;
    private VBox loadingOverlay;
    private final Label loadingLabel = new Label();

    /**
     * Ett lager över textvyn med en snurra och "Öppnar <bok>". Ligger i
     * contentPane ovanpå WebView och är dolt tills en bok öppnas.
     */
    private void buildLoadingOverlay() {
        javafx.scene.control.ProgressIndicator spinner = new javafx.scene.control.ProgressIndicator();
        spinner.setMaxSize(48, 48);
        loadingLabel.getStyleClass().add("loading-label");
        loadingLabel.setWrapText(true);

        VBox card = new VBox(12, spinner, loadingLabel);
        card.getStyleClass().add("loading-card");
        card.setAlignment(javafx.geometry.Pos.CENTER);
        card.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);
        card.setPadding(new Insets(20, 28, 20, 28));

        loadingOverlay = new VBox(card);
        loadingOverlay.getStyleClass().add("loading-overlay");
        loadingOverlay.setAlignment(javafx.geometry.Pos.CENTER);
        loadingOverlay.setVisible(false);
        contentPane.getChildren().add(loadingOverlay);
        anchorToAllSides(loadingOverlay);
    }

    /** Texten har laddats i vyn / boken är inläst – "Öppnar …" döljs när båda är klara. */
    private boolean textReady = false;
    private boolean dataReady = false;

    private void hideLoadingWhenReady() {
        if (textReady && dataReady) {
            hideLoading();
        }
    }

    private void showLoading(javafx.beans.value.ObservableValue<String> message) {
        textReady = false;
        dataReady = false;
        loadingLabel.textProperty().bind(message);
        loadingOverlay.setVisible(true);
        loadingOverlay.toFront();
    }

    private void hideLoading() {
        if (loadingOverlay == null) {
            return;
        }
        if (loadStarted != 0) {
            System.err.printf("[oppna] allt klart efter %d ms%n", ms(loadStarted, System.nanoTime()));
            loadStarted = 0;
        }
        loadingOverlay.setVisible(false);
        loadingLabel.textProperty().unbind();
    }

    /** Titeln ur listan "Senaste" (eller mappens namn) – innan ncc:n är läst. */
    private String recentTitleFor(File nccFile) {
        for (Settings.RecentBook book : settings.getRecentBooks()) {
            if (book.ncc.equalsIgnoreCase(nccFile.getAbsolutePath())) {
                return recentTitle(book);
            }
        }
        File dir = nccFile.getAbsoluteFile().getParentFile();
        return dir == null ? nccFile.getName() : dir.getName();
    }

    // ------------------------------------------------------------ senaste böcker

    /** Sparar öppen bok och positionen i den överst i listan "Senaste". */
    private void rememberBook() {
        if (ncc == null || timeline == null) {
            return;
        }
        long position = seekTarget >= 0 ? seekTarget : mpv.getPosition();
        settings.addRecentBook(ncc.getNccFile().getAbsolutePath(), ncc.getTitle(), position);
        buildRecentMenu();
    }

    /**
     * Arkiv → Senaste: de senast öppnade böckerna (utom den som är öppen),
     * med position. Den översta öppnas också med Ctrl+Skift+O.
     */
    private void buildRecentMenu() {
        // Knappen i verktygsraden: senaste boken som inte är öppen
        Settings.RecentBook recent = mostRecentBook();
        if (btnOpenRecent != null) {
            btnOpenRecent.setDisable(recent == null);
            btnOpenRecent.setTooltip(new javafx.scene.control.Tooltip(
                recent == null ? "Öppna senaste bok (Ctrl+Skift+O)"
                               : "Öppna " + recentTitle(recent) + " (Ctrl+Skift+O)"));
        }
        if (menuRecent == null) {
            return;
        }
        menuRecent.getItems().clear();
        String open = ncc == null ? null : ncc.getNccFile().getAbsolutePath();
        for (Settings.RecentBook book : settings.getRecentBooks()) {
            if (book.ncc.equalsIgnoreCase(open)) {
                continue;
            }
            File file = new File(book.ncc);
            MenuItem item = new MenuItem(recentTitle(book) + "  –  " + Timeline.format(book.positionMs));
            item.setMnemonicParsing(false);
            item.setOnAction(e -> openProject(file));
            if (menuRecent.getItems().isEmpty()) {
                item.setAccelerator(javafx.scene.input.KeyCombination.keyCombination("Shortcut+Shift+O"));
            }
            menuRecent.getItems().add(item);
        }
        if (menuRecent.getItems().isEmpty()) {
            MenuItem none = new MenuItem("(inga)");
            none.setDisable(true);
            menuRecent.getItems().add(none);
        }
    }

    /** Senast använda bok som inte är den öppna, eller null. */
    private Settings.RecentBook mostRecentBook() {
        String open = ncc == null ? null : ncc.getNccFile().getAbsolutePath();
        for (Settings.RecentBook book : settings.getRecentBooks()) {
            if (!book.ncc.equalsIgnoreCase(open)) {
                return book;
            }
        }
        return null;
    }

    private void openMostRecent() {
        Settings.RecentBook book = mostRecentBook();
        if (book != null) {
            openProject(new File(book.ncc));
        }
    }

    /** Bokens titel, eller mappens namn om titel saknas. */
    private static String recentTitle(Settings.RecentBook book) {
        if (book.title != null && !book.title.isBlank()) {
            return book.title;
        }
        File dir = new File(book.ncc).getParentFile();
        return dir == null ? book.ncc : dir.getName();
    }

    private void saveLayout() {
        captureDividers();
        double[] dividers = this.dividers.clone();   // alltid tre kolumners läge
        // double[] dividers = splitPane.getDividerPositions();
        double[] bounds = null;
        if (!stage.isMaximized() && !stage.isIconified()) {
            bounds = new double[] { stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight() };
        }
        settings.addLayout(dividers, bounds, stage.isMaximized());
    }

    // ------------------------------------------------------------ menyn Anpassning

    /**
     * Anpassning → Visa ljudkurvan / vänsterkolumnen / högerkolumnen.
     * Kortkommandona hanteras i onKeyPressed (som tar dem först);
     * acceleratorerna visar dem i menyn.
     */
    private void setUpViewMenu() {
        if (menuItemShowWaveform != null) {
            menuItemShowWaveform.setAccelerator(new javafx.scene.input.KeyCodeCombination(
                KeyCode.K, javafx.scene.input.KeyCombination.SHORTCUT_DOWN));
            menuItemShowWaveform.setOnAction(e -> toggleWaveform());
        }
        if (menuItemShowLeft != null) {
            menuItemShowLeft.setAccelerator(new javafx.scene.input.KeyCodeCombination(
                KeyCode.DIGIT1, javafx.scene.input.KeyCombination.SHORTCUT_DOWN));
            menuItemShowLeft.setOnAction(e -> toggleSidePane(true));
        }
        if (menuItemShowRight != null) {
            menuItemShowRight.setAccelerator(new javafx.scene.input.KeyCodeCombination(
                KeyCode.DIGIT2, javafx.scene.input.KeyCombination.SHORTCUT_DOWN));
            menuItemShowRight.setOnAction(e -> toggleSidePane(false));
        }
        updateViewMenu();
    }

    /** Bockarna i menyn visar vad som syns (också efter kortkommando eller knapp). */
    private void updateViewMenu() {
        if (menuItemShowWaveform != null && waveformView != null) {
            menuItemShowWaveform.setSelected(waveformView.isVisible());
        }
        if (menuItemShowLeft != null) {
            menuItemShowLeft.setSelected(showLeft);
        }
        if (menuItemShowRight != null) {
            menuItemShowRight.setSelected(showRight);
        }
    }

    // ------------------------------------------------------------ kolumner

    /** Vänster- och högerkolumnen (de yttre i splitPane). */
    private Node leftPane;
    private Node rightPane;
    private boolean showLeft = true;
    private boolean showRight = true;
    /**
     * Avdelarnas lägen när alla tre kolumner visas. Sparas också när en
     * kolumn är dold, så att den kommer tillbaka med samma bredd.
     */
    private double[] dividers = { 0.15, 0.85 };

    /** Ctrl+1 / Ctrl+2 och knapparna: dölj eller visa en sidokolumn. */
    private void toggleSidePane(boolean left) {
        captureDividers();
        if (left) {
            showLeft = !showLeft;
        }
        else {
            showRight = !showRight;
        }
        settings.addShowSidePanes(showLeft, showRight);
        applySidePanes();
    }

    /**
     * Läser av avdelarna och för in dem i dividers. Med en dold kolumn finns
     * bara en avdelare – den motsvarar textens vänster- eller högerkant.
     */
    private void captureDividers() {
        double[] now = splitPane.getDividerPositions();
        if (showLeft && showRight && now.length == 2) {
            dividers = now.clone();
        }
        else if (!showLeft && showRight && now.length == 1) {
            dividers[1] = now[0];   // textens högerkant
            dividers[0] = Math.min(dividers[0], dividers[1] - 0.05);
        }
        else if (showLeft && !showRight && now.length == 1) {
            dividers[0] = now[0];   // textens vänsterkant
            dividers[1] = Math.max(dividers[1], dividers[0] + 0.05);
        }
    }

    /** Lägger in de kolumner som ska synas och sätter avdelarna. */
    private void applySidePanes() {
        javafx.collections.ObservableList<Node> items = splitPane.getItems();
        items.setAll(contentPane);
        if (showLeft) {
            items.add(0, leftPane);
        }
        if (showRight) {
            items.add(rightPane);
        }
        updateViewMenu();
        // Efter layouten, annars skriver SplitPane över lägena
        Platform.runLater(() -> {
            if (showLeft && showRight) {
                splitPane.setDividerPositions(dividers[0], dividers[1]);
            }
            else if (showLeft) {
                splitPane.setDividerPositions(dividers[0]);
            }
            else if (showRight) {
                splitPane.setDividerPositions(dividers[1]);
            }
        });
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
    void handleMenuItemQuit(ActionEvent event) {
        stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    @FXML
    void handleMenuItemShowReport(ActionEvent event) {
        showReport();
    }

    @FXML
    void handleMenuItemOpenReportFolder(ActionEvent event) {
        openReportFolder();
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

    @FXML
    void handleMenuItemManual(ActionEvent event) {
        viewManual();
    }

}
