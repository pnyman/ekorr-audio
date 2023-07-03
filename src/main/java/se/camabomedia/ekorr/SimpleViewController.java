package se.camabomedia.ekorr;

import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

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
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
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

    @FXML // fx:id="tableColumnType"
    private TableColumn<Report.Comment, String> tableColumnType; // Value injected by FXMLLoader

    @FXML
    private TableColumn<Report.Comment, String> tableColumnComment;

    @FXML // fx:id="menuItemOpen"
    private MenuItem menuItemOpen; // Value injected by FXMLLoader

    @FXML // fx:id="menuOpenRecent"
    private Menu menuOpenRecent; // Value injected by FXMLLoader

    @FXML // fx:id="menuItemQuit"
    private MenuItem menuItemQuit; // Value injected by FXMLLoader

    @FXML // fx:id="menuItemCreateComment"
    private MenuItem menuItemCreateComment; // Value injected by FXMLLoader

    @FXML // fx:id="menuItemEditComment"
    private MenuItem menuItemEditComment; // Value injected by FXMLLoader

    @FXML // fx:id="menuItemRemoveComment"
    private MenuItem menuItemRemoveComment; // Value injected by FXMLLoader

    @FXML // fx:id="menuItemManual"
    private MenuItem menuItemManual; // Value injected by FXMLLoader

    @FXML // fx:id="titlePane"
    private Pane titlePane;

    @FXML // fx:id="x3"
    private Font x3; // Value injected by FXMLLoader

    @FXML // fx:id="x4"
    private Color x4; // Value injected by FXMLLoader

    @FXML // fx:id="btnViewReport"
    private Button btnViewReport; // Value injected by FXMLLoader

    @FXML // fx:id="btnOpenFolder"
    private Button btnOpenReportDirectory; // Value injected by FXMLLoader

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
    private static final DataFormat SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");
    KeyCombination ctrlO = KeyCodeCombination.keyCombination("Ctrl+O");
    KeyCombination ctrlQ = KeyCodeCombination.keyCombination("Ctrl+Q");

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // Set the cell value factories for each column
        tableColumnPage.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getPage()));
        tableColumnType.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getType()));
        tableColumnComment.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getSeek()));

        // Register a listener on the selection model property
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                // The user has selected a row, do something with the selected item
                selected = newSelection;
            }
        });

        // Key events
        tableView.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (ctrlO.match(event)) {
                    selectProject();
                }
                else if (ctrlQ.match(event)) {
                    stage.close();
                }
                else if (event.isAltDown()) {
                    switch (event.getCode()) {
                        case UP:
                            moveSelectedRowUp();
                            event.consume();
                            break;
                        case DOWN:
                            moveSelectedRowDown();
                            event.consume();
                            break;
                        default:
                            break;
                    }
                }

                else {
                    switch (event.getCode()) {
                    case SPACE:
                        editComment();
                        break;
                    case DELETE:
                        removeComment();
                        break;
                    default:
                        break;
                    }
                }
            }
        });

        tableView.setOnKeyTyped(evt -> {
                if (evt.getCharacter().equals("?")) {
                    try {
                        viewManual();
                    }
                    catch (IOException e) {
                        System.out.println("Error " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });

        tableView.setRowFactory(tv -> {
            TableRow<Report.Comment> row = new TableRow<>();

            // Edit comment with double click
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    editComment();
                }
            });

            // Rearrange order of comment by drag and drop
            row.setOnDragDetected(event -> {
                if (! row.isEmpty()) {
                    Integer index = row.getIndex();
                    Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                    db.setDragView(row.snapshot(null, null));
                    ClipboardContent cc = new ClipboardContent();
                    cc.put(SERIALIZED_MIME_TYPE, index);
                    db.setContent(cc);
                    event.consume();
                }
            });

            row.setOnDragOver(event -> {
                Dragboard db = event.getDragboard();
                if (db.hasContent(SERIALIZED_MIME_TYPE)) {
                    if (row.getIndex() != ((Integer)db.getContent(SERIALIZED_MIME_TYPE)).intValue()) {
                        event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                        event.consume();
                    }
                }
            });

            row.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                if (db.hasContent(SERIALIZED_MIME_TYPE)) {
                    int draggedIndex = (Integer) db.getContent(SERIALIZED_MIME_TYPE);
                    Report.Comment draggedComment = tableView.getItems().remove(draggedIndex);
                    int dropIndex ;

                    if (row.isEmpty()) {
                        dropIndex = tableView.getItems().size() ;
                    }
                    else {
                        dropIndex = row.getIndex();
                    }

                    tableView.getItems().add(dropIndex, draggedComment);
                    event.setDropCompleted(true);
                    tableView.getSelectionModel().select(dropIndex);
                    event.consume();

                    rearrangeOrderOfComments();
                }
            });
            // Ends rearrange order of comments by drag and drop

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
                try {
                    report.removeComment(selected);
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
                        setColumns();
                    }
                });
            }
        });

        taskThread.start();
    }

    private void warning(String text) {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle(text);
        alert.setHeaderText("");
        alert.setContentText(text);
        alert.showAndWait();
    }

    private void rearrangeOrderOfComments() {
        ObservableList<Report.Comment> obComments = tableView.getItems();
        ArrayList<Report.Comment> comments = new ArrayList<Report.Comment>(obComments);

        try {
            report.setComments(comments);
            report.createAndWriteReport();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void moveSelectedRowUp() {
        int selectedIndex = tableView.getSelectionModel().getSelectedIndex();
        if (selectedIndex > 0) {
            ObservableList<Report.Comment> items = tableView.getItems();
            Report.Comment selectedItem = items.remove(selectedIndex);
            items.add(selectedIndex - 1, selectedItem);
            tableView.getSelectionModel().clearAndSelect(selectedIndex - 1);
            rearrangeOrderOfComments();
        }
    }

    private void moveSelectedRowDown() {
        int selectedIndex = tableView.getSelectionModel().getSelectedIndex();
        ObservableList<Report.Comment> items = tableView.getItems();
        if (selectedIndex < items.size() - 1) {
            Report.Comment selectedItem = items.remove(selectedIndex);
            items.add(selectedIndex + 1, selectedItem);
            tableView.getSelectionModel().clearAndSelect(selectedIndex + 1);
            rearrangeOrderOfComments();
        }
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

    private void viewReport() {
        File file = report.getReportFile();
        try {
            Desktop.getDesktop().browse(file.toURI());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void openReportDirectory() {
        File directory = report.getReportDirectory();
        try {
            Desktop.getDesktop().open(directory);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @FXML
    void handleMenuItemOpen(ActionEvent event) {
        selectProject();
    }

    @FXML
    void handleMenuOpenRecent(ActionEvent event) {

    }

    @FXML
    void handleMenuItemQuit(ActionEvent event) {
        stage.close();
    }

    @FXML
    void handleMenuItemCreateComment(ActionEvent event) {
        createComment();
    }

    @FXML
    void handleMenuItemEditComment(ActionEvent event) {
        editComment();
    }

    @FXML
    void handleMenuItemRemoveComment(ActionEvent event) {
        removeComment();
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
    void handleBtnViewReport(ActionEvent event) {
        viewReport();
    }

    @FXML
    void handleBtnOpenReportDirectory(ActionEvent event) {
        openReportDirectory();
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
