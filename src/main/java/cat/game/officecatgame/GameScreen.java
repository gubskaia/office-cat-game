package cat.game.officecatgame;

import javafx.animation.AnimationTimer;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
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
    private static final double MEOW_COOLDOWN_SECONDS = 5.0;
    private static final double MEOW_CHAOS_GAIN = 4.0;
    private static final double MEOW_EVENT_RADIUS = 210.0;
    private static final double MEOW_EVENT_SEVERITY = 4.8;
    private static final double DANGER_ZONE_DURATION_SECONDS = 8.0;
    private static final double DANGER_ZONE_RADIUS = 92.0;
    private static final double DANGER_ZONE_PENALTY_INTERVAL = 1.25;
    private static final double SHAKE_DECAY_PER_SECOND = 3.4;
    private static final Point PLAYER_RESPAWN = new Point(90, 110);
    private static final double PLAYER_DRAW_SIZE = 78;
    private static final double NPC_DRAW_SIZE = 72;
    private static final double PROP_SMALL = 54;
    private static final double PROP_MEDIUM = 82;
    private static final double PROP_LARGE = 190;
    private static final double PROP_XL = 250;

    private final Canvas canvas = new Canvas(WIDTH, HEIGHT);
    private final InputState input = new InputState();
    private final PlayerCat player = new PlayerCat(PLAYER_RESPAWN.x(), PLAYER_RESPAWN.y());
    private final List<Rect> walls = new ArrayList<>();
    private final List<ChaosInteraction> interactions = new ArrayList<>();
    private final List<HideSpot> hideSpots = new ArrayList<>();
    private final List<ChaosEvent> chaosEvents = new ArrayList<>();
    private final List<DangerZone> dangerZones = new ArrayList<>();
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
    private final Image catPounceSprite = SpriteLoader.loadSingle("/assets/sprites/cat/cat_pounce.png");
    private final List<Image> catWalkFrames = SpriteLoader.loadStrip("/assets/sprites/cat/cat_walk.png");
    private final Image employeeIdleSprite = SpriteLoader.loadSingle("/assets/sprites/employees/employee_idle.png");
    private final List<Image> employeeWalkFrames = SpriteLoader.loadStrip("/assets/sprites/employees/employee_walk.png");
    private final List<Image> employeeReactFrames = SpriteLoader.loadStrip("/assets/sprites/employees/employee_react.png");
    private final List<Image> employeePanicFrames = SpriteLoader.loadStrip("/assets/sprites/employees/employee_panic.png");
    private final Image managerIdleSprite = SpriteLoader.loadSingle("/assets/sprites/manager/manager_idle.png");
    private final List<Image> managerAlertFrames = SpriteLoader.loadStrip("/assets/sprites/manager/manager_alert.png");
    private final List<Image> managerWalkFrames = SpriteLoader.loadStrip("/assets/sprites/manager/manager_walk.png");
    private final List<Image> managerChaseFrames = SpriteLoader.loadStrip("/assets/sprites/manager/manager_chase.png");
    private final Image officeTileset = SpriteLoader.loadRaw("/assets/tiles/office/office_tileset.png");
    private final Image openSpaceFloorTile = cropImage(officeTileset, 28, 862, 94, 94);
    private final Image meetingFloorTile = cropImage(officeTileset, 363, 862, 94, 94);
    private final Image kitchenFloorTile = cropImage(officeTileset, 586, 862, 94, 94);
    private final Image directorFloorTile = cropImage(officeTileset, 698, 862, 94, 94);
    private final Image deskSprite = SpriteLoader.loadMaskedSingle("/assets/sprites/props/desk.png");
    private final Image keyboardSprite = SpriteLoader.loadMaskedSingle("/assets/sprites/props/keyboard.png");
    private final Image mugSprite = SpriteLoader.loadMaskedSingle("/assets/sprites/props/mug.png");
    private final Image mugSpilledSprite = SpriteLoader.loadMaskedSingle("/assets/sprites/props/mug_spilled.png");
    private final Image wifiRouterSprite = SpriteLoader.loadMaskedSingle("/assets/sprites/props/wifi_router.png");
    private final Image boxSprite = SpriteLoader.loadMaskedSingle("/assets/sprites/props/box.png");
    private final Image papersSprite = SpriteLoader.loadMaskedSingle("/assets/sprites/props/papers.png");
    private final Image papersScatteredSprite = SpriteLoader.loadMaskedSingle("/assets/sprites/props/papers_scattered.png");
    private final Image meetingTableSprite = SpriteLoader.loadMaskedSingle("/assets/sprites/props/meeting_table.png");
    private final Image chairSprite = SpriteLoader.loadMaskedSingle("/assets/sprites/props/chair.png");
    private final Image cabinetSprite = SpriteLoader.loadMaskedSingle("/assets/sprites/props/cabinet.png");
    private final Image plantSprite = SpriteLoader.loadMaskedSingle("/assets/sprites/props/plant.png");
    private final Image sleepingKeyboardSprite = SpriteLoader.loadMaskedSingle("/assets/sprites/cat/cat_sleep_keyboard.png");

    private ManagerNpc manager = new ManagerNpc(managerSpawn.x(), managerSpawn.y(), managerPatrolPath);

    private AnimationTimer loop;
    private double chaosPercent;
    private double timeLeft = DAY_DURATION_SECONDS;
    private double managerPenaltyCooldown;
    private double comboTimer;
    private double meowCooldownRemaining;
    private double dangerZoneCreateCooldown;
    private double dangerExposureTimer;
    private double animationClock;
    private double shakeIntensity;
    private double shakePhase;
    private int comboCount;
    private int objectiveIndex;
    private ChaosObjective currentObjective;
    private boolean interactionHeld;
    private HideSpot activeHideSpot;
    private boolean dashHeld;
    private boolean meowHeld;
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
            meowCooldownRemaining = Math.max(0, meowCooldownRemaining - deltaSeconds);
            dangerZoneCreateCooldown = Math.max(0, dangerZoneCreateCooldown - deltaSeconds);
            shakeIntensity = Math.max(0, shakeIntensity - SHAKE_DECAY_PER_SECOND * deltaSeconds);
            shakePhase += deltaSeconds * 34;
            if (comboTimer == 0) {
                comboCount = 0;
            }
            handleDashInput();
            handleMeowInput();
            player.update(input, deltaSeconds, walls);
            handleInteractionAttempt();
            updateChaosEvents(deltaSeconds);
            updateDangerZones(deltaSeconds);
            updateFloatingTexts(deltaSeconds);
            updateIncidentFeed(deltaSeconds);
            updateNpcs(deltaSeconds);
            applyDangerZonePressure(deltaSeconds);
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

    private void handleDashInput() {
        boolean dashPressed = input.isPressed(KeyCode.SHIFT);

        if (dashPressed && !dashHeld && gameState == GameState.PLAYING && player.tryStartDash(input)) {
            addIncident("Cat dash burst");
            addShake(2.5);
            floatingTexts.add(new FloatingText(
                    "Dash!",
                    player.centerX(),
                    player.y() - 6,
                    Color.web("#7dd3fc"),
                    0.9
            ));
        }

        dashHeld = dashPressed;
    }

    private void handleMeowInput() {
        boolean meowPressed = input.isPressed(KeyCode.SPACE);

        if (meowPressed && !meowHeld && gameState == GameState.PLAYING && meowCooldownRemaining <= 0) {
            meowCooldownRemaining = MEOW_COOLDOWN_SECONDS;
            chaosPercent = Math.min(100, chaosPercent + MEOW_CHAOS_GAIN);
            chaosEvents.add(new ChaosEvent(
                    "cat meow",
                    player.centerX(),
                    player.centerY(),
                    MEOW_EVENT_RADIUS,
                    MEOW_EVENT_SEVERITY,
                    1.8
            ));
            addIncident("Cat meowed to lure attention");
            addShake(1.8);
            floatingTexts.add(new FloatingText(
                    "Meow!",
                    player.centerX(),
                    player.y() - 20,
                    Color.web("#f9a8d4"),
                    0.9
            ));
        }

        meowHeld = meowPressed;
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
        meowCooldownRemaining = 0;
        dangerZoneCreateCooldown = 0;
        dangerExposureTimer = 0;
        comboCount = 0;
        animationClock = 0;
        shakeIntensity = 0;
        shakePhase = 0;
        objectiveIndex = 0;
        currentObjective = nextObjective();
        interactionHeld = false;
        dashHeld = false;
        meowHeld = false;
        endMessage = "";
        player.setPosition(PLAYER_RESPAWN.x(), PLAYER_RESPAWN.y());
        player.setHidden(false);
        activeHideSpot = null;
        chaosEvents.clear();
        dangerZones.clear();
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

    private void updateDangerZones(double deltaSeconds) {
        dangerZones.forEach(zone -> zone.update(deltaSeconds));
        dangerZones.removeIf(zone -> !zone.isActive());
    }

    private void updateIncidentFeed(double deltaSeconds) {
        incidentFeed.forEach(entry -> entry.update(deltaSeconds));
        incidentFeed.removeIf(entry -> !entry.isAlive());
    }

    private void updateNpcs(double deltaSeconds) {
        for (EmployeeNpc employee : employees) {
            employee.update(deltaSeconds, player, strongestEventNear(employee.x(), employee.y(), 165));
        }
        ChaosEvent managerEvent = strongestEventNear(manager.x(), manager.y(), 260);
        manager.update(deltaSeconds, player, managerEvent, currentChaosPressure());

        if (managerEvent != null
                && manager.mode() == ManagerNpc.Mode.INVESTIGATING
                && manager.distanceTo(managerEvent.x(), managerEvent.y()) < 34
                && dangerZoneCreateCooldown <= 0) {
            dangerZoneCreateCooldown = 2.2;
            dangerZones.add(new DangerZone(
                    managerEvent.x(),
                    managerEvent.y(),
                    DANGER_ZONE_RADIUS,
                    DANGER_ZONE_DURATION_SECONDS
            ));
            addIncident("Manager marked a danger zone");
            floatingTexts.add(new FloatingText(
                    "Risk zone!",
                    managerEvent.x(),
                    managerEvent.y() - 18,
                    Color.web("#fca5a5"),
                    1.1
            ));
        }
    }

    private void applyDangerZonePressure(double deltaSeconds) {
        if (player.isHidden()) {
            dangerExposureTimer = 0;
            return;
        }

        boolean insideDangerZone = dangerZones.stream()
                .anyMatch(zone -> zone.contains(player.centerX(), player.centerY()));

        if (!insideDangerZone) {
            dangerExposureTimer = 0;
            return;
        }

        dangerExposureTimer += deltaSeconds;
        if (dangerExposureTimer >= DANGER_ZONE_PENALTY_INTERVAL) {
            dangerExposureTimer = 0;
            chaosPercent = Math.max(0, chaosPercent - 2);
            timeLeft = Math.max(0, timeLeft - 2);
            floatingTexts.add(new FloatingText(
                    "Too exposed!",
                    player.centerX(),
                    player.y() - 10,
                    Color.web("#fda4af"),
                    0.9
            ));
        }
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
        drawDangerZones(gc);
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

        fillAreaWithTile(gc, openSpaceFloorTile, 40, 70, 520, 300, 64);
        fillAreaWithTile(gc, meetingFloorTile, 590, 70, 300, 220, 64);
        fillAreaWithTile(gc, kitchenFloorTile, 920, 70, 300, 220, 64);
        fillAreaWithTile(gc, directorFloorTile, 780, 330, 440, 260, 64);

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

        gc.setStroke(Color.rgb(59, 73, 97, 0.4));
        gc.setLineWidth(2);
        for (Rect wall : walls) {
            gc.strokeRoundRect(wall.x(), wall.y(), wall.width(), wall.height(), 12, 12);
        }

        drawFurniture(gc);
    }

    private void drawInteractions(GraphicsContext gc) {
        for (ChaosInteraction interaction : interactions) {
            Image sprite = interactionSprite(interaction);
            if (sprite != null) {
                drawCenteredSprite(gc, sprite, interaction.x(), interaction.y(), interactionSize(interaction));
            } else {
                gc.setFill(interaction.color());
                gc.fillRoundRect(interaction.x() - 16, interaction.y() - 16, 32, 32, 10, 10);
            }

            if (!interaction.canTrigger()) {
                gc.setFill(Color.rgb(30, 30, 30, 0.22));
                gc.fillOval(interaction.x() - 30, interaction.y() - 30, 60, 60);
            }
        }
    }

    private void drawHideSpots(GraphicsContext gc) {
        for (HideSpot hideSpot : hideSpots) {
            drawCenteredSprite(gc, boxSprite, hideSpot.x(), hideSpot.y(), PROP_MEDIUM);
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

    private void drawDangerZones(GraphicsContext gc) {
        for (DangerZone zone : dangerZones) {
            gc.setFill(Color.rgb(239, 68, 68, 0.12));
            gc.fillOval(
                    zone.x() - zone.radius(),
                    zone.y() - zone.radius(),
                    zone.radius() * 2,
                    zone.radius() * 2
            );
            gc.setStroke(Color.rgb(248, 113, 113, Math.min(0.5, zone.timeLeft() / DANGER_ZONE_DURATION_SECONDS)));
            gc.setLineWidth(3);
            gc.strokeOval(
                    zone.x() - zone.radius(),
                    zone.y() - zone.radius(),
                    zone.radius() * 2,
                    zone.radius() * 2
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
        boolean insideDangerZone = dangerZones.stream()
                .anyMatch(zone -> zone.contains(player.centerX(), player.centerY()));

        gc.setFill(Color.rgb(17, 24, 39, 0.88));
        gc.fillRoundRect(20, 588, 338, 112, 18, 18);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 16));
        gc.fillText("Office Cat", 36, 614);

        gc.setFont(Font.font("Verdana", 13));
        gc.fillText(String.format("Time %02d:%02d", (int) timeLeft / 60, (int) timeLeft % 60), 36, 638);
        gc.fillText(player.isHidden() ? "Status Hidden in a box" : "Status Causing trouble", 36, 658);
        gc.fillText(comboCount > 1 && comboTimer > 0
                ? String.format("Combo x%d %.1fs", comboCount, comboTimer)
                : "Chain actions for combo", 36, 678);
        gc.fillText(player.isDashReady()
                ? "Dash Ready [Shift]"
                : String.format("Dash %.1fs", player.dashCooldownRemaining()), 196, 678);
        gc.fillText(insideDangerZone
                ? "Danger Zone Active"
                : (meowCooldownRemaining <= 0
                ? "Meow Ready [Space]"
                : String.format("Meow %.1fs", meowCooldownRemaining)), 36, 696);

        gc.setFill(Color.rgb(255, 255, 255, 0.2));
        gc.fillRoundRect(36, 548, 322, 18, 10, 10);
        gc.setFill(Color.web("#ef4444"));
        gc.fillRoundRect(36, 548, 3.22 * chaosPercent, 18, 10, 10);

        gc.setFill(Color.web("#111827"));
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 13));
        gc.fillText(String.format("Chaos %.0f%%", chaosPercent), 162, 562);

        gc.setFill(Color.rgb(17, 24, 39, 0.88));
        gc.fillRoundRect(1022, 588, 218, 112, 18, 18);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Verdana", 12));
        gc.fillText("Manager", 1038, 614);
        gc.fillText(manager.statusText(), 1038, 636);
        gc.fillText(String.format("Pressure %.0f%%", currentChaosPressure() * 50), 1038, 658);
        gc.fillText("WASD move  E interact", 1038, 676);
        gc.fillText("Shift dash  Space meow", 1038, 692);

        gc.setFill(Color.rgb(17, 24, 39, 0.9));
        gc.fillRoundRect(378, 620, 340, 80, 18, 18);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 13));
        gc.fillText("Objective", 394, 644);
        gc.setFont(Font.font("Verdana", 12));
        if (currentObjective != null) {
            gc.fillText(currentObjective.title(), 394, 666);
            gc.fillText(currentObjective.description(), 394, 686);
            gc.fillText(String.format("+%.0f chaos", currentObjective.bonusChaos()), 636, 644);
        }
    }

    private void drawIncidentFeed(GraphicsContext gc) {
        gc.setFill(Color.rgb(17, 24, 39, 0.88));
        gc.fillRoundRect(736, 620, 268, 80, 18, 18);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 13));
        gc.fillText("Incidents", 752, 644);
        gc.setFont(Font.font("Verdana", 11));

        int line = 0;
        for (IncidentFeedEntry entry : incidentFeed) {
            if (line >= 3) {
                break;
            }
            gc.setFill(Color.WHITE.deriveColor(0, 1, 1, entry.alpha()));
            gc.fillText("- " + entry.text(), 752, 664 + line * 12);
            line++;
        }
    }

    private void drawPrompt(GraphicsContext gc, ChaosInteraction interaction) {
        gc.setFill(Color.rgb(17, 24, 39, 0.92));
        gc.fillRoundRect(430, 656, 420, 34, 14, 14);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 13));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText("[E] " + interaction.promptText(), WIDTH / 2.0, 673);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.BASELINE);
    }

    private void drawHideSpotPrompt(GraphicsContext gc, HideSpot hideSpot) {
        gc.setFill(Color.rgb(17, 24, 39, 0.92));
        gc.fillRoundRect(430, 656, 420, 34, 14, 14);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 13));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText("[E] Hide in " + hideSpot.label(), WIDTH / 2.0, 673);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.BASELINE);
    }

    private void drawHidePrompt(GraphicsContext gc) {
        gc.setFill(Color.rgb(17, 24, 39, 0.92));
        gc.fillRoundRect(430, 656, 420, 34, 14, 14);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 13));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText("[E] Sneak out of the box", WIDTH / 2.0, 673);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.BASELINE);
    }

    private void drawGameOverOverlay(GraphicsContext gc) {
        gc.setFill(Color.rgb(0, 0, 0, 0.28));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        gc.setFill(Color.rgb(255, 248, 235, 0.95));
        gc.fillRoundRect(380, 240, 520, 156, 24, 24);

        gc.setFill(Color.web("#111827"));
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 24));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(endMessage, WIDTH / 2.0, 292);

        gc.setFont(Font.font("Verdana", 16));
        gc.fillText(String.format("Final chaos %.0f%%", chaosPercent), WIDTH / 2.0, 330);
        gc.fillText("[R] Restart   [Enter] Menu", WIDTH / 2.0, 360);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawMenuOverlay(GraphicsContext gc) {
        gc.setFill(Color.rgb(10, 15, 25, 0.28));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        gc.setFill(Color.rgb(255, 248, 235, 0.96));
        gc.fillRoundRect(360, 170, 560, 240, 26, 26);

        gc.setFill(Color.web("#111827"));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 28));
        gc.fillText("Office Cat: Chaos Manager", WIDTH / 2.0, 225);

        gc.setFont(Font.font("Verdana", 17));
        gc.fillText("Turn a productive office into a furry disaster zone.", WIDTH / 2.0, 264);

        gc.setFont(Font.font("Verdana", 14));
        gc.fillText("Reach 100% chaos before the work day ends.", WIDTH / 2.0, 305);
        gc.fillText("Avoid the manager and chain mischief for combos.", WIDTH / 2.0, 330);
        gc.fillText("Use Shift to dash out of trouble.", WIDTH / 2.0, 352);
        gc.fillText("Use Space to lure NPCs with a meow.", WIDTH / 2.0, 374);
        gc.fillText("[Enter] Start run", WIDTH / 2.0, 398);
        gc.fillText("WASD move   E interact   Shift dash   Space meow   Esc pause", WIDTH / 2.0, 420);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawPauseOverlay(GraphicsContext gc) {
        gc.setFill(Color.rgb(0, 0, 0, 0.22));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        gc.setFill(Color.rgb(255, 248, 235, 0.95));
        gc.fillRoundRect(440, 250, 400, 128, 22, 22);

        gc.setFill(Color.web("#111827"));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 24));
        gc.fillText("Paused", WIDTH / 2.0, 295);

        gc.setFont(Font.font("Verdana", 16));
        gc.fillText("[Esc / P] Resume", WIDTH / 2.0, 330);
        gc.fillText("[R] Restart run", WIDTH / 2.0, 356);
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

    private void drawFurniture(GraphicsContext gc) {
        drawCenteredSprite(gc, deskSprite, 360, 157, PROP_XL);
        drawCenteredSprite(gc, deskSprite, 360, 252, PROP_XL);
        drawCenteredSprite(gc, meetingTableSprite, 735, 180, 290);
        drawCenteredSprite(gc, cabinetSprite, 1040, 150, PROP_LARGE);
        drawCenteredSprite(gc, deskSprite, 1045, 430, PROP_XL);
        drawCenteredSprite(gc, cabinetSprite, 1095, 520, PROP_LARGE);
        drawCenteredSprite(gc, cabinetSprite, 250, 470, PROP_XL);
        drawCenteredSprite(gc, cabinetSprite, 260, 578, PROP_LARGE);
        drawCenteredSprite(gc, cabinetSprite, 595, 578, PROP_MEDIUM);

        drawCenteredSprite(gc, chairSprite, 170, 175, PROP_SMALL);
        drawCenteredSprite(gc, chairSprite, 170, 272, PROP_SMALL);
        drawCenteredSprite(gc, chairSprite, 980, 525, PROP_SMALL);

        drawCenteredSprite(gc, plantSprite, 530, 330, PROP_SMALL);
        drawCenteredSprite(gc, plantSprite, 1210, 270, PROP_SMALL);
        drawCenteredSprite(gc, plantSprite, 795, 575, PROP_SMALL);
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
        if (player.isDashing()) {
            return catPounceSprite;
        }
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

    private Image interactionSprite(ChaosInteraction interaction) {
        return switch (interaction.id()) {
            case "keyboard" -> interaction.canTrigger() ? keyboardSprite : sleepingKeyboardSprite;
            case "mug" -> interaction.canTrigger() ? mugSprite : mugSpilledSprite;
            case "wifi" -> wifiRouterSprite;
            case "papers" -> interaction.canTrigger() ? papersSprite : papersScatteredSprite;
            case "meeting" -> null;
            default -> null;
        };
    }

    private double interactionSize(ChaosInteraction interaction) {
        return switch (interaction.id()) {
            case "keyboard" -> 94;
            case "mug", "papers", "wifi" -> PROP_SMALL;
            default -> PROP_SMALL;
        };
    }

    private Image frameAt(List<Image> frames, double framesPerSecond) {
        int index = (int) (animationClock * framesPerSecond) % frames.size();
        return frames.get(index);
    }

    private Image cropImage(Image image, int x, int y, int width, int height) {
        return new WritableImage(image.getPixelReader(), x, y, width, height);
    }

    private void fillAreaWithTile(GraphicsContext gc, Image tile, double x, double y, double width, double height, double tileDrawSize) {
        for (double drawY = y; drawY < y + height; drawY += tileDrawSize) {
            for (double drawX = x; drawX < x + width; drawX += tileDrawSize) {
                double actualWidth = Math.min(tileDrawSize, x + width - drawX);
                double actualHeight = Math.min(tileDrawSize, y + height - drawY);
                gc.drawImage(tile, drawX, drawY, actualWidth, actualHeight);
            }
        }
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
