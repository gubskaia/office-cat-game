package cat.game.officecatgame;

import javafx.scene.paint.Color;

public class CatSupportSpot {
    private final String id;
    private final double x;
    private final double y;
    private final String promptText;
    private final String effectText;
    private final double cooldownSeconds;
    private final double promptRadius;
    private final Color color;

    private long availableAtMillis;

    public CatSupportSpot(
            String id,
            double x,
            double y,
            String promptText,
            String effectText,
            double cooldownSeconds,
            double promptRadius,
            Color color
    ) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.promptText = promptText;
        this.effectText = effectText;
        this.cooldownSeconds = cooldownSeconds;
        this.promptRadius = promptRadius;
        this.color = color;
    }

    public boolean canUse() {
        return System.currentTimeMillis() >= availableAtMillis;
    }

    public void use() {
        availableAtMillis = System.currentTimeMillis() + (long) (cooldownSeconds * 1000);
    }

    public void reset() {
        availableAtMillis = 0;
    }

    public double distanceTo(double px, double py) {
        return Math.hypot(px - x, py - y);
    }

    public String id() {
        return id;
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

    public String effectText() {
        return effectText;
    }

    public double promptRadius() {
        return promptRadius;
    }

    public Color color() {
        return color;
    }
}
