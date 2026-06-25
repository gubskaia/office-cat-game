package cat.game.officecatgame;

import javafx.scene.paint.Color;

import java.util.List;

public class EmployeeNpc {
    private static final String[] INVESTIGATION_LINES = {
            "Checking the noise",
            "Who did that?",
            "Again?!",
            "This office is cursed"
    };
    private static final String[] PANIC_LINES = {
            "What was that?!",
            "Everything is breaking!",
            "Not the deadline day!",
            "This is a disaster!"
    };
    private static final String[] SPOTTED_LINES = {
            "Cat spotted!",
            "There it is!",
            "Someone grab the cat!",
            "The menace is back!"
    };

    public enum State {
        WORKING,
        DISTRACTED,
        INVESTIGATING,
        PANICKING
    }

    private static final double WALK_SPEED = 78.0;
    private static final double INSPECT_SPEED = 110.0;
    private static final double WIDTH = 28.0;
    private static final double HEIGHT = 28.0;

    private final String name;
    private final double homeX;
    private final double homeY;
    private final Color color;
    private final List<Point> routinePoints;

    private double x;
    private double y;
    private State state = State.WORKING;
    private String reactionText = "Focused";
    private double reactionTimer;
    private double targetX;
    private double targetY;
    private double productivity = 1.0;
    private double reportCooldown;
    private double wanderTimer;
    private int routineIndex;

    public EmployeeNpc(String name, double x, double y, Color color, List<Point> routinePoints) {
        this.name = name;
        this.homeX = x;
        this.homeY = y;
        this.x = x;
        this.y = y;
        this.targetX = x;
        this.targetY = y;
        this.color = color;
        this.routinePoints = routinePoints;
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
        reportCooldown = 0;
        wanderTimer = 0;
        routineIndex = 0;
    }

    public void update(double deltaSeconds, PlayerCat player, Point playerTarget, ChaosEvent strongestEvent, List<Rect> walls) {
        reportCooldown = Math.max(0, reportCooldown - deltaSeconds);
        wanderTimer = Math.max(0, wanderTimer - deltaSeconds);

        if (strongestEvent != null) {
            reactToEvent(strongestEvent);
        }

        if (canSeePlayer(player) && state != State.PANICKING) {
            state = State.PANICKING;
            reactionText = pickLine(SPOTTED_LINES, name.hashCode());
            reactionTimer = 2.4;
            targetX = playerTarget.x();
            targetY = playerTarget.y();
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
            moveToward(targetX, targetY, state == State.PANICKING ? INSPECT_SPEED : WALK_SPEED, deltaSeconds, walls);
        } else if (state == State.WORKING) {
            if (wanderTimer == 0 || distanceTo(targetX, targetY) < 12) {
                chooseRoutineTarget();
            }
            reactionText = distanceTo(homeX, homeY) < 20 ? "Focused" : "Stretching legs";
            moveToward(targetX, targetY, WALK_SPEED * 0.65, deltaSeconds, walls);
        }
    }

    private void reactToEvent(ChaosEvent event) {
        if (event.severity() >= 7.5) {
            state = State.PANICKING;
            reactionText = pickLine(PANIC_LINES, event.label().hashCode() + name.hashCode());
            reactionTimer = 3.2;
            targetX = event.x();
            targetY = event.y();
            productivity = 0.0;
        } else {
            state = State.INVESTIGATING;
            reactionText = pickLine(INVESTIGATION_LINES, event.label().hashCode() + name.hashCode());
            reactionTimer = 2.2;
            targetX = event.x();
            targetY = event.y();
            productivity = Math.min(productivity, 0.35);
        }
    }

    private boolean canSeePlayer(PlayerCat player) {
        if (player.isHidden()) {
            return false;
        }
        return distanceTo(player.centerX(), player.centerY()) < 85;
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

    private void chooseRoutineTarget() {
        if (routinePoints == null || routinePoints.isEmpty()) {
            targetX = homeX;
            targetY = homeY;
            wanderTimer = 2.2;
            return;
        }

        Point nextPoint = routinePoints.get(routineIndex % routinePoints.size());
        routineIndex = (routineIndex + 1) % routinePoints.size();
        targetX = nextPoint.x();
        targetY = nextPoint.y();
        wanderTimer = 2.8;
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

    public boolean canReportCat(PlayerCat player) {
        return state == State.PANICKING
                && !player.isHidden()
                && reportCooldown <= 0
                && distanceTo(player.centerX(), player.centerY()) < 150;
    }

    public void markReportUsed() {
        reportCooldown = 5.2;
    }

    private String pickLine(String[] variants, int seed) {
        int index = Math.floorMod(seed, variants.length);
        return variants[index];
    }
}
