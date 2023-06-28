package se.camabomedia.ekorr;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.FileChooser.ExtensionFilter;


public class SimpleViewController implements Initializable {

    @FXML // fx:id="menuItemOpen"
    private MenuItem menuItemOpen; // Value injected by FXMLLoader

    @FXML // fx:id="titlePane"
    private Pane titlePane;

    @FXML // fx:id="x3"
    private Font x3; // Value injected by FXMLLoader

    @FXML // fx:id="x4"
    private Color x4; // Value injected by FXMLLoader

    private Stage stage;
    private Ncc ncc;

    public String page;
    public String noteType;
    public String seek;
    public String wrong;
    public String right;


    @FXML
    void handleMenuItemOpen(ActionEvent event) {
        selectProject();
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

    }

    void initData(Stage stage) {
        this.stage = stage;
        stage.getScene().setOnKeyPressed(new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent event) {
                    switch (event.getCode()) {
                    case ENTER:
                        createNote();
                        break;
                    default:
                        break;
                    }
                }
            });
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
        titlePane.getChildren().add(new Label(ncc.getTitle()));
    }

    private void createNote() {
        final Stage commentStage = new Stage();
        commentStage.initModality(Modality.APPLICATION_MODAL);
        commentStage.initOwner(stage);
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("CommentView3.fxml"));
        CommentView3Controller commentCtrl = new CommentView3Controller();
        Scene commentScene;
        try {
            commentScene = new Scene(fxmlLoader.load());
            commentStage.setScene(commentScene);
            commentCtrl.initData(this);
            commentStage.setResizable(false);
            commentStage.showAndWait();

            System.out.println(seek);


        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
