package cat.game.officecatgame;

import javafx.scene.input.KeyCode;

import java.util.List;

public class PlayerCat {
    private static final double SPEED = 260.0;

    private double x;
    private double y;
    private final double width = 38;
    private final double height = 38;

    public PlayerCat(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void update(InputState input, double deltaSeconds, List<Rect> walls) {
        double dx = 0;
        double dy = 0;

        if (input.isPressed(KeyCode.A) || input.isPressed(KeyCode.LEFT)) {
            dx -= 1;
        }
        if (input.isPressed(KeyCode.D) || input.isPressed(KeyCode.RIGHT)) {
            dx += 1;
        }
        if (input.isPressed(KeyCode.W) || input.isPressed(KeyCode.UP)) {
            dy -= 1;
        }
        if (input.isPressed(KeyCode.S) || input.isPressed(KeyCode.DOWN)) {
            dy += 1;
        }

        double length = Math.hypot(dx, dy);
        if (length > 0) {
            dx = (dx / length) * SPEED * deltaSeconds;
            dy = (dy / length) * SPEED * deltaSeconds;
        }

        moveX(dx, walls);
        moveY(dy, walls);

        x = clamp(x, 0, GameScreen.WIDTH - width);
        y = clamp(y, 0, GameScreen.HEIGHT - height);
    }

    private void moveX(double dx, List<Rect> walls) {
        x += dx;
        Rect bounds = bounds();
        for (Rect wall : walls) {
            if (bounds.intersects(wall)) {
                if (dx > 0) {
                    x = wall.x() - width;
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
                    y = wall.y() - height;
                } else if (dy < 0) {
                    y = wall.y() + wall.height();
                }
                bounds = bounds();
            }
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private Rect bounds() {
        return new Rect(x, y, width, height);
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double width() {
        return width;
    }

    public double height() {
        return height;
    }

    public double centerX() {
        return x + width / 2.0;
    }

    public double centerY() {
        return y + height / 2.0;
    }
}
