package se.camabomedia.ekorr;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;


public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {

        stage.setTitle("Ekorr");
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("MainView.fxml"));
        scene = new Scene(fxmlLoader.load());
        MainViewController ctrl = fxmlLoader.getController();
        stage.setScene(scene);
        stage.setMaximized(true);
        ctrl.initData(stage);

        stage.show();
    }



    public static void main( String[] args ) {
        launch();
    }
}
