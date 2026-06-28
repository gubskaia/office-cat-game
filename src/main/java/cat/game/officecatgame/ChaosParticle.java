package cat.game.officecatgame;

import javafx.scene.paint.Color;

public class ChaosParticle {
    private final Color color;
    private final double size;
    private double x;
    private double y;
    private double velocityX;
    private double velocityY;
    private double timeLeft;

    public ChaosParticle(
            double x,
            double y,
            double velocityX,
            double velocityY,
            double size,
            Color color,
            double lifetimeSeconds
    ) {
        this.x = x;
        this.y = y;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.size = size;
        this.color = color;
        this.timeLeft = lifetimeSeconds;
    }

    public void update(double deltaSeconds) {
        timeLeft = Math.max(0, timeLeft - deltaSeconds);
        x += velocityX * deltaSeconds;
        y += velocityY * deltaSeconds;
        velocityX *= 0.98;
        velocityY *= 0.98;
    }

    public boolean isAlive() {
        return timeLeft > 0;
    }

    public double alpha() {
        return Math.min(1.0, timeLeft / 0.6);
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double size() {
        return size;
    }

    public Color color() {
        return color;
    }
}
