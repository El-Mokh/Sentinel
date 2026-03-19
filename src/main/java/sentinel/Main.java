package sentinel;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import sentinel.db.DatabaseManager;

public class Main extends Application {

    @Override
    public void start(Stage stage) {

        DatabaseManager.initializeDatabase();

        Label label = new Label("Sentinel EPD");
        label.setStyle("-fx-font-size: 20px; -fx-font-family: 'Arial';");
        
        StackPane root = new StackPane(label);
        Scene scene = new Scene(root, 600, 400);

        stage.setTitle("Sentinel System");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}