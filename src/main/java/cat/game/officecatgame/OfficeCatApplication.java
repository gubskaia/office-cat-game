package cat.game.officecatgame;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class OfficeCatApplication extends Application {
    @Override
    public void start(Stage stage) {
        GameScreen gameScreen = new GameScreen();
        Scene scene = new Scene(gameScreen, GameScreen.WIDTH, GameScreen.HEIGHT);

        gameScreen.bindInput(scene);

        stage.setTitle("Office Cat: Chaos Manager");
        stage.setMinWidth(GameScreen.WIDTH);
        stage.setMinHeight(GameScreen.HEIGHT);
        stage.setScene(scene);
        stage.show();

        gameScreen.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
