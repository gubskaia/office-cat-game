package cat.game.officecatgame;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class SpriteLoader {
    private static final int ALPHA_THRESHOLD = 12;
    private static final int MASK_TOLERANCE = 24;

    private SpriteLoader() {
    }

    public static Image loadSingle(String resourcePath) {
        return cropOpaqueBounds(loadRaw(resourcePath));
    }

    public static Image loadMaskedSingle(String resourcePath) {
        return cropOpaqueBounds(removeBackground(loadRaw(resourcePath), MASK_TOLERANCE));
    }

    public static List<Image> loadStrip(String resourcePath) {
        Image raw = loadRaw(resourcePath);
        List<Image> frames = extractFrames(raw);
        if (frames.isEmpty()) {
            return List.of(cropOpaqueBounds(raw));
        }
        return frames;
    }

    public static Image loadRaw(String resourcePath) {
        InputStream stream = SpriteLoader.class.getResourceAsStream(resourcePath);
        if (stream == null) {
            throw new IllegalArgumentException("Missing resource: " + resourcePath);
        }
        return new Image(stream);
    }

    private static WritableImage removeBackground(Image source, int tolerance) {
        int width = (int) source.getWidth();
        int height = (int) source.getHeight();
        PixelReader reader = source.getPixelReader();
        WritableImage output = new WritableImage(width, height);
        PixelWriter writer = output.getPixelWriter();

        int[] cornerColors = {
                reader.getArgb(0, 0),
                reader.getArgb(width - 1, 0),
                reader.getArgb(0, height - 1),
                reader.getArgb(width - 1, height - 1)
        };

        boolean[] visited = new boolean[width * height];
        boolean[] transparent = new boolean[width * height];
        ArrayDeque<Integer> queue = new ArrayDeque<>();

        push(0, 0, width, visited, queue);
        push(width - 1, 0, width, visited, queue);
        push(0, height - 1, width, visited, queue);
        push(width - 1, height - 1, width, visited, queue);

        while (!queue.isEmpty()) {
            int index = queue.removeFirst();
            int x = index % width;
            int y = index / width;
            int argb = reader.getArgb(x, y);

            if (!isBackground(argb, cornerColors, tolerance)) {
                continue;
            }

            transparent[index] = true;

            if (x > 0) {
                push(x - 1, y, width, visited, queue);
            }
            if (x + 1 < width) {
                push(x + 1, y, width, visited, queue);
            }
            if (y > 0) {
                push(x, y - 1, width, visited, queue);
            }
            if (y + 1 < height) {
                push(x, y + 1, width, visited, queue);
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                int argb = reader.getArgb(x, y);
                writer.setArgb(x, y, transparent[index] ? (argb & 0x00FFFFFF) : argb);
            }
        }

        return output;
    }

    private static void push(int x, int y, int width, boolean[] visited, ArrayDeque<Integer> queue) {
        int index = y * width + x;
        if (!visited[index]) {
            visited[index] = true;
            queue.add(index);
        }
    }

    private static boolean isBackground(int argb, int[] cornerColors, int tolerance) {
        int alpha = (argb >>> 24) & 0xFF;
        if (alpha < ALPHA_THRESHOLD) {
            return true;
        }

        int red = (argb >>> 16) & 0xFF;
        int green = (argb >>> 8) & 0xFF;
        int blue = argb & 0xFF;

        for (int corner : cornerColors) {
            int cornerRed = (corner >>> 16) & 0xFF;
            int cornerGreen = (corner >>> 8) & 0xFF;
            int cornerBlue = corner & 0xFF;
            int difference = Math.abs(red - cornerRed) + Math.abs(green - cornerGreen) + Math.abs(blue - cornerBlue);
            if (difference <= tolerance) {
                return true;
            }
        }

        return false;
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
