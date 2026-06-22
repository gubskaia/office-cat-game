package cat.game.officecatgame;

public class HideSpot {
    private final double x;
    private final double y;
    private final double promptRadius;
    private final String label;

    public HideSpot(double x, double y, double promptRadius, String label) {
        this.x = x;
        this.y = y;
        this.promptRadius = promptRadius;
        this.label = label;
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

    public double promptRadius() {
        return promptRadius;
    }

    public String label() {
        return label;
    }
}
