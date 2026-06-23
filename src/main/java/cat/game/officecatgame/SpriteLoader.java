package cat.game.officecatgame;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class SpriteLoader {
    private static final int ALPHA_THRESHOLD = 12;

    private SpriteLoader() {
    }

    public static Image loadSingle(String resourcePath) {
        return cropOpaqueBounds(loadRaw(resourcePath));
    }

    public static List<Image> loadStrip(String resourcePath) {
        Image raw = loadRaw(resourcePath);
        List<Image> frames = extractFrames(raw);
        if (frames.isEmpty()) {
            return List.of(cropOpaqueBounds(raw));
        }
        return frames;
    }

    private static Image loadRaw(String resourcePath) {
        InputStream stream = SpriteLoader.class.getResourceAsStream(resourcePath);
        if (stream == null) {
            throw new IllegalArgumentException("Missing resource: " + resourcePath);
        }
        return new Image(stream);
    }

    private static Image cropOpaqueBounds(Image image) {
        PixelReader reader = image.getPixelReader();
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();

        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int alpha = (reader.getArgb(x, y) >>> 24) & 0xFF;
                if (alpha >= ALPHA_THRESHOLD) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return image;
        }

        return new WritableImage(reader, minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static List<Image> extractFrames(Image strip) {
        PixelReader reader = strip.getPixelReader();
        int width = (int) strip.getWidth();
        int height = (int) strip.getHeight();
        List<Image> frames = new ArrayList<>();

        int startX = -1;
        for (int x = 0; x < width; x++) {
            boolean opaque = columnHasOpaquePixels(reader, x, height);
            if (opaque && startX == -1) {
                startX = x;
            } else if (!opaque && startX != -1) {
                frames.add(cropFrame(reader, startX, x - 1, height));
                startX = -1;
            }
        }

        if (startX != -1) {
            frames.add(cropFrame(reader, startX, width - 1, height));
        }

        return frames.stream().filter(frame -> frame != null).toList();
    }

    private static boolean columnHasOpaquePixels(PixelReader reader, int x, int height) {
        for (int y = 0; y < height; y++) {
            int alpha = (reader.getArgb(x, y) >>> 24) & 0xFF;
            if (alpha >= ALPHA_THRESHOLD) {
                return true;
            }
        }
        return false;
    }

    private static Image cropFrame(PixelReader reader, int startX, int endX, int height) {
        int minY = height;
        int maxY = -1;

        for (int x = startX; x <= endX; x++) {
            for (int y = 0; y < height; y++) {
                int alpha = (reader.getArgb(x, y) >>> 24) & 0xFF;
                if (alpha >= ALPHA_THRESHOLD) {
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        if (maxY < minY) {
            return null;
        }

        return new WritableImage(reader, startX, minY, endX - startX + 1, maxY - minY + 1);
    }
}
