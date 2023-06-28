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

    // private Stage stage;

    private String[] noteTypes = {
        "1. Felläsning",
        "2. Omtagning",
        "3. Utelämning",
        "4. Stakning",
        "5. Paus",
        "6. Extra ljud",
        "7. Bakgrundsljud",
        "8. Fritext"
    };

    public String page;
    public String noteType;
    public String seek;
    public String wrong;
    public String right;

    private SimpleViewController svc;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        listViewType.getItems().addAll(noteTypes);

        // This sets the initial height of the ListView:
        // listViewType.setPrefHeight(noteTypes.length * 24 + 2);

        listViewType.getSelectionModel().selectedItemProperty()
                .addListener(new ChangeListener<String>() {
                    @Override
                    public void changed(ObservableValue<? extends String> arg0, String arg1, String arg2) {
                        noteType = listViewType.getSelectionModel().getSelectedItem();
                    }
                });

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

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                textFieldPage.requestFocus();
            }
        });

        // textFieldPage.requestFocus();
    }

    public void initData(SimpleViewController svc) {
        this.svc = svc;
    }

    private void setSelectedType(int i) {
        listViewType.getSelectionModel().select(i);
        listViewType.getFocusModel().focus(i);

        System.out.println(noteType);
    }

    @FXML
    void handleBtnCancel(ActionEvent event) {

    }

    @FXML
    void handleBtnOk(ActionEvent event) {
        svc.page = textFieldPage.getText();
        svc.seek = textAreaSeek.getText();
        svc.wrong = textAreaWrong.getText();
        svc.right = textAreaRight.getText();

        Stage stage = (Stage) btnOk.getScene().getWindow();
        stage.close();
    }

}
