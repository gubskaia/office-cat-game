package cat.game.officecatgame;

public class IncidentFeedEntry {
    private final String text;
    private double timeLeft;

    public IncidentFeedEntry(String text, double lifetimeSeconds) {
        this.text = text;
        this.timeLeft = lifetimeSeconds;
    }

    public void update(double deltaSeconds) {
        timeLeft = Math.max(0, timeLeft - deltaSeconds);
    }

    public boolean isAlive() {
        return timeLeft > 0;
    }

    public double alpha() {
        return Math.min(1.0, timeLeft / 3.0);
    }

    public String text() {
        return text;
    }
}
