package cat.game.officecatgame;

public record Rect(double x, double y, double width, double height) {
    public boolean intersects(Rect other) {
        return x < other.x + other.width
                && x + width > other.x
                && y < other.y + other.height
                && y + height > other.y;
    }
}
