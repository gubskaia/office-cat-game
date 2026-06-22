package cat.game.officecatgame;

import javafx.scene.paint.Color;

public class EmployeeNpc {
    public enum State {
        WORKING,
        DISTRACTED,
        INVESTIGATING,
        PANICKING
    }

    private static final double WALK_SPEED = 78.0;
    private static final double INSPECT_SPEED = 110.0;

    private final String name;
    private final double homeX;
    private final double homeY;
    private final Color color;

    private double x;
    private double y;
    private State state = State.WORKING;
    private String reactionText = "Focused";
    private double reactionTimer;
    private double targetX;
    private double targetY;
    private double productivity = 1.0;

    public EmployeeNpc(String name, double x, double y, Color color) {
        this.name = name;
        this.homeX = x;
        this.homeY = y;
        this.x = x;
        this.y = y;
        this.targetX = x;
        this.targetY = y;
        this.color = color;
    }

    public void reset() {
        x = homeX;
        y = homeY;
        state = State.WORKING;
        reactionText = "Focused";
        reactionTimer = 0;
        targetX = homeX;
        targetY = homeY;
        productivity = 1.0;
    }

    public void update(double deltaSeconds, PlayerCat player, ChaosEvent strongestEvent) {
        if (strongestEvent != null) {
            reactToEvent(strongestEvent);
        }

        if (canSeePlayer(player) && state != State.PANICKING) {
            state = State.PANICKING;
            reactionText = "Cat spotted!";
            reactionTimer = 2.4;
            targetX = player.x();
            targetY = player.y();
            productivity = 0.1;
        }

        if (reactionTimer > 0) {
            reactionTimer = Math.max(0, reactionTimer - deltaSeconds);
        } else if (state != State.WORKING) {
            state = State.WORKING;
            reactionText = "Back to work";
            targetX = homeX;
            targetY = homeY;
            productivity = 1.0;
        }

        if (state == State.INVESTIGATING || state == State.PANICKING) {
            moveToward(targetX, targetY, state == State.PANICKING ? INSPECT_SPEED : WALK_SPEED, deltaSeconds);
        } else if (state == State.WORKING) {
            moveToward(homeX, homeY, WALK_SPEED, deltaSeconds);
        }
    }

    private void reactToEvent(ChaosEvent event) {
        if (event.severity() >= 7.5) {
            state = State.PANICKING;
            reactionText = "What was that?!";
            reactionTimer = 3.2;
            targetX = event.x();
            targetY = event.y();
            productivity = 0.0;
        } else {
            state = State.INVESTIGATING;
            reactionText = "Checking " + event.label();
            reactionTimer = 2.2;
            targetX = event.x();
            targetY = event.y();
            productivity = Math.min(productivity, 0.35);
        }
    }

    private boolean canSeePlayer(PlayerCat player) {
        return distanceTo(player.centerX(), player.centerY()) < 85;
    }

    private void moveToward(double destinationX, double destinationY, double speed, double deltaSeconds) {
        double dx = destinationX - x;
        double dy = destinationY - y;
        double length = Math.hypot(dx, dy);
        if (length < 1.5) {
            return;
        }

        x += (dx / length) * speed * deltaSeconds;
        y += (dy / length) * speed * deltaSeconds;
    }

    public double distanceTo(double px, double py) {
        return Math.hypot(px - x, py - y);
    }

    public String name() {
        return name;
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

    public State state() {
        return state;
    }

    public String reactionText() {
        return reactionText;
    }

    public double productivity() {
        return productivity;
    }
}
