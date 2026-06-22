package cat.game.officecatgame;

public class ChaosEvent {
    private final String label;
    private final double x;
    private final double y;
    private final double radius;
    private final double severity;
    private double timeLeft;

    public ChaosEvent(String label, double x, double y, double radius, double severity, double durationSeconds) {
        this.label = label;
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.severity = severity;
        this.timeLeft = durationSeconds;
    }

    public void update(double deltaSeconds) {
        timeLeft = Math.max(0, timeLeft - deltaSeconds);
    }

    public boolean isActive() {
        return timeLeft > 0;
    }

    public String label() {
        return label;
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

    public double severity() {
        return severity;
    }

    public double timeLeft() {
        return timeLeft;
    }
}
