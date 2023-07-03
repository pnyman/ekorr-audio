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
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

public class CommentView3Controller implements Initializable {

    @FXML // fx:id="btnCancel"
    private Button btnCancel; // Value injected by FXMLLoader

    @FXML // fx:id="btnOk"
    private Button btnOk; // Value injected by FXMLLoader

    @FXML // fx:id="listViewType"
    private ListView<String> listViewType; // Value injected by FXMLLoader

    @FXML // fx:id="textAreaSeek"
    private TextArea textAreaSeek; // Value injected by FXMLLoader

    @FXML // fx:id="textAreaRight"
    private TextArea textAreaRight; // Value injected by FXMLLoader

    @FXML // fx:id="textAreaWrong"
    private TextArea textAreaWrong; // Value injected by FXMLLoader

    @FXML // fx:id="textFieldPage"
    private TextField textFieldPage; // Value injected by FXMLLoader

    private Stage stage;
    private Report report;
    private Report.Comment comment;
    private String type = null;
    boolean editComment = false;

    private String[] commentTypes = {
        "1. Felläsning",
        "2. Omtagning",
        "3. Lucka",
        "4. Stakning",
        "5. Paus",
        "6. Extra ljud",
        "7. Bakgrundsljud",
        "8. Fritext"
    };


    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        listViewType.getItems().addAll(commentTypes);

        // This sets the initial height of the ListView:
        // listViewType.setPrefHeight(commentTypes.length * 24 + 2);

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

        // Set default type
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                listViewType.getSelectionModel().select(0);
            }
        });

    }

    /**
     * Creates a new comment
     */
    public void initData(Stage stage, Report report) {
        this.stage = stage;
        this.report = report;
        bindKeys();
    }


    /**
     * Edits a comment
     */
    public void initData(Stage stage, Report report, Report.Comment comment) {
        this.stage = stage;
        this.report = report;
        this.comment = comment;
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
        textFieldPage.setText(comment.getPage());
        textAreaSeek.setText(comment.getSeek());
        textAreaWrong.setText(comment.getWrong());
        textAreaRight.setText(comment.getRight());
    }

    private void setSelectedType(int i) {
        listViewType.getSelectionModel().select(i);
        listViewType.getFocusModel().focus(i);
    }

    private void saveAndClose() {
        if (type == null) {
            warningDialogType();
            return;
        }

        String page = textFieldPage.getText();
        String seek = textAreaSeek.getText();
        String wrong = textAreaWrong.getText();
        String right = textAreaRight.getText();

        if (seek == null || seek.strip() == "") {
            warningDialogSeek();
            return;
        }

        Thread taskThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (editComment) {
                    Report.Comment newComment = report.new Comment(page, type, seek, wrong, right);
                    report.replaceComment(comment, newComment);
                }
                else {
                    report.addComment(page, type, seek, wrong, right);
                }
                try {
                    report.createAndWriteReport();
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        Stage stage = (Stage) btnOk.getScene().getWindow();
                        stage.close();
                    }
                });
            }
        });

        taskThread.start();

        // Stage stage = (Stage) btnOk.getScene().getWindow();
        // stage.close();
    }

    @FXML
    void handleBtnCancel(ActionEvent event) {
        stage.close();
    }

    @FXML
    void handleBtnOk(ActionEvent event) {
        saveAndClose();
    }


    void warningDialogType() {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle("Kommentartyp saknas");
        alert.setHeaderText("");
        alert.setContentText("Välj typ av kommentar.");
        alert.showAndWait();
    }


    void warningDialogSeek() {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle("Söktext saknas");
        alert.setHeaderText("");
        alert.setContentText("Infoga kontext i fältet Sök");
        alert.showAndWait();
    }

}
