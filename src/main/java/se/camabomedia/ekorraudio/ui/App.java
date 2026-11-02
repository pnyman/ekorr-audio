package se.camabomedia.ekorraudio.ui;

import se.camabomedia.ekorraudio.Settings;

// import java.awt.Image;
import java.io.IOException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;


public class App extends Application {

    private static Scene scene;
    public Settings settings = new Settings();

    @Override
    public void start(Stage stage) throws IOException {

        stage.setTitle("Ekorr+");
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/se/camabomedia/ekorraudio/MainView.fxml"));
        scene = new Scene(fxmlLoader.load());
        MainViewController ctrl = fxmlLoader.getController();
        stage.setScene(scene);
        stage.getIcons().add(new Image(App.class.getResourceAsStream("/se/camabomedia/ekorraudio/icon.png")));
        ctrl.initData(stage, settings);

        stage.show();
        stage.setResizable(true);

        stage.setOnCloseRequest(event -> {
                Platform.exit();
                System.exit(0);
            });
    }

    public static void main( String[] args ) {
        launch();
    }
}
