package se.camabomedia.ekorr;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.FileChooser.ExtensionFilter;


public class SimpleViewController implements Initializable {

    @FXML
    private TableView<Report.Comment> tableComments;

    @FXML
    private TableColumn<Report.Comment, String> tableColumnPage;

    @FXML
    private TableColumn<Report.Comment, String> tableColumnComment;

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
    public Report report;
    private Report.Comment selectedComment;

    @FXML
    void handleMenuItemOpen(ActionEvent event) {
        selectProject();
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // Set the cell value factories for each column
        tableColumnPage.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getPage()));
        tableColumnComment.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getType() + " " +
                                                                                     cellData.getValue().getWrong() + " " +
                                                                                     cellData.getValue().getRight()));

        // Register a listener on the selection model property
        tableComments.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                // The user has selected a row, do something with the selected item
                selectedComment = newSelection;
            }
        });

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
        report = new Report(ncc);
        // titlePane.getChildren().add(new Label(ncc.getTitle()));
        makeWindowTitle();
        setColumns();
    }

    private void makeWindowTitle() {
        String windowTitle = "";
        windowTitle += stage.getTitle();
        windowTitle += "      ";
        windowTitle += ncc.getIdentifier();
        windowTitle += " ";
        windowTitle += ncc.getTitle();
        // windowTitle += " ";
        // windowTitle += String.join(", ", ncc.getCreators());
        stage.setTitle(windowTitle);
    }

    @FXML
    public void setColumns() {
        // Add data here
        ObservableList<Report.Comment> data = FXCollections.observableArrayList();
        for (Report.Comment comment : report.getComments()) {
            data.add(comment);
        }
        try {
            tableComments.setItems(data);
        }
        catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void createNote() {
        final Stage commentStage = new Stage();
        commentStage.initModality(Modality.APPLICATION_MODAL);
        commentStage.initOwner(stage);
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("CommentView3.fxml"));
        Scene commentScene;

        try {
            commentScene = new Scene(fxmlLoader.load());
            commentStage.setScene(commentScene);
            commentStage.setTitle("Skapa kommentar");
            CommentView3Controller ctrl = fxmlLoader.getController();
            ctrl.initData(report);
            commentStage.setResizable(false);
            commentStage.showAndWait();
            // commentStage.show();
            setColumns();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
