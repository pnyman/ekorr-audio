package se.camabomedia.ekorr;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.ImageView;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

public class CommentView3Controller implements Initializable {

    @FXML private Button btnDeleteSound;
    @FXML private Button btnPlaySound;
    @FXML private Button btnRecordSound;
    @FXML private Button btnCancel;
    @FXML private Button btnOk;
    @FXML private ListView<String> listViewType;
    @FXML private TextArea textAreaSeek;
    @FXML private TextArea textAreaRight;
    @FXML private TextArea textAreaWrong;
    @FXML private TextField textFieldPage;
    @FXML private Label labelAudio;

    private Stage stage;
    private Report report;
    private Comment comment;
    private String type = null;
    boolean editComment = false;
    private final String ENDASH = "–";

    private String[] commentTypes = {
        "1. Läsfel",
        "2. Talsyntes hörs",
        "3. Inläsning saknas",
        "4. Omtagning",
        "5. Stakning",
        "6. Paus",
        "7. Överflödigt ljud",
        "8. Bakgrundsljud",
        "9. Synkfel",
        "0. Övrigt"
    };

    private Ncc ncc;
    private AudioRecorderPlayer audioRecorderPlayer;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // Set focus on page number field initially
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                textFieldPage.requestFocus();
                if (editComment) {
                    fillForm();
                }
            }
        });

        // Populate commment types
        listViewType.getItems().addAll(commentTypes);

        // This sets the initial height of the ListView:
        listViewType.setPrefHeight(commentTypes.length * 24);
        listViewType.setMaxHeight(Control.USE_PREF_SIZE);
        listViewType.setMinHeight(Control.USE_PREF_SIZE);

        // Handle selection of comment type
        listViewType.getSelectionModel().selectedItemProperty()
                .addListener(new ChangeListener<String>() {
                    @Override
                    public void changed(ObservableValue<? extends String> arg0, String arg1, String arg2) {
                        type = listViewType.getSelectionModel().getSelectedItem().substring(3);
                    }
                });

        // Add key events to list of comment types
        listViewType.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                switch (event.getCode()) {
                case DIGIT1:
                    setSelectedType(0);
                    break;
                case DIGIT2:
                    setSelectedType(1);
                    break;
                case DIGIT3:
                    setSelectedType(2);
                    break;
                case DIGIT4:
                    setSelectedType(3);
                    break;
                case DIGIT5:
                    setSelectedType(4);
                    break;
                case DIGIT6:
                    setSelectedType(5);
                    break;
                case DIGIT7:
                    setSelectedType(6);
                    break;
                case DIGIT8:
                    setSelectedType(7);
                    break;
                case DIGIT9:
                    setSelectedType(8);
                    break;
                case DIGIT0:
                    setSelectedType(9);
                    break;
                case ENTER:
                    saveAndClose();
                    break;
                case ESCAPE:
                    stage.close();
                    break;
                default:
                    break;
                }
            }
        });

        // Set default type
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                listViewType.getSelectionModel().select(0);
            }
        });

        // Make Tab focus next element in text areas
        ArrayList<TextArea> areas = new ArrayList<>();
        areas.add(textAreaSeek);
        areas.add(textAreaWrong);
        areas.add(textAreaRight);

        for (TextArea area : areas) {
            area.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent event) {
                    KeyCode code = event.getCode();
                    if (code == KeyCode.TAB && !event.isShiftDown() && !event.isControlDown()) {
                        event.consume();
                        Node node = (Node) event.getSource();
                        KeyEvent newEvent = new KeyEvent(event.getSource(),
                                event.getTarget(), event.getEventType(),
                                event.getCharacter(), event.getText(),
                                event.getCode(), event.isShiftDown(),
                                true, event.isAltDown(),
                                event.isMetaDown());
                        node.fireEvent(newEvent);
                    }
                    if (code == KeyCode.ENTER && !event.isShiftDown() && !event.isControlDown()) {
                        event.consume();
                        saveAndClose();
                    }
                }
            });
        }

        setButtons();
        labelAudio.setText("");
    }


    private void setButtons() {
        setButton(btnRecordSound, "images/recording_inactive.png", "Spela in ljud");
        setButton(btnPlaySound, "images/playing_inactive.png", "Spela upp ljud");
        setButton(btnDeleteSound, "images/delete.png", "Radera ljud");

        btnRecordSound.setOnMousePressed(event -> {
            if (!type.equals("Läsfel")) {
                showWarningDialog("Kan inte spela in", null, "Inspelning kan bara göras vid Läsfel.");
                return;
            }

            try {
                setButton(btnRecordSound, "images/recording_active.png", "Spela in ljud");
                audioRecorderPlayer.startRecording();
            }
            catch (AudioRecorderPlayer.NoMicrophoneException e) {
                showWarningDialog("Kan inte spela in", null, e.getMessage());
            }
        });

        btnRecordSound.setOnMouseReleased(event -> {
            setButton(btnRecordSound, "images/recording_inactive.png", "Spela in ljud");
            audioRecorderPlayer.stopRecording();
            setHasAudio();
        });

        btnPlaySound.setOnMousePressed(event -> {
            if (audioRecorderPlayer.getWavData() == null) {
                return;
            }

            setButton(btnPlaySound, "images/playing_active.png", "Spela upp ljud");
            audioRecorderPlayer.playFromBase64();
        });

        btnPlaySound.setOnMouseReleased(event -> {
            setButton(btnPlaySound, "images/playing_inactive.png", "Spela upp ljud");
            audioRecorderPlayer.stopPlaying();
        });
    }

    private void setButton(Button button, String imagePath, String tooltipText) {
        double BUTTON_HEIGHT = 40.0;
        ImageView imageView = new ImageView(this.getClass().getResource(imagePath).toExternalForm());
        imageView.setFitHeight(BUTTON_HEIGHT);
        imageView.setPreserveRatio(true);
        button.setGraphic(imageView);
        // button.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 0;");
        setTooltip(button, tooltipText);
    }

    private void setTooltip(Control control, String text) {
        if (text == null)
            return;
        Tooltip tooltip = new Tooltip(text);
        tooltip.setShowDelay(Duration.millis(1));
        control.setTooltip(tooltip);
    }

    private void setHasAudio() {
        if (audioRecorderPlayer.getWavData() != null) {
            labelAudio.setText("Har ljud");
            labelAudio.setStyle("-fx-background-color: green; -fx-text-fill: white; -fx-border-radius: 20 20 20 20; -fx-background-radius: 20 20 20 20;");
        }
        else {
            labelAudio.setText("");
            labelAudio.setStyle("-fx-background-color: none; -fx-text-fill: white; -fx-border-radius: 20 20 20 20; -fx-background-radius: 20 20 20 20;");
        }
    }

    /**
     * Creates a new comment
     */
    public void initData(Stage stage, Report report, Ncc ncc) {
        this.stage = stage;
        this.report = report;
        this.ncc = ncc;
        this.audioRecorderPlayer = new AudioRecorderPlayer(report.getReportDirectory());
        bindKeys();
    }

    /**
     * Edits a comment
     */
    public void initData(Stage stage, Report report, Comment comment, Ncc ncc) {
        this.stage = stage;
        this.report = report;
        this.comment = comment;
        this.ncc = ncc;
        this.audioRecorderPlayer = new AudioRecorderPlayer(report.getReportDirectory());
        audioRecorderPlayer.setWavData(comment.getAudioData());
        setHasAudio();
        editComment = true;
        bindKeys();
    }


    private void bindKeys() {
        stage.getScene().setOnKeyPressed(new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent event) {
                    switch (event.getCode()) {
                    case ENTER:
                        saveAndClose();
                        break;
                    case ESCAPE:
                        stage.close();
                        break;
                    default:
                        break;
                    }
                }
            });
    }


    private void fillForm() {
        type = comment.getType();

        for (int i = 0; i < commentTypes.length; i++) {
            if (commentTypes[i].contains(type)) {
                setSelectedType(i);
                break;
            }
        }

        String pageNr = comment.getPage();
            if (pageNr.strip().equals(ENDASH)) {
            textFieldPage.setText("");
        }
        else {
            textFieldPage.setText(pageNr);
        }

        textAreaSeek.setText(comment.getSeek());
        textAreaWrong.setText(comment.getWrong());
        textAreaRight.setText(comment.getRight());
    }

    private void setSelectedType(int i) {
        Platform.runLater(() -> {
            listViewType.getSelectionModel().select(i);
            listViewType.getFocusModel().focus(i);
        });
    }

    private boolean isCommentValid() {
        String page = textFieldPage.getText().strip();

        if (!checkPageNr(page)) {
            showWarningDialog("Felaktigt sidnummer", "Detta sidnummer finns inte i talboken:", page);
            textFieldPage.setText("");
            Platform.runLater(() -> textFieldPage.requestFocus());
            return false;
        }

        if (type == null) {
            showWarningDialog("Kommentartyp saknas", null, "Välj typ av kommentar.");
            return false;
        }

        String seek = textAreaSeek.getText().strip();
        if (seek == null || seek.strip().equals("")) {
            showWarningDialog("Söktext saknas", null, "Infoga text i fältet Söktext från boken.");
            return false;
        }

        String wrong = textAreaWrong.getText().strip();
        if (wrong == null || wrong.strip().equals("")) {
            showWarningDialog("Felbeskrivning saknas", null, "Fyll i Beskrivning av felet.");
            return false;
        }

        String right = textAreaRight.getText().strip();

        if (type.equals("Läsfel") && (right == null || right.equals(""))) {
            showWarningDialog("Rättelse saknas", null, "Fyll i fältet Rättelse.");
            return false;
        }

        if (!commentTypes[0].contains(type) && !(right == null || right.equals(""))) {
            showWarningDialog("Rättelse ej tillämpligt", null, "Rättelse används bara vid kommentartyp 1. Läsfel.");
            return false;
        }

        if (seek.equals(wrong)) {
            showWarningDialog("Samma text", null, "Fälten Söktext från boken och Beskrivning av felet innehåller samma text.");
            return false;
        }

        if (seek.equals(right)) {
            showWarningDialog("Samma text", null, "Fälten Söktext från boken och Rättelse innehåller samma text.");
            return false;
        }

        if (wrong.equals(right)) {
            showWarningDialog("Samma text", null, "Fälten Beskrivning av felet och Rättelse innehåller samma text.");
            return false;
        }

        return true;
    }


    private void saveAndClose() {
        if (!isCommentValid()) {
            return;
        }

        String page = textFieldPage.getText().strip();
        String seek = textAreaSeek.getText().strip();
        String wrong = textAreaWrong.getText().strip();
        String right = textAreaRight.getText().strip();

        Thread taskThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (editComment) {
                        Comment newComment = new Comment(null, page, type, seek, wrong, right, audioRecorderPlayer.getWavData());
                        report.replaceComment(comment, newComment);
                    }
                    else {
                        report.addComment(null, page, type, seek, wrong, right, audioRecorderPlayer.getWavData());
                    }
                    report.createAndWriteReport();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        Stage stage = (Stage) btnOk.getScene().getWindow();
                        // stage.close();
                        stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
                    }
                });
            }
        });

        taskThread.start();
    }

    private boolean checkPageNr(String pageNr) {
        if (pageNr.strip().equals("") || pageNr.strip().equals(ENDASH)) {
            return true;
        }
        return ncc.getPages().contains(pageNr);
    }

    private void showWarningDialog(String title, String header, String content) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    void handleBtnDeleteSound(ActionEvent event) {
        audioRecorderPlayer.deleteAudio();
        setHasAudio();
    }

    @FXML
    void handleBtnPlaySound(ActionEvent event) {

    }

    @FXML
    void handleBtnRecordSound(ActionEvent event) {

    }

    @FXML
    void handleBtnCancel(ActionEvent event) {
        stage.close();
    }

    @FXML
    void handleBtnOk(ActionEvent event) {
        saveAndClose();
    }

}
