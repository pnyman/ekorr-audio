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
import java.util.Hashtable;
import java.util.ResourceBundle;

import org.apache.commons.io.FileUtils;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;


public class SimpleViewController implements Initializable {

    @FXML private TableView<Comment> tableView;
    @FXML private TableColumn<Comment, String> tableColumnNr;
    @FXML private TableColumn<Comment, String> tableColumnPage;
    @FXML private TableColumn<Comment, String> tableColumnType;
    @FXML private TableColumn<Comment, String> tableColumnAudio;
    @FXML private TableColumn<Comment, String> tableColumnComment;
    @FXML private MenuItem menuItemOpen;
    @FXML private MenuItem menuItemOpenLast;
    @FXML private MenuItem menuItemClose;
    @FXML private MenuItem menuItemQuit;
    @FXML private MenuItem menuItemCreateComment;
    @FXML private MenuItem menuItemEditComment;
    @FXML private MenuItem menuItemRemoveComment;
    @FXML private MenuItem menuItemName;
    @FXML private MenuItem menuItemTalkingBooks;
    @FXML private MenuItem menuItemReports;
    @FXML private MenuItem menuItemManual;
    @FXML private MenuItem menuItemAbout;
    @FXML private Label labelNrOfComments;
    @FXML private Button btnViewReport;
    @FXML private Button btnOpenReportDirectory;
    @FXML private Button btnCreate;
    @FXML private Button btnEdit;
    @FXML private Button btnRemove;
    @FXML private Button btnQuit;

    private Stage stage;
    private Ncc ncc;
    public Report report;
    private Comment selected;
    private ReportCopy reportCopy;

    private static final DataFormat SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");
    KeyCombination ctrlO = KeyCodeCombination.keyCombination("Ctrl+O");
    KeyCombination ctrlW = KeyCodeCombination.keyCombination("Ctrl+W");
    KeyCombination ctrlE = KeyCodeCombination.keyCombination("Ctrl+E");
    KeyCombination ctrlQ = KeyCodeCombination.keyCombination("Ctrl+Q");

    private Settings settings;


    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // Set minimum and maximum widths for columns
        tableColumnNr.setMinWidth(50);
        tableColumnNr.setMaxWidth(50);

        tableColumnPage.setMinWidth(65);
        tableColumnPage.setMaxWidth(65);

        tableColumnType.setMinWidth(100);
        tableColumnType.setMaxWidth(100);

        tableColumnAudio.setMinWidth(35);
        tableColumnAudio.setMaxWidth(35);

        tableColumnComment.setMinWidth(140);

