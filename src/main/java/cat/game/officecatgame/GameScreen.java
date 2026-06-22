package cat.game.officecatgame;

import javafx.animation.AnimationTimer;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GameScreen extends StackPane {
    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;

    private static final double DAY_DURATION_SECONDS = 180.0;
    private static final double EVENT_DURATION_SECONDS = 2.8;
    private static final double MANAGER_PENALTY_COOLDOWN_SECONDS = 4.0;
    private static final Point PLAYER_RESPAWN = new Point(90, 110);

    private final Canvas canvas = new Canvas(WIDTH, HEIGHT);
    private final InputState input = new InputState();
    private final PlayerCat player = new PlayerCat(PLAYER_RESPAWN.x(), PLAYER_RESPAWN.y());
    private final List<Rect> walls = new ArrayList<>();
    private final List<ChaosInteraction> interactions = new ArrayList<>();
    private final List<ChaosEvent> chaosEvents = new ArrayList<>();
    private final List<EmployeeNpc> employees = new ArrayList<>();
    private final ManagerNpc manager = new ManagerNpc(
            780, 180,
            List.of(
                    new Point(760, 130),
                    new Point(1120, 180),
                    new Point(1090, 520),
                    new Point(620, 510),
                    new Point(260, 340)
            )
    );

    private AnimationTimer loop;
    private double chaosPercent;
    private double timeLeft = DAY_DURATION_SECONDS;
    private double managerPenaltyCooldown;
    private boolean interactionHeld;
    private boolean gameFinished;
    private String endMessage = "";

    public GameScreen() {
        getChildren().add(canvas);
        buildOfficeLayout();
    }

    public void bindInput(Scene scene) {
        scene.setOnKeyPressed(event -> input.setPressed(event.getCode(), true));
        scene.setOnKeyReleased(event -> input.setPressed(event.getCode(), false));
    }

    public void start() {
        loop = new AnimationTimer() {
            private long previousNanos;

            @Override
            public void handle(long now) {
                if (previousNanos == 0L) {
                    previousNanos = now;
                    render();
                    return;
                }

                double deltaSeconds = (now - previousNanos) / 1_000_000_000.0;
                previousNanos = now;

                update(deltaSeconds);
                render();
            }
        };
        loop.start();
    }

    private void update(double deltaSeconds) {
        if (!gameFinished) {
            timeLeft = Math.max(0, timeLeft - deltaSeconds);
            managerPenaltyCooldown = Math.max(0, managerPenaltyCooldown - deltaSeconds);
            player.update(input, deltaSeconds, walls);
            handleInteractionAttempt();
            updateChaosEvents(deltaSeconds);
            updateNpcs(deltaSeconds);
            handleManagerCatch();

            if (chaosPercent >= 100) {
                gameFinished = true;
                endMessage = "Victory! The office is fully consumed by cat chaos.";
            } else if (timeLeft <= 0) {
                gameFinished = true;
                endMessage = "Defeat! The work day ended before chaos reached 100%.";
            }
        }
    }

    private void handleInteractionAttempt() {
        boolean pressed = input.isPressed(KeyCode.E);
        if (pressed && !interactionHeld && !gameFinished) {
            ChaosInteraction nearest = getNearestInteraction();
            if (nearest != null && nearest.canTrigger()) {
                nearest.trigger();
                chaosPercent = Math.min(100, chaosPercent + nearest.chaosGain());
                chaosEvents.add(new ChaosEvent(
                        nearest.eventLabel(),
                        nearest.x(),
                        nearest.y(),
                        nearest.eventRadius(),
                        nearest.eventSeverity(),
                        EVENT_DURATION_SECONDS
                ));
            }
        }
        interactionHeld = pressed;
    }

    private void updateChaosEvents(double deltaSeconds) {
        chaosEvents.forEach(event -> event.update(deltaSeconds));
        chaosEvents.removeIf(event -> !event.isActive());
    }

    private void updateNpcs(double deltaSeconds) {
        for (EmployeeNpc employee : employees) {
            employee.update(deltaSeconds, player, strongestEventNear(employee.x(), employee.y(), 165));
        }
        manager.update(deltaSeconds, player, strongestEventNear(manager.x(), manager.y(), 260));
    }

    private ChaosEvent strongestEventNear(double x, double y, double maxDistance) {
        return chaosEvents.stream()
                .filter(ChaosEvent::isActive)
                .filter(event -> Math.hypot(event.x() - x, event.y() - y) <= Math.min(event.radius(), maxDistance))
                .max(Comparator.comparingDouble(ChaosEvent::severity))
                .orElse(null);
    }

    private void handleManagerCatch() {
        if (managerPenaltyCooldown > 0 || gameFinished) {
            return;
        }

        if (manager.catches(player)) {
            managerPenaltyCooldown = MANAGER_PENALTY_COOLDOWN_SECONDS;
            chaosPercent = Math.max(0, chaosPercent - 10);
            timeLeft = Math.max(0, timeLeft - 12);
            player.setPosition(PLAYER_RESPAWN.x(), PLAYER_RESPAWN.y());
        }
    }

    private ChaosInteraction getNearestInteraction() {
        ChaosInteraction result = null;
        double bestDistance = Double.MAX_VALUE;

        for (ChaosInteraction interaction : interactions) {
            double distance = interaction.distanceTo(player.centerX(), player.centerY());
            if (distance <= interaction.promptRadius() && distance < bestDistance) {
                bestDistance = distance;
                result = interaction;
            }
        }

        return result;
    }

    private void render() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        drawOffice(gc);
        drawChaosEvents(gc);
        drawInteractions(gc);
        drawNpcs(gc);
        drawPlayer(gc);
        drawHud(gc);

        ChaosInteraction nearest = getNearestInteraction();
        if (!gameFinished && nearest != null) {
            drawPrompt(gc, nearest);
        }

        if (gameFinished) {
            drawGameOverOverlay(gc);
        }
    }

    private void drawOffice(GraphicsContext gc) {
        gc.setFill(Color.web("#f8f3e8"));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        gc.setFill(Color.web("#e8eef4"));
        gc.fillRect(40, 70, 520, 300);
        gc.setFill(Color.web("#f2e7ff"));
        gc.fillRect(590, 70, 300, 220);
        gc.setFill(Color.web("#fff1d6"));
        gc.fillRect(920, 70, 300, 220);
        gc.setFill(Color.web("#ffe1d6"));
        gc.fillRect(780, 330, 440, 260);

        gc.setStroke(Color.web("#4f5d75"));
        gc.setLineWidth(4);
        gc.strokeRect(40, 70, 520, 300);
        gc.strokeRect(590, 70, 300, 220);
        gc.strokeRect(920, 70, 300, 220);
        gc.strokeRect(780, 330, 440, 260);

        gc.setFill(Color.web("#374151"));
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 20));
        gc.fillText("Open Space", 60, 100);
        gc.fillText("Meeting Room", 610, 100);
        gc.fillText("Kitchen", 940, 100);
        gc.fillText("Director's Office", 800, 360);

        gc.setFill(Color.web("#8b6f47"));
        for (Rect wall : walls) {
            gc.fillRect(wall.x(), wall.y(), wall.width(), wall.height());
        }
    }

    private void drawInteractions(GraphicsContext gc) {
        for (ChaosInteraction interaction : interactions) {
            gc.setFill(interaction.color());
            gc.fillRoundRect(interaction.x() - 16, interaction.y() - 16, 32, 32, 10, 10);

            if (!interaction.canTrigger()) {
                gc.setFill(Color.rgb(30, 30, 30, 0.35));
                gc.fillRoundRect(interaction.x() - 16, interaction.y() - 16, 32, 32, 10, 10);
            }
        }
    }

    private void drawChaosEvents(GraphicsContext gc) {
        for (ChaosEvent event : chaosEvents) {
            gc.setStroke(Color.rgb(239, 68, 68, 0.25));
            gc.setLineWidth(2);
            gc.strokeOval(
                    event.x() - event.radius() / 2.0,
                    event.y() - event.radius() / 2.0,
                    event.radius(),
                    event.radius()
            );
        }
    }

    private void drawNpcs(GraphicsContext gc) {
        for (EmployeeNpc employee : employees) {
            gc.setFill(employee.color());
            gc.fillRoundRect(employee.x() - 14, employee.y() - 14, 28, 28, 10, 10);
            drawSpeechTag(gc, employee.x(), employee.y() - 24, employee.reactionText(), Color.web("#1f2937"));
        }

        gc.setFill(manager.color());
        gc.fillRoundRect(manager.x() - 16, manager.y() - 16, 32, 32, 12, 12);
        drawSpeechTag(gc, manager.x(), manager.y() - 28, manager.statusText(), Color.web("#5b21b6"));
    }

    private void drawPlayer(GraphicsContext gc) {
        gc.setFill(Color.web("#d97706"));
        gc.fillRoundRect(player.x(), player.y(), player.width(), player.height(), 14, 14);

        gc.setFill(Color.web("#fff7ed"));
        gc.fillOval(player.x() + 8, player.y() + 9, 8, 8);
        gc.fillOval(player.x() + 22, player.y() + 9, 8, 8);
    }

    private void drawHud(GraphicsContext gc) {
        gc.setFill(Color.rgb(17, 24, 39, 0.88));
        gc.fillRoundRect(24, 18, 430, 110, 20, 20);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 18));
        gc.fillText("Office Cat: Chaos Manager", 42, 47);

        gc.setFont(Font.font("Verdana", 15));
        gc.fillText(String.format("Time left: %02d:%02d", (int) timeLeft / 60, (int) timeLeft % 60), 42, 74);
        gc.fillText("Goal: reach 100% chaos before the work day ends", 42, 96);
        gc.fillText("Penalty: manager catch costs 10% chaos and 12 seconds", 42, 118);

        gc.setFill(Color.rgb(255, 255, 255, 0.2));
        gc.fillRoundRect(470, 28, 320, 24, 12, 12);
        gc.setFill(Color.web("#ef4444"));
        gc.fillRoundRect(470, 28, 3.2 * chaosPercent, 24, 12, 12);

        gc.setFill(Color.web("#111827"));
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 14));
        gc.fillText(String.format("Chaos: %.0f%%", chaosPercent), 585, 45);

        gc.setFill(Color.rgb(17, 24, 39, 0.88));
        gc.fillRoundRect(820, 18, 420, 110, 20, 20);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Verdana", 15));
        gc.fillText("Controls: WASD / arrows to move, E to cause trouble", 842, 48);
        gc.fillText("Current MVP actions: keyboard nap, mug push, Wi-Fi sabotage", 842, 76);
        gc.fillText("Manager: " + manager.statusText(), 842, 104);
    }

    private void drawPrompt(GraphicsContext gc, ChaosInteraction interaction) {
        gc.setFill(Color.rgb(17, 24, 39, 0.92));
        gc.fillRoundRect(420, 635, 440, 48, 16, 16);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 16));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText("[E] " + interaction.promptText(), WIDTH / 2.0, 659);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.BASELINE);
    }

    private void drawGameOverOverlay(GraphicsContext gc) {
        gc.setFill(Color.rgb(0, 0, 0, 0.5));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        gc.setFill(Color.rgb(255, 248, 235, 0.95));
        gc.fillRoundRect(250, 220, 780, 220, 28, 28);

        gc.setFill(Color.web("#111827"));
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 30));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(endMessage, WIDTH / 2.0, 300);

        gc.setFont(Font.font("Verdana", 20));
        gc.fillText(String.format("Final chaos level: %.0f%%", chaosPercent), WIDTH / 2.0, 350);
        gc.fillText("Restart is still app relaunch for now.", WIDTH / 2.0, 390);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawSpeechTag(GraphicsContext gc, double centerX, double y, String text, Color background) {
        gc.setFill(background);
        gc.fillRoundRect(centerX - 62, y - 14, 124, 24, 12, 12);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Verdana", 11));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(text, centerX, y - 2);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.BASELINE);
    }

    private void buildOfficeLayout() {
        walls.add(new Rect(250, 140, 220, 34));
        walls.add(new Rect(250, 235, 220, 34));
        walls.add(new Rect(650, 150, 180, 34));
        walls.add(new Rect(980, 150, 160, 34));
        walls.add(new Rect(920, 410, 220, 34));
        walls.add(new Rect(120, 450, 540, 32));
        walls.add(new Rect(120, 560, 280, 32));
        walls.add(new Rect(540, 560, 120, 32));

        interactions.add(new ChaosInteraction(
                360, 157,
                "Nap on a developer keyboard",
                "keyboard chaos",
                18,
                5.0,
                76,
                180,
                5.2,
                Color.web("#60a5fa")
        ));
        interactions.add(new ChaosInteraction(
                1040, 165,
                "Push a coffee mug off the kitchen counter",
                "spilled coffee",
                12,
                4.0,
                70,
                150,
                4.6,
                Color.web("#f59e0b")
        ));
        interactions.add(new ChaosInteraction(
                1010, 430,
                "Disable the office Wi-Fi router",
                "Wi-Fi outage",
                26,
                8.0,
                82,
                320,
                8.7,
                Color.web("#ef4444")
        ));

        employees.add(new EmployeeNpc("Mila", 165, 155, Color.web("#3b82f6")));
        employees.add(new EmployeeNpc("Jon", 160, 250, Color.web("#22c55e")));
        employees.add(new EmployeeNpc("Ava", 720, 185, Color.web("#f97316")));
        employees.add(new EmployeeNpc("Noah", 960, 505, Color.web("#ec4899")));
    }
}
