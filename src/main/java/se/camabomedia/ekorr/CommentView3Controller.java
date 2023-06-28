package se.camabomedia.ekorr;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

public class CommentView3Controller implements Initializable {

    @FXML // fx:id="btnCancel"
    private Button btnCancel; // Value injected by FXMLLoader

    @FXML // fx:id="btnOk"
    private Button btnOk; // Value injected by FXMLLoader

    @FXML // fx:id="listViewType"
    private ListView<String> listViewType; // Value injected by FXMLLoader

    @FXML // fx:id="textAreaRight"
    private TextArea textAreaRight; // Value injected by FXMLLoader

    @FXML // fx:id="textAreaSeek"
    private TextArea textAreaSeek; // Value injected by FXMLLoader

    @FXML // fx:id="textAreaWrong"
    private TextArea textAreaWrong; // Value injected by FXMLLoader

    @FXML // fx:id="textFieldPage"
    private TextField textFieldPage; // Value injected by FXMLLoader

    private Report report;
    private String type;

    private String[] commentTypes = {
        "1. Felläsning",
        "2. Omtagning",
        "3. Utelämning",
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
            }
        });

    }

    public void initData(Report report) {
        this.report = report;
    }

    private void setSelectedType(int i) {
        listViewType.getSelectionModel().select(i);
        listViewType.getFocusModel().focus(i);
    }

    @FXML
    void handleBtnCancel(ActionEvent event) {

    }

    @FXML
    void handleBtnOk(ActionEvent event) {
        String page = textFieldPage.getText();
        String seek = textAreaSeek.getText();
        String wrong = textAreaWrong.getText();
        String right = textAreaRight.getText();

        Thread taskThread = new Thread(new Runnable() {
            @Override
            public void run() {
                report.addRemark(page, type, seek, wrong, right);
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
    }

}
