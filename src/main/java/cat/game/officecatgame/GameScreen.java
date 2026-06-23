package cat.game.officecatgame;

import javafx.animation.AnimationTimer;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
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
    private enum GameState {
        MENU,
        PLAYING,
        PAUSED,
        FINISHED
    }

    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;

    private static final double DAY_DURATION_SECONDS = 180.0;
    private static final double EVENT_DURATION_SECONDS = 2.8;
    private static final double MANAGER_PENALTY_COOLDOWN_SECONDS = 4.0;
    private static final double COMBO_WINDOW_SECONDS = 6.0;
    private static final double SHAKE_DECAY_PER_SECOND = 3.4;
    private static final Point PLAYER_RESPAWN = new Point(90, 110);
    private static final double PLAYER_DRAW_SIZE = 78;
    private static final double NPC_DRAW_SIZE = 72;

    private final Canvas canvas = new Canvas(WIDTH, HEIGHT);
    private final InputState input = new InputState();
    private final PlayerCat player = new PlayerCat(PLAYER_RESPAWN.x(), PLAYER_RESPAWN.y());
    private final List<Rect> walls = new ArrayList<>();
    private final List<ChaosInteraction> interactions = new ArrayList<>();
    private final List<HideSpot> hideSpots = new ArrayList<>();
    private final List<ChaosEvent> chaosEvents = new ArrayList<>();
    private final List<FloatingText> floatingTexts = new ArrayList<>();
    private final List<IncidentFeedEntry> incidentFeed = new ArrayList<>();
    private final List<ChaosObjective> objectiveDeck = List.of(
            new ChaosObjective("keyboard", "Keyboard Tyrant", "Nap on the developer keyboard", 8),
            new ChaosObjective("mug", "Coffee Disaster", "Knock the kitchen mug to the floor", 7),
            new ChaosObjective("wifi", "Offline Office", "Disable the office Wi-Fi", 10),
            new ChaosObjective("meeting", "Meeting Menace", "Interrupt the online call with a meow", 8),
            new ChaosObjective("papers", "Executive Mess", "Scatter the director's paperwork", 9)
    );
    private final List<EmployeeNpc> employees = new ArrayList<>();
    private final List<Point> managerPatrolPath = List.of(
            new Point(760, 130),
            new Point(1120, 180),
            new Point(1090, 520),
            new Point(620, 510),
            new Point(260, 340)
    );

    private final Point managerSpawn = new Point(780, 180);
    private final Image catIdleSprite = SpriteLoader.loadSingle("/assets/sprites/cat/cat_idle.png");
    private final Image catHideSprite = SpriteLoader.loadSingle("/assets/sprites/cat/cat_hide.png");
    private final List<Image> catWalkFrames = SpriteLoader.loadStrip("/assets/sprites/cat/cat_walk.png");
    private final Image employeeIdleSprite = SpriteLoader.loadSingle("/assets/sprites/employees/employee_idle.png");
    private final List<Image> employeeWalkFrames = SpriteLoader.loadStrip("/assets/sprites/employees/employee_walk.png");
    private final List<Image> employeeReactFrames = SpriteLoader.loadStrip("/assets/sprites/employees/employee_react.png");
    private final List<Image> employeePanicFrames = SpriteLoader.loadStrip("/assets/sprites/employees/employee_panic.png");
    private final Image managerIdleSprite = SpriteLoader.loadSingle("/assets/sprites/manager/manager_idle.png");
    private final List<Image> managerAlertFrames = SpriteLoader.loadStrip("/assets/sprites/manager/manager_alert.png");
    private final List<Image> managerWalkFrames = SpriteLoader.loadStrip("/assets/sprites/manager/manager_walk.png");
    private final List<Image> managerChaseFrames = SpriteLoader.loadStrip("/assets/sprites/manager/manager_chase.png");

    private ManagerNpc manager = new ManagerNpc(managerSpawn.x(), managerSpawn.y(), managerPatrolPath);

    private AnimationTimer loop;
    private double chaosPercent;
    private double timeLeft = DAY_DURATION_SECONDS;
    private double managerPenaltyCooldown;
    private double comboTimer;
    private double animationClock;
    private double shakeIntensity;
    private double shakePhase;
    private int comboCount;
    private int objectiveIndex;
    private ChaosObjective currentObjective;
    private boolean interactionHeld;
    private HideSpot activeHideSpot;
    private boolean startHeld;
    private boolean pauseHeld;
    private boolean restartHeld;
    private GameState gameState = GameState.MENU;
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
        handleStateInput();

        if (gameState == GameState.PLAYING) {
            animationClock += deltaSeconds;
            timeLeft = Math.max(0, timeLeft - deltaSeconds);
            managerPenaltyCooldown = Math.max(0, managerPenaltyCooldown - deltaSeconds);
            comboTimer = Math.max(0, comboTimer - deltaSeconds);
            shakeIntensity = Math.max(0, shakeIntensity - SHAKE_DECAY_PER_SECOND * deltaSeconds);
            shakePhase += deltaSeconds * 34;
            if (comboTimer == 0) {
                comboCount = 0;
            }
            player.update(input, deltaSeconds, walls);
            handleInteractionAttempt();
            updateChaosEvents(deltaSeconds);
            updateFloatingTexts(deltaSeconds);
            updateIncidentFeed(deltaSeconds);
            updateNpcs(deltaSeconds);
            handleManagerCatch();

            if (chaosPercent >= 100) {
                gameState = GameState.FINISHED;
                endMessage = "Victory! The office is fully consumed by cat chaos.";
            } else if (timeLeft <= 0) {
                gameState = GameState.FINISHED;
                endMessage = "Defeat! The work day ended before chaos reached 100%.";
            }
        }
    }

    private void handleStateInput() {
        boolean startPressed = input.isPressed(KeyCode.ENTER);
        boolean pausePressed = input.isPressed(KeyCode.ESCAPE) || input.isPressed(KeyCode.P);
        boolean restartPressed = input.isPressed(KeyCode.R);

        if (startPressed && !startHeld) {
            if (gameState == GameState.MENU) {
                startNewRun();
            } else if (gameState == GameState.FINISHED) {
                returnToMenu();
            }
        }

        if (pausePressed && !pauseHeld) {
            if (gameState == GameState.PLAYING) {
                gameState = GameState.PAUSED;
            } else if (gameState == GameState.PAUSED) {
                gameState = GameState.PLAYING;
            }
        }

        if (restartPressed && !restartHeld) {
            if (gameState == GameState.PLAYING || gameState == GameState.PAUSED || gameState == GameState.FINISHED) {
                startNewRun();
            }
        }

        startHeld = startPressed;
        pauseHeld = pausePressed;
        restartHeld = restartPressed;
    }

    private void startNewRun() {
        resetSession();
        gameState = GameState.PLAYING;
    }

    private void returnToMenu() {
        resetSession();
        gameState = GameState.MENU;
    }

    private void resetSession() {
        chaosPercent = 0;
        timeLeft = DAY_DURATION_SECONDS;
        managerPenaltyCooldown = 0;
        comboTimer = 0;
        comboCount = 0;
        animationClock = 0;
        shakeIntensity = 0;
        shakePhase = 0;
        objectiveIndex = 0;
        currentObjective = nextObjective();
        interactionHeld = false;
        endMessage = "";
        player.setPosition(PLAYER_RESPAWN.x(), PLAYER_RESPAWN.y());
        player.setHidden(false);
        activeHideSpot = null;
        chaosEvents.clear();
        floatingTexts.clear();
        incidentFeed.clear();

        for (ChaosInteraction interaction : interactions) {
            interaction.reset();
        }
        for (EmployeeNpc employee : employees) {
            employee.reset();
        }
        manager.reset(managerSpawn.x(), managerSpawn.y());
    }

    private void handleInteractionAttempt() {
        boolean pressed = input.isPressed(KeyCode.E);
        if (pressed && !interactionHeld && gameState == GameState.PLAYING) {
            if (player.isHidden()) {
                exitHideSpot();
            } else {
                HideSpot nearestHideSpot = getNearestHideSpot();
                ChaosInteraction nearest = getNearestInteraction();

                if (nearestHideSpot != null && shouldPreferHideSpot(nearestHideSpot, nearest)) {
                    enterHideSpot(nearestHideSpot);
                } else if (nearest != null && nearest.canTrigger()) {
                    nearest.trigger();
                    double chaosGain = applyCombo(nearest);
                    chaosPercent = Math.min(100, chaosPercent + chaosGain);
                    chaosEvents.add(new ChaosEvent(
                            nearest.eventLabel(),
                            nearest.x(),
                            nearest.y(),
                            nearest.eventRadius(),
                            nearest.eventSeverity(),
                            EVENT_DURATION_SECONDS
                    ));
                    addIncident("Chaos: " + nearest.eventLabel());
                    floatingTexts.add(new FloatingText(
                            String.format("+%.0f chaos", chaosGain),
                            nearest.x(),
                            nearest.y() - 18,
                            Color.web("#ef4444"),
                            1.2
                    ));
                    if (comboCount >= 2) {
                        floatingTexts.add(new FloatingText(
                                "Combo x" + comboCount,
                                nearest.x(),
                                nearest.y() - 40,
                            Color.web("#fbbf24"),
                            1.4
                        ));
                    }
                    addShake(nearest.eventSeverity() >= 7 ? 7.5 : 4.0);
                    checkObjectiveCompletion(nearest);
                }
            }
        }
        interactionHeld = pressed;
    }

    private ChaosObjective nextObjective() {
        if (objectiveDeck.isEmpty()) {
            return null;
        }
        ChaosObjective template = objectiveDeck.get(objectiveIndex % objectiveDeck.size());
        objectiveIndex++;
        return new ChaosObjective(
                template.interactionId(),
                template.title(),
                template.description(),
                template.bonusChaos()
        );
    }

    private void checkObjectiveCompletion(ChaosInteraction interaction) {
        if (currentObjective == null || currentObjective.completed() || !currentObjective.matches(interaction)) {
            return;
        }

        currentObjective.complete();
        chaosPercent = Math.min(100, chaosPercent + currentObjective.bonusChaos());
        addIncident("Objective complete: " + currentObjective.title());
        floatingTexts.add(new FloatingText(
                currentObjective.title() + " complete!",
                interaction.x(),
                interaction.y() - 62,
                Color.web("#34d399"),
                1.8
        ));
        floatingTexts.add(new FloatingText(
                String.format("Objective bonus +%.0f", currentObjective.bonusChaos()),
                interaction.x(),
                interaction.y() - 82,
                Color.web("#a7f3d0"),
                1.8
        ));
        currentObjective = nextObjective();
    }

    private double applyCombo(ChaosInteraction interaction) {
        comboCount = Math.min(comboCount + 1, 5);
        comboTimer = COMBO_WINDOW_SECONDS;
        double multiplier = 1.0 + (comboCount - 1) * 0.18;
        return interaction.chaosGain() * multiplier;
    }

    private void enterHideSpot(HideSpot hideSpot) {
        activeHideSpot = hideSpot;
        player.setPosition(hideSpot.x() - player.width() / 2.0, hideSpot.y() - player.height() / 2.0);
        player.setHidden(true);
    }

    private void exitHideSpot() {
        if (activeHideSpot != null) {
            player.setPosition(activeHideSpot.x() + 34, activeHideSpot.y());
        }
        player.setHidden(false);
        activeHideSpot = null;
    }

    private boolean shouldPreferHideSpot(HideSpot hideSpot, ChaosInteraction interaction) {
        if (hideSpot == null) {
            return false;
        }
        if (interaction == null) {
            return true;
        }
        return hideSpot.distanceTo(player.centerX(), player.centerY())
                <= interaction.distanceTo(player.centerX(), player.centerY());
    }

    private void updateChaosEvents(double deltaSeconds) {
        chaosEvents.forEach(event -> event.update(deltaSeconds));
        chaosEvents.removeIf(event -> !event.isActive());
    }

    private void updateFloatingTexts(double deltaSeconds) {
        floatingTexts.forEach(text -> text.update(deltaSeconds));
        floatingTexts.removeIf(text -> !text.isAlive());
    }

    private void updateIncidentFeed(double deltaSeconds) {
        incidentFeed.forEach(entry -> entry.update(deltaSeconds));
        incidentFeed.removeIf(entry -> !entry.isAlive());
    }

    private void updateNpcs(double deltaSeconds) {
        for (EmployeeNpc employee : employees) {
            employee.update(deltaSeconds, player, strongestEventNear(employee.x(), employee.y(), 165));
        }
        manager.update(deltaSeconds, player, strongestEventNear(manager.x(), manager.y(), 260), currentChaosPressure());
    }

    private double currentChaosPressure() {
        double pressure = chaosPercent / 40.0;
        if (comboCount >= 3 && comboTimer > 0) {
            pressure += 0.45;
        }
        return Math.min(2.0, pressure);
    }

    private ChaosEvent strongestEventNear(double x, double y, double maxDistance) {
        return chaosEvents.stream()
                .filter(ChaosEvent::isActive)
                .filter(event -> Math.hypot(event.x() - x, event.y() - y) <= Math.min(event.radius(), maxDistance))
                .max(Comparator.comparingDouble(ChaosEvent::severity))
                .orElse(null);
    }

    private void handleManagerCatch() {
        if (managerPenaltyCooldown > 0 || gameState != GameState.PLAYING) {
            return;
        }

        if (manager.catches(player)) {
            managerPenaltyCooldown = MANAGER_PENALTY_COOLDOWN_SECONDS;
            chaosPercent = Math.max(0, chaosPercent - 10);
            timeLeft = Math.max(0, timeLeft - 12);
            comboTimer = 0;
            comboCount = 0;
            player.setHidden(false);
            activeHideSpot = null;
            player.setPosition(PLAYER_RESPAWN.x(), PLAYER_RESPAWN.y());
            addIncident("Manager intercepted the cat");
            addShake(9.0);
            floatingTexts.add(new FloatingText(
                    "Manager caught you! -10 chaos",
                    PLAYER_RESPAWN.x() + 100,
                    PLAYER_RESPAWN.y() - 12,
                    Color.web("#93c5fd"),
                    1.6
            ));
        }
    }

    private ChaosInteraction getNearestInteraction() {
        if (player.isHidden()) {
            return null;
        }

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

    private HideSpot getNearestHideSpot() {
        if (player.isHidden()) {
            return activeHideSpot;
        }

        HideSpot result = null;
        double bestDistance = Double.MAX_VALUE;

        for (HideSpot hideSpot : hideSpots) {
            double distance = hideSpot.distanceTo(player.centerX(), player.centerY());
            if (distance <= hideSpot.promptRadius() && distance < bestDistance) {
                bestDistance = distance;
                result = hideSpot;
            }
        }

        return result;
    }

    private void render() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setImageSmoothing(false);
        gc.save();
        applyCameraShake(gc);
        drawOffice(gc);
        drawChaosEvents(gc);
        drawInteractions(gc);
        drawHideSpots(gc);
        drawNpcs(gc);
        drawPlayer(gc);
        drawFloatingTexts(gc);
        gc.restore();
        drawHud(gc);
        drawIncidentFeed(gc);

        ChaosInteraction nearest = getNearestInteraction();
        HideSpot nearestHideSpot = getNearestHideSpot();
        if (gameState == GameState.PLAYING) {
            if (player.isHidden()) {
                drawHidePrompt(gc);
            } else if (nearestHideSpot != null && shouldPreferHideSpot(nearestHideSpot, nearest)) {
                drawHideSpotPrompt(gc, nearestHideSpot);
            } else if (nearest != null) {
                drawPrompt(gc, nearest);
            }
        }

        if (gameState == GameState.MENU) {
            drawMenuOverlay(gc);
        } else if (gameState == GameState.PAUSED) {
            drawPauseOverlay(gc);
        } else if (gameState == GameState.FINISHED) {
            drawGameOverOverlay(gc);
        }
    }

    private void applyCameraShake(GraphicsContext gc) {
        if (shakeIntensity <= 0) {
            return;
        }
        double offsetX = Math.sin(shakePhase) * shakeIntensity;
        double offsetY = Math.cos(shakePhase * 1.6) * shakeIntensity * 0.7;
        gc.translate(offsetX, offsetY);
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

    private void drawHideSpots(GraphicsContext gc) {
        for (HideSpot hideSpot : hideSpots) {
            gc.setFill(Color.web("#c08457"));
            gc.fillRoundRect(hideSpot.x() - 20, hideSpot.y() - 20, 40, 40, 8, 8);
            gc.setStroke(Color.web("#7c5a36"));
            gc.strokeRoundRect(hideSpot.x() - 20, hideSpot.y() - 20, 40, 40, 8, 8);
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
            drawCenteredSprite(gc, employeeSprite(employee), employee.x(), employee.y(), NPC_DRAW_SIZE);
            drawSpeechTag(gc, employee.x(), employee.y() - 24, employee.reactionText(), Color.web("#1f2937"));
        }

        drawCenteredSprite(gc, managerSprite(), manager.x(), manager.y(), NPC_DRAW_SIZE);
        drawSpeechTag(gc, manager.x(), manager.y() - 28, manager.statusText(), Color.web("#5b21b6"));
    }

    private void drawPlayer(GraphicsContext gc) {
        Image sprite = player.isHidden() ? catHideSprite : currentCatSprite();
        drawCenteredSprite(gc, sprite, player.centerX(), player.centerY(), PLAYER_DRAW_SIZE);
    }

    private void drawFloatingTexts(GraphicsContext gc) {
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 16));
        for (FloatingText text : floatingTexts) {
            gc.setFill(text.color().deriveColor(0, 1, 1, text.alpha()));
            gc.fillText(text.text(), text.x(), text.y());
        }
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawHud(GraphicsContext gc) {
        gc.setFill(Color.rgb(17, 24, 39, 0.88));
        gc.fillRoundRect(24, 18, 470, 130, 20, 20);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 18));
        gc.fillText("Office Cat: Chaos Manager", 42, 47);

        gc.setFont(Font.font("Verdana", 15));
        gc.fillText(String.format("Time left: %02d:%02d", (int) timeLeft / 60, (int) timeLeft % 60), 42, 74);
        gc.fillText("Goal: reach 100% chaos before the work day ends", 42, 96);
        gc.fillText(player.isHidden() ? "Status: hidden in a box" : "Status: exposed and chaotic", 42, 118);
        gc.fillText(comboCount > 1 && comboTimer > 0
                ? String.format("Combo x%d active for %.1fs", comboCount, comboTimer)
                : "Combo: build a streak by chaining chaos quickly", 42, 140);

        gc.setFill(Color.rgb(255, 255, 255, 0.2));
        gc.fillRoundRect(470, 28, 320, 24, 12, 12);
        gc.setFill(Color.web("#ef4444"));
        gc.fillRoundRect(470, 28, 3.2 * chaosPercent, 24, 12, 12);

        gc.setFill(Color.web("#111827"));
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 14));
        gc.fillText(String.format("Chaos: %.0f%%", chaosPercent), 585, 45);

        gc.setFill(Color.rgb(17, 24, 39, 0.88));
        gc.fillRoundRect(790, 18, 450, 130, 20, 20);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Verdana", 15));
        gc.fillText("Controls: WASD / arrows to move, E to cause trouble", 812, 48);
        gc.fillText("Actions: keyboard nap, mug push, Wi-Fi, meeting meow, paper shred", 812, 76);
        gc.fillText("Manager: " + manager.statusText(), 812, 104);
        gc.fillText(String.format("Pressure: %.0f%%", currentChaosPressure() * 50), 812, 132);
        gc.fillText("State: " + gameState.name(), 1060, 132);

        gc.setFill(Color.rgb(17, 24, 39, 0.9));
        gc.fillRoundRect(24, 160, 1216, 72, 20, 20);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 16));
        gc.fillText("Current Objective", 42, 188);
        gc.setFont(Font.font("Verdana", 15));
        if (currentObjective != null) {
            gc.fillText(currentObjective.title() + ": " + currentObjective.description(), 42, 214);
            gc.fillText(String.format("Reward: +%.0f bonus chaos", currentObjective.bonusChaos()), 960, 214);
        }
    }

    private void drawIncidentFeed(GraphicsContext gc) {
        gc.setFill(Color.rgb(17, 24, 39, 0.88));
        gc.fillRoundRect(958, 160, 282, 120, 18, 18);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 15));
        gc.fillText("Recent Incidents", 978, 186);
        gc.setFont(Font.font("Verdana", 13));

        int line = 0;
        for (IncidentFeedEntry entry : incidentFeed) {
            if (line >= 4) {
                break;
            }
            gc.setFill(Color.WHITE.deriveColor(0, 1, 1, entry.alpha()));
            gc.fillText("- " + entry.text(), 978, 210 + line * 18);
            line++;
        }
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

    private void drawHideSpotPrompt(GraphicsContext gc, HideSpot hideSpot) {
        gc.setFill(Color.rgb(17, 24, 39, 0.92));
        gc.fillRoundRect(420, 635, 440, 48, 16, 16);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 16));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText("[E] Hide in " + hideSpot.label(), WIDTH / 2.0, 659);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.BASELINE);
    }

    private void drawHidePrompt(GraphicsContext gc) {
        gc.setFill(Color.rgb(17, 24, 39, 0.92));
        gc.fillRoundRect(420, 635, 440, 48, 16, 16);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 16));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText("[E] Sneak out of the box", WIDTH / 2.0, 659);
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
        gc.fillText("[R] Restart run    [Enter] Return to menu", WIDTH / 2.0, 390);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawMenuOverlay(GraphicsContext gc) {
        gc.setFill(Color.rgb(10, 15, 25, 0.55));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        gc.setFill(Color.rgb(255, 248, 235, 0.96));
        gc.fillRoundRect(210, 150, 860, 360, 32, 32);

        gc.setFill(Color.web("#111827"));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 36));
        gc.fillText("Office Cat: Chaos Manager", WIDTH / 2.0, 235);

        gc.setFont(Font.font("Verdana", 22));
        gc.fillText("Turn a productive office into a furry disaster zone.", WIDTH / 2.0, 285);

        gc.setFont(Font.font("Verdana", 18));
        gc.fillText("Reach 100% chaos before the work day ends.", WIDTH / 2.0, 338);
        gc.fillText("Avoid the manager, trigger distractions, and hide in boxes.", WIDTH / 2.0, 370);
        gc.fillText("Follow daily cat objectives for bonus chaos.", WIDTH / 2.0, 402);
        gc.fillText("Big sabotage events now shake the office view.", WIDTH / 2.0, 430);
        gc.fillText("[Enter] Start run", WIDTH / 2.0, 462);
        gc.fillText("[WASD / Arrows] Move    [E] Interact    [Esc / P] Pause", WIDTH / 2.0, 494);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawPauseOverlay(GraphicsContext gc) {
        gc.setFill(Color.rgb(0, 0, 0, 0.42));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        gc.setFill(Color.rgb(255, 248, 235, 0.95));
        gc.fillRoundRect(320, 220, 640, 200, 28, 28);

        gc.setFill(Color.web("#111827"));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 30));
        gc.fillText("Paused", WIDTH / 2.0, 290);

        gc.setFont(Font.font("Verdana", 20));
        gc.fillText("[Esc / P] Resume", WIDTH / 2.0, 338);
        gc.fillText("[R] Restart run", WIDTH / 2.0, 372);
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

        hideSpots.add(new HideSpot(120, 340, 68, "storage box"));
        hideSpots.add(new HideSpot(916, 175, 68, "meeting room box"));
        hideSpots.add(new HideSpot(1180, 520, 68, "director archive box"));

        interactions.add(new ChaosInteraction(
                "keyboard",
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
                "mug",
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
                "wifi",
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
        interactions.add(new ChaosInteraction(
                "meeting",
                730, 165,
                "Meow during the online meeting",
                "meeting disruption",
                16,
                5.5,
                78,
                240,
                6.4,
                Color.web("#8b5cf6")
        ));
        interactions.add(new ChaosInteraction(
                "papers",
                1110, 430,
                "Scatter the director's paperwork",
                "paper catastrophe",
                20,
                6.2,
                80,
                210,
                7.2,
                Color.web("#10b981")
        ));

        employees.add(new EmployeeNpc("Mila", 165, 155, Color.web("#3b82f6")));
        employees.add(new EmployeeNpc("Jon", 160, 250, Color.web("#22c55e")));
        employees.add(new EmployeeNpc("Ava", 720, 185, Color.web("#f97316")));
        employees.add(new EmployeeNpc("Noah", 960, 505, Color.web("#ec4899")));
    }

    private void addIncident(String text) {
        incidentFeed.add(0, new IncidentFeedEntry(text, 6.0));
        while (incidentFeed.size() > 5) {
            incidentFeed.remove(incidentFeed.size() - 1);
        }
    }

    private void addShake(double amount) {
        shakeIntensity = Math.max(shakeIntensity, amount);
    }

    private Image currentCatSprite() {
        if (player.isMoving() && !catWalkFrames.isEmpty()) {
            return frameAt(catWalkFrames, 7.5);
        }
        return catIdleSprite;
    }

    private Image employeeSprite(EmployeeNpc employee) {
        return switch (employee.state()) {
            case PANICKING -> employeePanicFrames.isEmpty() ? employeeIdleSprite : frameAt(employeePanicFrames, 4.0);
            case INVESTIGATING -> employeeWalkFrames.isEmpty() ? employeeIdleSprite : frameAt(employeeWalkFrames, 4.5);
            case DISTRACTED -> employeeReactFrames.isEmpty() ? employeeIdleSprite : frameAt(employeeReactFrames, 4.0);
            case WORKING -> employeeIdleSprite;
        };
    }

    private Image managerSprite() {
        return switch (manager.mode()) {
            case CHASING -> managerChaseFrames.isEmpty() ? managerIdleSprite : frameAt(managerChaseFrames, 8.0);
            case INVESTIGATING -> managerAlertFrames.isEmpty() ? managerIdleSprite : frameAt(managerAlertFrames, 4.5);
            case PATROLLING -> managerWalkFrames.isEmpty() ? managerIdleSprite : frameAt(managerWalkFrames, 6.0);
        };
    }

    private Image frameAt(List<Image> frames, double framesPerSecond) {
        int index = (int) (animationClock * framesPerSecond) % frames.size();
        return frames.get(index);
    }

    private void drawCenteredSprite(GraphicsContext gc, Image sprite, double centerX, double centerY, double maxSize) {
        double width = sprite.getWidth();
        double height = sprite.getHeight();
        double scale = Math.min(maxSize / width, maxSize / height);
        double drawWidth = width * scale;
        double drawHeight = height * scale;
        gc.drawImage(sprite, centerX - drawWidth / 2.0, centerY - drawHeight / 2.0, drawWidth, drawHeight);
    }
}
