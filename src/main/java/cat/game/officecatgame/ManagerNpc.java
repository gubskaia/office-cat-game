package cat.game.officecatgame;

import javafx.scene.paint.Color;

import java.util.List;

public class ManagerNpc {
    public enum Mode {
        PATROLLING,
        INVESTIGATING,
        CHASING,
        SEARCHING,
        LOCKDOWN
    }

    private static final double PATROL_SPEED = 95.0;
    private static final double CHASE_SPEED = 145.0;
    private static final double SEARCH_SPEED = 118.0;
    private static final double SEARCH_DURATION_SECONDS = 3.4;
    private static final double WIDTH = 32.0;
    private static final double HEIGHT = 32.0;

    private final List<Point> patrolPoints;
    private int patrolIndex;
    private double x;
    private double y;
    private Mode mode = Mode.PATROLLING;
    private String statusText = "Patrolling";
    private double investigateTimer;
    private double lastX;
    private double lastY;
    private double stuckTimer;
    private double searchTimer;
    private double lastKnownTargetX;
    private double lastKnownTargetY;

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
        lastX = x;
        lastY = y;
        stuckTimer = 0;
        searchTimer = 0;
        lastKnownTargetX = x;
        lastKnownTargetY = y;
    }

    public void update(
            double deltaSeconds,
            PlayerCat player,
            Point playerTarget,
            ChaosEvent strongestEvent,
            double chaosPressure,
            boolean officeCollapseActive,
            List<Rect> walls
    ) {
        double patrolSpeed = PATROL_SPEED + chaosPressure * 14;
        double chaseSpeed = CHASE_SPEED + chaosPressure * 22;
        double sightRange = 145 + chaosPressure * 26 + (officeCollapseActive ? 22 : 0);

        if (strongestEvent != null && strongestEvent.severity() >= 5.5) {
            mode = Mode.INVESTIGATING;
            statusText = "Investigating " + strongestEvent.label();
            investigateTimer = 3.6;
            moveToward(strongestEvent.x(), strongestEvent.y(), patrolSpeed + 15, deltaSeconds, walls);
        } else if (canSeePlayer(player, sightRange)) {
            mode = Mode.CHASING;
            statusText = chaosPressure >= 1.5 ? "Full panic pursuit!" : "Chasing the cat!";
            lastKnownTargetX = playerTarget.x();
            lastKnownTargetY = playerTarget.y();
            searchTimer = SEARCH_DURATION_SECONDS;
            moveToward(playerTarget.x(), playerTarget.y(), chaseSpeed, deltaSeconds, walls);
        } else if (officeCollapseActive) {
            mode = Mode.LOCKDOWN;
            statusText = "Locking down the office";
            patrol(deltaSeconds, patrolSpeed + 24, walls);
        } else if (searchTimer > 0) {
            searchTimer = Math.max(0, searchTimer - deltaSeconds);
            mode = Mode.SEARCHING;
            statusText = "Searching last known position";
            moveToward(lastKnownTargetX, lastKnownTargetY, SEARCH_SPEED, deltaSeconds, walls);
        } else if (investigateTimer > 0) {
            investigateTimer = Math.max(0, investigateTimer - deltaSeconds);
            moveToward(currentPatrolPoint().x(), currentPatrolPoint().y(), patrolSpeed, deltaSeconds, walls);
            if (investigateTimer == 0) {
                mode = Mode.PATROLLING;
                statusText = "Patrolling";
            }
        } else {
            mode = Mode.PATROLLING;
            statusText = chaosPressure >= 1.5 ? "Alert patrol" : "Patrolling";
            patrol(deltaSeconds, patrolSpeed, walls);
        }

        updateStuckRecovery(deltaSeconds);
    }

    private void patrol(double deltaSeconds, double patrolSpeed, List<Rect> walls) {
        Point patrolPoint = currentPatrolPoint();
        double dx = patrolPoint.x() - x;
        double dy = patrolPoint.y() - y;
        if (Math.hypot(dx, dy) < 10) {
            patrolIndex = (patrolIndex + 1) % patrolPoints.size();
            patrolPoint = currentPatrolPoint();
        }
        moveToward(patrolPoint.x(), patrolPoint.y(), patrolSpeed, deltaSeconds, walls);
    }

    private Point currentPatrolPoint() {
        return patrolPoints.get(patrolIndex);
    }

    private boolean canSeePlayer(PlayerCat player, double sightRange) {
        if (player.isHidden()) {
            return false;
        }
        return distanceTo(player.centerX(), player.centerY()) < sightRange;
    }

    private void moveToward(double destinationX, double destinationY, double speed, double deltaSeconds, List<Rect> walls) {
        double dx = destinationX - x;
        double dy = destinationY - y;
        double length = Math.hypot(dx, dy);
        if (length < 1.5) {
            return;
        }

        moveX((dx / length) * speed * deltaSeconds, walls);
        moveY((dy / length) * speed * deltaSeconds, walls);
        x = clamp(x, 0, GameScreen.WORLD_WIDTH - WIDTH);
        y = clamp(y, 0, GameScreen.WORLD_HEIGHT - HEIGHT);
    }

    private void moveX(double dx, List<Rect> walls) {
        x += dx;
        Rect bounds = bounds();
        for (Rect wall : walls) {
            if (bounds.intersects(wall)) {
                if (dx > 0) {
                    x = wall.x() - WIDTH;
                } else if (dx < 0) {
                    x = wall.x() + wall.width();
                }
                bounds = bounds();
            }
        }
    }

    private void moveY(double dy, List<Rect> walls) {
        y += dy;
        Rect bounds = bounds();
        for (Rect wall : walls) {
            if (bounds.intersects(wall)) {
                if (dy > 0) {
                    y = wall.y() - HEIGHT;
                } else if (dy < 0) {
                    y = wall.y() + wall.height();
                }
                bounds = bounds();
            }
        }
    }

    private Rect bounds() {
        return new Rect(x - WIDTH / 2.0, y - HEIGHT / 2.0, WIDTH, HEIGHT);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void updateStuckRecovery(double deltaSeconds) {
        double movement = Math.hypot(x - lastX, y - lastY);
        if (movement < 1.2) {
            stuckTimer += deltaSeconds;
        } else {
            stuckTimer = 0;
        }

        if (stuckTimer >= 1.0) {
            patrolIndex = (patrolIndex + 1) % patrolPoints.size();
            Point recoveryPoint = currentPatrolPoint();
            x += Math.signum(recoveryPoint.x() - x) * 14;
            y += Math.signum(recoveryPoint.y() - y) * 10;
            stuckTimer = 0;
        }

        lastX = x;
        lastY = y;
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
