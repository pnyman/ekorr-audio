package se.camabomedia.ekorr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.awt.Desktop;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
import javafx.stage.Stage;


public class SimpleViewController implements Initializable {

    @FXML
    private TableView<Report.Comment> tableView;

    @FXML
    private TableColumn<Report.Comment, String> tableColumnPage;

    @FXML
    private TableColumn<Report.Comment, String> tableColumnComment;

    @FXML // fx:id="menuItemOpen"
    private MenuItem menuItemOpen; // Value injected by FXMLLoader

    @FXML // fx:id="menuItemManual"
    private MenuItem menuItemManual; // Value injected by FXMLLoader

    @FXML // fx:id="titlePane"
    private Pane titlePane;

    @FXML // fx:id="x3"
    private Font x3; // Value injected by FXMLLoader

    @FXML // fx:id="x4"
    private Color x4; // Value injected by FXMLLoader

    @FXML // fx:id="btnCreate"
    private Button btnCreate; // Value injected by FXMLLoader

    @FXML // fx:id="btnEdit"
    private Button btnEdit; // Value injected by FXMLLoader

    @FXML // fx:id="btnRemove"
    private Button btnRemove; // Value injected by FXMLLoader

    @FXML // fx:id="btnQuit"
    private Button btnQuit; // Value injected by FXMLLoader

    private Stage stage;
    private Ncc ncc;
    public Report report;
    private Report.Comment selected;

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
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                // The user has selected a row, do something with the selected item
                selected = newSelection;
            }
        });

        // Edit comment by pressing SPACE, remove by pressing Delete
        tableView.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                    switch (event.getCode()) {
                    case SPACE:
                        editComment();
                        break;
                    case DELETE:
                        removeComment();
                    default:
                        break;
                    }
            }
        });

        // Edit comment with double click
        tableView.setRowFactory(tv -> {
            TableRow<Report.Comment> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    editComment();
                }
            });
            return row;
        });

    }

    void initData(Stage stage) {
        this.stage = stage;
        stage.getScene().setOnKeyPressed(new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent event) {
                    switch (event.getCode()) {
                    case ENTER:
                        createComment();
                        break;
                    case ESCAPE:
                        stage.close();
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
            tableView.setItems(data);
        }
        catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void createComment() {
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
            ctrl.initData(commentStage, report);
            commentStage.setResizable(false);
            commentStage.showAndWait();
            setColumns();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void editComment() {
        if (selected == null) {
            return;
        }

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
            ctrl.initData(commentStage, report, selected);
            commentStage.setResizable(false);
            commentStage.showAndWait();
            setColumns();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void removeComment() {
        if (selected == null) {
            return;
        }

        Thread taskThread = new Thread(new Runnable() {
            @Override
            public void run() {
                report.removeComment(selected);
                report.createAndWriteReport();
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        setColumns();
                    }
                });
            }
        });
        taskThread.start();
    }

    void viewManual() throws FileNotFoundException, IOException {
        InputStream inputStream = getClass().getResourceAsStream("/se/camabomedia/ekorr/Manual.html");
        File tempFile;
        tempFile = File.createTempFile("Manual", ".html");

        // Write the InputStream contents to the temporary file
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }

        inputStream.close();
        Desktop.getDesktop().browse(tempFile.toURI());
    }

    @FXML
    void handleMenuItemOpen(ActionEvent event) {
        selectProject();
    }

    @FXML
    void handleMenuItemManual(ActionEvent event) {
        try {
            viewManual();
        }
        catch (IOException e) {
            System.out.println("Error " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void handleBtnCreate(ActionEvent event) {
        createComment();
    }

    @FXML
    void handleBtnEdit(ActionEvent event) {
        editComment();
    }

    @FXML
    void handleBtnRemove(ActionEvent event) {
        removeComment();
    }

    @FXML
    void handleBtnQuit(ActionEvent event) {
        stage.close();
    }

}
