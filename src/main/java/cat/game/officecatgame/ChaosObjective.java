package cat.game.officecatgame;

public class ChaosObjective {
    private final String interactionId;
    private final String title;
    private final String description;
    private final double bonusChaos;
    private boolean completed;

    public ChaosObjective(String interactionId, String title, String description, double bonusChaos) {
        this.interactionId = interactionId;
        this.title = title;
        this.description = description;
        this.bonusChaos = bonusChaos;
    }

    public boolean matches(ChaosInteraction interaction) {
        return interactionId.equals(interaction.id());
    }

    public String interactionId() {
        return interactionId;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public double bonusChaos() {
        return bonusChaos;
    }

    public boolean completed() {
        return completed;
    }

    public void complete() {
        completed = true;
    }
}
