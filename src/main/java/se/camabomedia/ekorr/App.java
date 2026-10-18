package se.camabomedia.ekorr;

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
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("MainView.fxml"));
        // FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("SimpleView.fxml"));
        scene = new Scene(fxmlLoader.load());
        MainViewController ctrl = fxmlLoader.getController();
        // SimpleViewController ctrl = fxmlLoader.getController();
        stage.setScene(scene);
        stage.getIcons().add(new Image(App.class.getResourceAsStream("icon.png")));
        // stage.setMaximized(true);
        ctrl.initData(stage);


        // scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
        //         @Override
        //         public void handle(KeyEvent event) {
        //             System.out.println(event.getCode());
        //         }
        //     });

        // stage.setWidth(1200);
        // stage.setHeight(750);
        stage.show();
        stage.setResizable(true);

        stage.setOnCloseRequest(event -> {
                Platform.exit();
                System.exit(0);
            });

        // ctrl.openProject(new File("C:/Users/qwfwq/programmering/ekorr/src/test/resources/CA60564_Box_PUBLISHER_fixgen/Daisy_202/ncc.html"));


        // ctrl.openProject(new File("C:/Users/qwfwq/filfixaren/workspace/leverans/CA54896_Industrial_PUBLISHER_fixgen/CA_Daisy_202_audioFullText/ncc.html"));

        // ctrl.openProject(new File("C:/Users/qwfwq/programmering/ekorr/src/test/resources/CA60564_Box_PUBLISHER_fixgen/Daisy_202/ncc.html"));

        // ctrl.setFirstElement();

    }


    public static void main( String[] args ) {
        launch();
    }
}
