package cat.game.officecatgame;

import javafx.scene.input.KeyCode;

import java.util.EnumSet;
import java.util.Set;

public class InputState {
    private final Set<KeyCode> pressedKeys = EnumSet.noneOf(KeyCode.class);

    public void setPressed(KeyCode keyCode, boolean pressed) {
        if (pressed) {
            pressedKeys.add(keyCode);
        } else {
            pressedKeys.remove(keyCode);
        }
    }

    public boolean isPressed(KeyCode keyCode) {
        return pressedKeys.contains(keyCode);
    }
}
