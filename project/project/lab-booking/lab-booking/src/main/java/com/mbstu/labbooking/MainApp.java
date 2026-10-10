package com.mbstu.labbooking;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        switchScene("login.fxml", "Lab Resource Booking - Login");
        stage.show();
    }

    /** Load any FXML from resources/fxml and set it as the current scene. */
    public static void switchScene(String fxmlFile, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MainApp.class.getResource("/fxml/" + fxmlFile));
            Scene scene = new Scene(loader.load());
            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