        // Set column resize policy
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        tableView.setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        // Set the cell value factories for each column
        tableColumnNr.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getNr()));
        tableColumnPage.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getPage()));
        tableColumnType.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getType()));
        tableColumnAudio.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().hasAudio()));
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
                else if (ctrlW.match(event)) {
                    closeProject();
                }
                else if (ctrlE.match(event)) {
                    openLastProject();
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
            TableRow<Comment> row = new TableRow<>();

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
                    Comment draggedComment = tableView.getItems().remove(draggedIndex);
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


    void initData(Stage stage, Settings settings) {
        this.stage = stage;
        this.settings = settings;

        stage.setOnHiding( event -> {
                onExitApplication();
            });

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


        putTableViewPlaceholder();
    }


    private void onExitApplication() {
        if (report != null) {

            String nccFile = "";
            String title = "";
            String author = "";
            String ID = "";

            try {
                nccFile = ncc.getNccFile().toString();
            }
            catch (Exception ignore) {}

            try {
                title = ncc.getTitle();
            }
            catch (Exception ignore) {}

            try {
                author = ncc.getCreators().get(0);
            }
            catch (Exception ignore) {}

            try {
                ID = ncc.getIdentifier();
            }
            catch (Exception ignore) {}

            settings.addLastProject(nccFile, title, author, ID);

        }
    }


    private void putTableViewPlaceholder() {
        if (settings.getProofreader() == null) {
            tableView.setPlaceholder(placeHolder1());
        }
        else if (settings.getWantsDefaultLocation()
                 && settings.getDefaultLocation() == null) {
            tableView.setPlaceholder(placeHolder2());
        }
        else if (settings.getPrivateDefaultLocation() == null) {
            tableView.setPlaceholder(placeHolder3());
        }
        else if (report == null) {
            tableView.setPlaceholder(placeHolder4());
        }
        else {
            Label label = new Label("Inga kommentarer");
            label.setStyle("-fx-font-weight: normal; -fx-font-size: 32");
            tableView.setPlaceholder(label);
        }
    }


    private void selectProject() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Öppna en talbok");
        fileChooser.getExtensionFilters().addAll(new ExtensionFilter("Ncc-fil", "ncc.html"));
        if (settings.getWantsDefaultLocation()
            && settings.getDefaultLocation() != null) {
            fileChooser.setInitialDirectory(new File(settings.getDefaultLocation()));
        }
        File selectedFile = fileChooser.showOpenDialog(stage);
        openProject(selectedFile);
    }

    public void openProject(File selectedFile) {
        try {
            if (!selectedFile.exists()) {
                warning("Filen\n" + selectedFile.toString() + "\n" + "existerar inte.");
                return;
            }

            ncc = new Ncc(selectedFile);
            report = new Report(ncc);

            String proofreader = settings.getProofreader();
            if (proofreader != null) {
                report.setProofreader(proofreader);
            }

            report.createAndWriteReport();

            if (settings.getPrivateDefaultLocation() != null) {
                reportCopy = new ReportCopy(report, new File(settings.getPrivateDefaultLocation()));
            }

            makeWindowTitle();
            putTableViewPlaceholder();
            setColumns();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    // Automatic copy of report
    private void copyReport() {
        if (report == null || reportCopy == null) {
            return;
        }
        reportCopy.copy();
    }


    // Method with file chooser, if we decide that ot should be done manually
    private void XXXcopyReport() {
        if (report == null) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Kopiera rapport");
        fileChooser.getExtensionFilters().addAll(new ExtensionFilter("Html-fil", "*.html"));
        if (settings.getPrivateDefaultLocation() != null) {
            fileChooser.setInitialDirectory(new File(settings.getPrivateDefaultLocation()));
        }
        fileChooser.setInitialFileName(report.getReportFileName());
        File dest = fileChooser.showSaveDialog(stage);

        if (dest != null) {
            try {
                FileUtils.copyFile(report.getReportFile(), dest);
            } catch (IOException e) {
                System.out.println("Could not copy " + report.getReportFile() + " to " + dest);
            }
        }
    }


    private void closeProject() {
        onExitApplication();
        report = null;
        tableView.setItems(null);
        putTableViewPlaceholder();
        stage.setTitle("Ekorr");
        labelNrOfComments.setText("");
    }

    private void setDefaultLocation(boolean b) {
        if (!b) {
            settings.addWantsDefaultLocation(b);
            return;
        }

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Välj mapp för talböcker");
        File selectedDirectory = directoryChooser.showDialog(stage);
        settings.addDefaultLocation(selectedDirectory.getPath());
        settings.addWantsDefaultLocation(b);
    }

    private void setPrivateDefaultLocation() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Välj mapp för rapporter");
        File selectedDirectory = directoryChooser.showDialog(stage);
        settings.addPrivateDefaultLocation(selectedDirectory.getPath());
    }

    private void makeWindowTitle() {
        String windowTitle = "Ekorr";
        windowTitle += "      ";
        windowTitle += ncc.getIdentifier();
        windowTitle += " ";
        windowTitle += ncc.getTitle();
        // windowTitle += " ";
        // windowTitle += String.join(", ", ncc.getCreators());
        stage.setTitle(windowTitle);
    }

    private VBox placeHolder1() {
        Label label = new Label("Skriv in ditt namn");
        label.setStyle("-fx-font-weight: normal; -fx-font-size: 32");
        label.setPadding(new Insets(0, 0, 24, 0)); // top, right, bottom, left

        TextField nameField = new TextField();
        nameField.setPrefWidth(250);
        nameField.setMinWidth(Control.USE_PREF_SIZE);
        nameField.setMaxWidth(Control.USE_PREF_SIZE);
        Platform.runLater(() -> nameField.requestFocus());

        Button okBtn = new Button("OK");
        okBtn.setPrefWidth(Control.USE_COMPUTED_SIZE);
        okBtn.setMinWidth(Control.USE_PREF_SIZE);
        okBtn.setMaxWidth(Control.USE_PREF_SIZE);
        okBtn.setOnMouseClicked((click) -> {
                settings.addProofreader(nameField.getText());
                putTableViewPlaceholder();
        });

        int width = 290;
        int height = 35;

        HBox hb = new HBox();
        hb.setAlignment(Pos.CENTER);
        hb.getChildren().addAll(nameField, okBtn);
        hb.setSpacing(12);
        hb.setPrefSize(width, Control.USE_COMPUTED_SIZE);
        hb.setMinSize(width, Control.USE_PREF_SIZE);
        hb.setMaxSize(width, Control.USE_PREF_SIZE);

        Label bottomLabel = makeLabel("Läs manual");
        bottomLabel.setPadding(new Insets(32, 0, 0, 0)); // top, right, bottom, left
        bottomLabel.setOnMouseClicked((click) -> {
            try {
                viewManual();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        VBox vb = new VBox();
        vb.setAlignment(Pos.CENTER);
        vb.getChildren().addAll(label, hb, bottomLabel);

        // Default values from modena.css
        vb.setStyle("-fx-focus-color: #039ED3; -fx-faint-focus-color: #039ED322;");

        return vb;
    }

    private VBox placeHolder2() {
        Label topLabel = makeLabel("Välj mapp för talböcker");
        topLabel.setOnMouseClicked((click) -> {
                setDefaultLocation(true);
                putTableViewPlaceholder();
            });

        Label middleLabel = makeLabel("Nej tack");
        middleLabel.setOnMouseClicked((click) -> {
                setDefaultLocation(false);
                putTableViewPlaceholder();
            });

        Label bottomLabel = makeLabel("Läs manual");
        bottomLabel.setOnMouseClicked((click) -> {
            try {
                viewManual();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        VBox vb = new VBox();
        vb.setAlignment(Pos.CENTER);
        vb.getChildren().addAll(topLabel, middleLabel, bottomLabel);
        return vb;
    }

    private VBox placeHolder3() {
        Label topLabel = makeLabel("Välj mapp för rapporter");
        topLabel.setOnMouseClicked((click) -> {
                setPrivateDefaultLocation();
                putTableViewPlaceholder();
            });

        // Label middleLabel = makeLabel("Nej tack");
        // middleLabel.setOnMouseClicked((click) -> {
        //         setPrivateDefaultLocation(false);
        //         putTableViewPlaceholder();
        //     });

        Label bottomLabel = makeLabel("Läs manual");
        bottomLabel.setOnMouseClicked((click) -> {
            try {
                viewManual();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        VBox vb = new VBox();
        vb.setAlignment(Pos.CENTER);
        vb.getChildren().addAll(topLabel, bottomLabel);
        return vb;
    }

    private VBox placeHolder4() {
        Label middleLabel = null;
        Hashtable<String, String> lastProj = settings.getLastProject();

        Label topLabel = makeLabel("Öppna en talbok");
        topLabel.setOnMouseClicked((click) -> selectProject());

        if (!lastProj.isEmpty()) {
            middleLabel = makeLabel("Öppna senaste");
            middleLabel.setOnMouseClicked((click) ->
                                          openProject(new File(lastProj.get("lastProjectNcc"))));
        }

        Label bottomLabel = makeLabel("Läs manual");
        bottomLabel.setOnMouseClicked((click) -> {
            try {
                viewManual();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        VBox vb = new VBox();
        vb.setAlignment(Pos.CENTER);
        if (middleLabel != null) {
            vb.getChildren().addAll(topLabel, middleLabel, bottomLabel);
        }
        else {
            vb.getChildren().addAll(topLabel, bottomLabel);
        }
        return vb;
    }

    private Label makeLabel(String text) {
        int fontSize = 32;
        int padding = 32;
        String color = "#2E86C1";

        Label label = new Label(text);
        label.setStyle("-fx-font-weight: normal; -fx-font-size: 32");
        label.setPadding(new Insets(0, 0, padding, 0)); // top, right, bottom, left
        label.setTextFill(Color.web(color));
        label.setOnMouseEntered(event -> label.setStyle("-fx-font-weight: bold; -fx-font-size: 32"));
        label.setOnMouseExited(event -> label.setStyle("-fx-font-weight: normal; -fx-font-size: 32"));

        return label;
    }

    @FXML
    public void setColumns() {
        // Add data here
        ObservableList<Comment> data = FXCollections.observableArrayList();
        for (Comment comment : report.getComments()) {
            data.add(comment);
        }
        try {
            tableView.setItems(data);
        }
        catch (NullPointerException e) {
            e.printStackTrace();
        }
        labelNrOfComments.setText(report.getNumberOfComments());
    }

    private void createComment() {
        if (report == null) {
            warning("Ingen talbok är öppen.");
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
            commentStage.setTitle("Ekorr – Skapa kommentar");
            CommentView3Controller ctrl = fxmlLoader.getController();
            commentStage.getIcons().add(new Image(getClass().getResourceAsStream("icon.png")));
            ctrl.initData(commentStage, report, ncc);
            commentStage.setResizable(false);
            commentStage.showAndWait();
            setColumns();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        copyReport();
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
            commentStage.setTitle("Ekorr – Redigera kommentar");
            CommentView3Controller ctrl = fxmlLoader.getController();
            commentStage.getIcons().add(new Image(getClass().getResourceAsStream("icon.png")));
            ctrl.initData(commentStage, report, selected, ncc);
            commentStage.setResizable(false);
            commentStage.showAndWait();
            setColumns();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        copyReport();
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
                    copyReport();
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

    private void rearrangeOrderOfComments() {
        ObservableList<Comment> obComments = tableView.getItems();
        ArrayList<Comment> comments = new ArrayList<Comment>(obComments);

        try {
            report.setComments(comments);
            report.createAndWriteReport();
            copyReport();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void warning(String text) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("");
        alert.setHeaderText(text);
        alert.setContentText("");
        alert.showAndWait();
    }

    private void moveSelectedRowUp() {
        int selectedIndex = tableView.getSelectionModel().getSelectedIndex();
        if (selectedIndex > 0) {
            ObservableList<Comment> items = tableView.getItems();
            Comment selectedItem = items.remove(selectedIndex);
            items.add(selectedIndex - 1, selectedItem);
            tableView.getSelectionModel().clearAndSelect(selectedIndex - 1);
            rearrangeOrderOfComments();
        }
    }

    private void moveSelectedRowDown() {
        int selectedIndex = tableView.getSelectionModel().getSelectedIndex();
        ObservableList<Comment> items = tableView.getItems();
        if (selectedIndex < items.size() - 1) {
            Comment selectedItem = items.remove(selectedIndex);
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
        if (report == null) {
            warning("Ingen talbok är öppen.");
            return;
        }

        File file = report.getReportFile();
        try {
            Desktop.getDesktop().browse(file.toURI());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void openReportDirectory() {
        if (report == null) {
            warning("Ingen talbok är öppen.");
            return;
        }

        File directory = report.getReportDirectory();
        try {
            Desktop.getDesktop().open(directory);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void openLastProject() {
        Hashtable<String, String> lastProj = settings.getLastProject();
        onExitApplication();
        if (!lastProj.isEmpty()) {
            openProject(new File(lastProj.get("lastProjectNcc")));
        }
    }

    @FXML
    void handleMenuItemOpen(ActionEvent event) {
        selectProject();
    }

    @FXML
    void handleMenuItemOpenLast(ActionEvent event) {
        openLastProject();
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
    void handleMenuItemAbout(ActionEvent event) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Om Ekorr");
        alert.setHeaderText(null);
        alert.setContentText("Ekorr v. 1.1\n© 2023 Camabo Media Production");
        alert.showAndWait();
    }

    @FXML
    void handleMenuItemName(ActionEvent event) {
        tableView.setPlaceholder(placeHolder1());
    }

    @FXML
    void handleMenuItemTalkingBooks(ActionEvent event) {
        tableView.setPlaceholder(placeHolder2());
    }

    @FXML
    void handleMenuItemReports(ActionEvent event) {
        setPrivateDefaultLocation();
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
        stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

}
