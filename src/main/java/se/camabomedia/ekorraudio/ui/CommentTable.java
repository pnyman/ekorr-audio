package se.camabomedia.ekorraudio.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import se.camabomedia.ekorraudio.report.Comment;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyEvent;

/**
 * Rapportens kommentarer som tabell: Nr, Sida, Typ, Söktext.
 *
 *   Dubbelklick      redigera
 *   Delete           ta bort
 *   Alt+Upp/Ned      flytta kommentaren uppåt/nedåt
 */
public class CommentTable {

    private final TableView<Comment> table = new TableView<>();

    private Consumer<Comment> onEdit = c -> {};
    private Consumer<Comment> onDelete = c -> {};
    private Consumer<List<Comment>> onReordered = list -> {};

    public CommentTable() {
        TableColumn<Comment, String> nr = column("Nr", 40, Comment::getNr);
        TableColumn<Comment, String> page = column("Sida", 50, Comment::getPage);
        TableColumn<Comment, String> type = column("Typ", 110, Comment::getType);
        TableColumn<Comment, String> seek = column("Söktext", 200, Comment::getSeek);

        // Nr och Sida har fast bredd. Utan minsta bredd trycker
        // CONSTRAINED_RESIZE_POLICY ihop dem till 10 px när tabellen skapas
        // (då är den 0 px bred), och då syns varken rubrik eller innehåll.
        fixedWidth(nr, 40);
        fixedWidth(page, 50);
        // Siffror högerjusteras (rubrikerna behåller standardjusteringen)
        nr.setStyle("-fx-alignment: CENTER-RIGHT;");
        page.setStyle("-fx-alignment: CENTER-RIGHT; -fx-padding: 0 8 0 0;");
        type.setMinWidth(80);
        seek.setMinWidth(80);

        table.getColumns().add(nr);
        table.getColumns().add(page);
        table.getColumns().add(type);
        table.getColumns().add(seek);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Inga kommentarer"));

        table.setRowFactory(tv -> {
            TableRow<Comment> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    onEdit.accept(row.getItem());
                }
            });
            return row;
        });

        table.addEventHandler(KeyEvent.KEY_PRESSED, this::onKeyPressed);
    }

    private static TableColumn<Comment, String> column(String title, double width,
                                                       java.util.function.Function<Comment, String> value) {
        TableColumn<Comment, String> c = new TableColumn<>(title);
        c.setPrefWidth(width);
        c.setSortable(false);   // ordningen bestäms av användaren (Alt+pil)
        c.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        return c;
    }

    private static void fixedWidth(TableColumn<Comment, String> column, double width) {
        column.setMinWidth(width);
        column.setPrefWidth(width);
        column.setMaxWidth(width);
        column.setResizable(false);
    }

    private void onKeyPressed(KeyEvent event) {
        Comment selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        if (event.isAltDown()) {
            switch (event.getCode()) {
            case UP:
                move(selected, -1);
                event.consume();
                break;
            case DOWN:
                move(selected, +1);
                event.consume();
                break;
            default:
                break;
            }
        }
        else {
            switch (event.getCode()) {
            case DELETE:
                onDelete.accept(selected);
                event.consume();
                break;
            default:
                break;
            }
        }
    }

    private void move(Comment comment, int direction) {
        ObservableList<Comment> items = table.getItems();
        int from = items.indexOf(comment);
        int to = from + direction;
        if (from < 0 || to < 0 || to >= items.size()) {
            return;
        }
        items.remove(from);
        items.add(to, comment);
        table.getSelectionModel().clearAndSelect(to);
        table.scrollTo(Math.max(0, to - 3));
        onReordered.accept(new ArrayList<>(items));
    }

    public TableView<Comment> getNode() {
        return table;
    }

    /** Visar kommentarerna och behåller (om möjligt) markerad rad. */
    public void setComments(List<Comment> comments, Comment select) {
        table.getItems().setAll(comments);
        table.refresh();   // numren kan ha ändrats utan att objekten bytts ut
        if (select != null && comments.contains(select)) {
            table.getSelectionModel().select(select);
            table.scrollTo(select);
        }
    }

    public void clear() {
        table.getItems().clear();
    }

    public Comment getSelected() {
        return table.getSelectionModel().getSelectedItem();
    }

    public void setOnEdit(Consumer<Comment> handler) {
        onEdit = handler;
    }

    public void setOnDelete(Consumer<Comment> handler) {
        onDelete = handler;
    }

    public void setOnReordered(Consumer<List<Comment>> handler) {
        onReordered = handler;
    }
}
