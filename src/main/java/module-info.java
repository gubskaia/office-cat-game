module cat.game.officecatgame {
    requires javafx.controls;
    requires javafx.fxml;


    opens cat.game.officecatgame to javafx.fxml;
    exports cat.game.officecatgame;
}