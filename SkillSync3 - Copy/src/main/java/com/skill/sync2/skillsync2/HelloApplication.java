package com.skill.sync2.skillsync2;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        DatabaseManager.initialize();

        // Use the absolute path starting with a forward slash
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("/com/skill/sync2/skillsync2/hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1100, 750);

        // Update the logo path as well
        stage.getIcons().add(new javafx.scene.image.Image(
                HelloApplication.class.getResourceAsStream("/com/skill/sync2/skillsync2/logo.png")
        ));

        stage.setTitle("SkillSync Pro v1.0.5");
        stage.setMinWidth(950);
        stage.setMinHeight(650);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        try {
            com.skill.sync2.skillsync2.DatabaseManager.disconnect();
        } catch (Exception e) {
            System.out.println("Application closed safely.");
        }
    }

    public static void main(String[] args) { launch(); }
}