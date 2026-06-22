package cat.game.officecatgame;

import javafx.scene.paint.Color;

public class ChaosInteraction {
    private final double x;
    private final double y;
    private final String promptText;
    private final double chaosGain;
    private final double cooldownSeconds;
    private final double promptRadius;
    private final Color color;

    private long availableAtMillis;

    public ChaosInteraction(
            double x,
            double y,
            String promptText,
            double chaosGain,
            double cooldownSeconds,
            double promptRadius,
            Color color
    ) {
        this.x = x;
        this.y = y;
        this.promptText = promptText;
        this.chaosGain = chaosGain;
        this.cooldownSeconds = cooldownSeconds;
        this.promptRadius = promptRadius;
        this.color = color;
    }

    public boolean canTrigger() {
        return System.currentTimeMillis() >= availableAtMillis;
    }

    public void trigger() {
        availableAtMillis = System.currentTimeMillis() + (long) (cooldownSeconds * 1000);
    }

    public double distanceTo(double px, double py) {
        return Math.hypot(px - x, py - y);
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public String promptText() {
        return promptText;
    }

    public double chaosGain() {
        return chaosGain;
    }

    public double promptRadius() {
        return promptRadius;
    }

    public Color color() {
        return color;
    }
}
