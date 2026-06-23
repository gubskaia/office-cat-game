package cat.game.officecatgame;

public class DangerZone {
    private final double x;
    private final double y;
    private final double radius;
    private double timeLeft;

    public DangerZone(double x, double y, double radius, double durationSeconds) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.timeLeft = durationSeconds;
    }

    public void update(double deltaSeconds) {
        timeLeft = Math.max(0, timeLeft - deltaSeconds);
    }

    public boolean isActive() {
        return timeLeft > 0;
    }

    public boolean contains(double px, double py) {
        return Math.hypot(px - x, py - y) <= radius;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double radius() {
        return radius;
    }

    public double timeLeft() {
        return timeLeft;
    }
}
