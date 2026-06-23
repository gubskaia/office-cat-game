package cat.game.officecatgame;

import javafx.scene.input.KeyCode;

import java.util.List;

public class PlayerCat {
    private static final double SPEED = 260.0;
    private static final double DASH_MULTIPLIER = 2.7;
    private static final double DASH_DURATION_SECONDS = 0.16;
    private static final double DASH_COOLDOWN_SECONDS = 3.5;

    private double x;
    private double y;
    private final double width = 38;
    private final double height = 38;
    private boolean hidden;
    private boolean moving;
    private double dashTimeRemaining;
    private double dashCooldownRemaining;
    private double dashDirectionX;
    private double dashDirectionY = 1;
    private double facingX;
    private double facingY = 1;

    public PlayerCat(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void update(InputState input, double deltaSeconds, List<Rect> walls) {
        dashCooldownRemaining = Math.max(0, dashCooldownRemaining - deltaSeconds);

        if (hidden) {
            moving = false;
            dashTimeRemaining = 0;
            return;
        }

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
        if (dashTimeRemaining > 0) {
            dashTimeRemaining = Math.max(0, dashTimeRemaining - deltaSeconds);
            dx = dashDirectionX * SPEED * DASH_MULTIPLIER * deltaSeconds;
            dy = dashDirectionY * SPEED * DASH_MULTIPLIER * deltaSeconds;
            moving = true;
        } else {
            if (length > 0) {
                dashDirectionX = dx / length;
                dashDirectionY = dy / length;
                facingX = dashDirectionX;
                facingY = dashDirectionY;
                dx = dashDirectionX * SPEED * deltaSeconds;
                dy = dashDirectionY * SPEED * deltaSeconds;
            }
            moving = length > 0;
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

    public void setPosition(double nextX, double nextY) {
        x = nextX;
        y = nextY;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
        if (hidden) {
            moving = false;
        }
    }

    public boolean isMoving() {
        return moving;
    }

    public boolean tryStartDash(InputState input) {
        if (hidden || dashCooldownRemaining > 0 || dashTimeRemaining > 0) {
            return false;
        }

        double inputX = 0;
        double inputY = 0;
        if (input.isPressed(KeyCode.A) || input.isPressed(KeyCode.LEFT)) {
            inputX -= 1;
        }
        if (input.isPressed(KeyCode.D) || input.isPressed(KeyCode.RIGHT)) {
            inputX += 1;
        }
        if (input.isPressed(KeyCode.W) || input.isPressed(KeyCode.UP)) {
            inputY -= 1;
        }
        if (input.isPressed(KeyCode.S) || input.isPressed(KeyCode.DOWN)) {
            inputY += 1;
        }

        double length = Math.hypot(inputX, inputY);
        if (length > 0) {
            dashDirectionX = inputX / length;
            dashDirectionY = inputY / length;
            facingX = dashDirectionX;
            facingY = dashDirectionY;
        } else {
            double facingLength = Math.hypot(facingX, facingY);
            if (facingLength == 0) {
                dashDirectionX = 0;
                dashDirectionY = 1;
            } else {
                dashDirectionX = facingX / facingLength;
                dashDirectionY = facingY / facingLength;
            }
        }

        dashTimeRemaining = DASH_DURATION_SECONDS;
        dashCooldownRemaining = DASH_COOLDOWN_SECONDS;
        return true;
    }

    public boolean isDashing() {
        return dashTimeRemaining > 0;
    }

    public boolean isDashReady() {
        return dashCooldownRemaining <= 0 && dashTimeRemaining <= 0 && !hidden;
    }

    public double dashCooldownRemaining() {
        return dashCooldownRemaining;
    }
}
