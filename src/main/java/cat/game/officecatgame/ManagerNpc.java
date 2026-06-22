package cat.game.officecatgame;

import javafx.scene.paint.Color;

import java.util.List;

public class ManagerNpc {
    public enum Mode {
        PATROLLING,
        INVESTIGATING,
        CHASING
    }

    private static final double PATROL_SPEED = 95.0;
    private static final double CHASE_SPEED = 145.0;

    private final List<Point> patrolPoints;
    private int patrolIndex;
    private double x;
    private double y;
    private Mode mode = Mode.PATROLLING;
    private String statusText = "Patrolling";
    private double investigateTimer;

    public ManagerNpc(double x, double y, List<Point> patrolPoints) {
        this.x = x;
        this.y = y;
        this.patrolPoints = patrolPoints;
    }

    public void reset(double nextX, double nextY) {
        x = nextX;
        y = nextY;
        patrolIndex = 0;
        mode = Mode.PATROLLING;
        statusText = "Patrolling";
        investigateTimer = 0;
    }

    public void update(double deltaSeconds, PlayerCat player, ChaosEvent strongestEvent) {
        if (strongestEvent != null && strongestEvent.severity() >= 5.5) {
            mode = Mode.INVESTIGATING;
            statusText = "Investigating " + strongestEvent.label();
            investigateTimer = 3.6;
            moveToward(strongestEvent.x(), strongestEvent.y(), PATROL_SPEED + 15, deltaSeconds);
        } else if (canSeePlayer(player)) {
            mode = Mode.CHASING;
            statusText = "Chasing the cat!";
            moveToward(player.x(), player.y(), CHASE_SPEED, deltaSeconds);
        } else if (investigateTimer > 0) {
            investigateTimer = Math.max(0, investigateTimer - deltaSeconds);
            moveToward(currentPatrolPoint().x(), currentPatrolPoint().y(), PATROL_SPEED, deltaSeconds);
            if (investigateTimer == 0) {
                mode = Mode.PATROLLING;
                statusText = "Patrolling";
            }
        } else {
            mode = Mode.PATROLLING;
            statusText = "Patrolling";
            patrol(deltaSeconds);
        }
    }

    private void patrol(double deltaSeconds) {
        Point patrolPoint = currentPatrolPoint();
        double dx = patrolPoint.x() - x;
        double dy = patrolPoint.y() - y;
        if (Math.hypot(dx, dy) < 10) {
            patrolIndex = (patrolIndex + 1) % patrolPoints.size();
            patrolPoint = currentPatrolPoint();
        }
        moveToward(patrolPoint.x(), patrolPoint.y(), PATROL_SPEED, deltaSeconds);
    }

    private Point currentPatrolPoint() {
        return patrolPoints.get(patrolIndex);
    }

    private boolean canSeePlayer(PlayerCat player) {
        if (player.isHidden()) {
            return false;
        }
        return distanceTo(player.centerX(), player.centerY()) < 145;
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

    public boolean catches(PlayerCat player) {
        if (player.isHidden()) {
            return false;
        }
        return distanceTo(player.centerX(), player.centerY()) < 48;
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

    public Mode mode() {
        return mode;
    }

    public String statusText() {
        return statusText;
    }

    public Color color() {
        return Color.web("#7c3aed");
    }
}
