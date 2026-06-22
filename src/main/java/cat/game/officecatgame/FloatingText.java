package cat.game.officecatgame;

import javafx.scene.paint.Color;

public class FloatingText {
    private final String text;
    private final Color color;
    private double x;
    private double y;
    private double timeLeft;

    public FloatingText(String text, double x, double y, Color color, double lifetimeSeconds) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.color = color;
        this.timeLeft = lifetimeSeconds;
    }

    public void update(double deltaSeconds) {
        timeLeft = Math.max(0, timeLeft - deltaSeconds);
        y -= 26 * deltaSeconds;
    }

    public boolean isAlive() {
        return timeLeft > 0;
    }

    public double alpha() {
        return Math.min(1.0, timeLeft / 1.2);
    }

    public String text() {
        return text;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public Color color() {
        return color;
    }
}
