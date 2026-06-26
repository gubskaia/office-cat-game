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
    public static final int WORLD_WIDTH = 1960;
    public static final int WORLD_HEIGHT = 1120;

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
    private static final double PRODUCTIVITY_CASCADE_THRESHOLD = 0.45;
    private static final double PRODUCTIVITY_CASCADE_RATE = 1.35;
    private static final double ZOOMIES_DURATION_SECONDS = 7.0;
    private static final double ZOOMIES_SPEED_MULTIPLIER = 1.45;
    private static final double WIFI_OUTAGE_DURATION_SECONDS = 10.0;
    private static final double MUG_SPILL_DURATION_SECONDS = 8.0;
    private static final double PAPERS_MESS_DURATION_SECONDS = 9.0;
    private static final double MEETING_ALERT_DURATION_SECONDS = 6.5;
    private static final double SHAKE_DECAY_PER_SECOND = 3.4;
    private static final Point PLAYER_RESPAWN = new Point(170, 190);
    private static final double PLAYER_DRAW_SIZE = 78;
    private static final double NPC_DRAW_SIZE = 72;
    private static final double PROP_SMALL = 54;
    private static final double PROP_MEDIUM = 82;
    private static final double PROP_LARGE = 190;
    private static final double PROP_XL = 250;
    private static final Rect OPEN_SPACE_ROOM = new Rect(80, 100, 760, 380);
    private static final Rect MEETING_ROOM = new Rect(920, 100, 420, 290);
    private static final Rect KITCHEN_ROOM = new Rect(1420, 100, 380, 290);
    private static final Rect DIRECTOR_ROOM = new Rect(1080, 520, 720, 320);
    private static final double OPEN_SPACE_DOOR_X = 750;
    private static final double MEETING_DOOR_X = 1084;
    private static final double KITCHEN_DOOR_X = 1496;
    private static final double DIRECTOR_DOOR_X = 1168;

    private final Canvas canvas = new Canvas(WIDTH, HEIGHT);
    private final InputState input = new InputState();
    private final PlayerCat player = new PlayerCat(PLAYER_RESPAWN.x(), PLAYER_RESPAWN.y());
    private final List<Rect> walls = new ArrayList<>();
    private final List<ChaosInteraction> interactions = new ArrayList<>();
    private final List<CatSupportSpot> supportSpots = new ArrayList<>();
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
    private final List<Point> investigationNodes = List.of(
            new Point(334, 244),
            new Point(334, 386),
            new Point(OPEN_SPACE_DOOR_X, 520),
            new Point(1120, 314),
            new Point(MEETING_DOOR_X, 520),
            new Point(1694, 246),
            new Point(KITCHEN_DOOR_X, 520),
            new Point(1288, 742),
            new Point(1512, 604),
            new Point(DIRECTOR_DOOR_X, 520)
    );
    private final List<Point> managerPatrolPath = List.of(
            new Point(250, 520),
            new Point(760, 520),
            new Point(OPEN_SPACE_DOOR_X, 520),
            new Point(MEETING_DOOR_X, 520),
            new Point(KITCHEN_DOOR_X, 520),
            new Point(1660, 520),
            new Point(1660, 700),
            new Point(1360, 700),
            new Point(DIRECTOR_DOOR_X, 520),
            new Point(900, 520),
            new Point(500, 520),
            new Point(190, 300)
    );

    private final Point managerSpawn = new Point(1020, 190);
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
    private double productivityCascadeTicker;
    private double keyboardNapTimer;
    private double wifiOutageTimer;
    private double mugSpillTimer;
    private double papersMessTimer;
    private double meetingAlertTimer;
    private double stagedInteractionTimer;
    private double animationClock;
    private double shakeIntensity;
    private double shakePhase;
    private double officeProductivity = 1.0;
    private Point keyboardNapExitPoint = new Point(0, 0);
    private ChaosInteraction stagedInteraction;
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
            wifiOutageTimer = Math.max(0, wifiOutageTimer - deltaSeconds);
            mugSpillTimer = Math.max(0, mugSpillTimer - deltaSeconds);
            papersMessTimer = Math.max(0, papersMessTimer - deltaSeconds);
            meetingAlertTimer = Math.max(0, meetingAlertTimer - deltaSeconds);
            updateStagedInteraction(deltaSeconds);
            updateKeyboardNap(deltaSeconds);
            shakeIntensity = Math.max(0, shakeIntensity - SHAKE_DECAY_PER_SECOND * deltaSeconds);
            shakePhase += deltaSeconds * 34;
            if (comboTimer == 0) {
                comboCount = 0;
            }
            if (!isPlayerLocked()) {
                handleDashInput();
                handleMeowInput();
                player.update(input, deltaSeconds, walls);
                handleInteractionAttempt();
            }
            updateChaosEvents(deltaSeconds);
            updateDangerZones(deltaSeconds);
            updateFloatingTexts(deltaSeconds);
            updateIncidentFeed(deltaSeconds);
            updateNpcs(deltaSeconds);
            updateOfficeProductivity(deltaSeconds);
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
        productivityCascadeTicker = 0;
        keyboardNapTimer = 0;
        wifiOutageTimer = 0;
        mugSpillTimer = 0;
        papersMessTimer = 0;
        meetingAlertTimer = 0;
        stagedInteractionTimer = 0;
        comboCount = 0;
        animationClock = 0;
        shakeIntensity = 0;
        shakePhase = 0;
        officeProductivity = 1.0;
        objectiveIndex = 0;
        currentObjective = nextObjective();
        interactionHeld = false;
        dashHeld = false;
        meowHeld = false;
        endMessage = "";
        player.setPosition(PLAYER_RESPAWN.x(), PLAYER_RESPAWN.y());
        player.setHidden(false);
        player.clearTemporaryEffects();
        activeHideSpot = null;
        keyboardNapExitPoint = new Point(0, 0);
        stagedInteraction = null;
        chaosEvents.clear();
        dangerZones.clear();
        floatingTexts.clear();
        incidentFeed.clear();

        for (ChaosInteraction interaction : interactions) {
            interaction.reset();
        }
        for (CatSupportSpot supportSpot : supportSpots) {
            supportSpot.reset();
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
                CatSupportSpot nearestSupportSpot = getNearestSupportSpot();

                if (nearestHideSpot != null
                        && shouldPreferHideSpot(nearestHideSpot, nearest, nearestSupportSpot)) {
                    enterHideSpot(nearestHideSpot);
                } else if (nearestSupportSpot != null
                        && shouldPreferSupportSpot(nearestSupportSpot, nearest, nearestHideSpot)
                        && nearestSupportSpot.canUse()) {
                    useSupportSpot(nearestSupportSpot);
                } else if (nearest != null && nearest.canTrigger()) {
                    nearest.trigger();
                    if ("keyboard".equals(nearest.id())) {
                        startKeyboardNap(nearest);
                        completeInteraction(nearest);
                    } else if ("wifi".equals(nearest.id())) {
                        startWifiOutage(nearest);
                        completeInteraction(nearest);
                    } else if (requiresStaging(nearest)) {
                        startStagedInteraction(nearest);
                    } else {
                        completeInteraction(nearest);
                    }
                }
            }
        }
        interactionHeld = pressed;
    }

    private boolean requiresStaging(ChaosInteraction interaction) {
        return switch (interaction.id()) {
            case "mug", "papers", "meeting" -> true;
            default -> false;
        };
    }

    private void startStagedInteraction(ChaosInteraction interaction) {
        stagedInteraction = interaction;
        stagedInteractionTimer = switch (interaction.id()) {
            case "mug" -> 0.55;
            case "papers" -> 0.72;
            case "meeting" -> 0.6;
            default -> 0;
        };
        floatingTexts.add(new FloatingText(
                switch (interaction.id()) {
                    case "mug" -> "Lining up the mug...";
                    case "papers" -> "Paws on the paperwork...";
                    case "meeting" -> "Timing the meow...";
                    default -> "Chaos brewing...";
                },
                interaction.x(),
                interaction.y() - 48,
                Color.web("#fde68a"),
                0.8
        ));
    }

    private void updateStagedInteraction(double deltaSeconds) {
        if (stagedInteraction == null) {
            stagedInteractionTimer = 0;
            return;
        }

        stagedInteractionTimer = Math.max(0, stagedInteractionTimer - deltaSeconds);
        if (stagedInteractionTimer == 0) {
            if ("mug".equals(stagedInteraction.id())) {
                startMugSpill(stagedInteraction);
            } else if ("papers".equals(stagedInteraction.id())) {
                startPaperMess(stagedInteraction);
            } else if ("meeting".equals(stagedInteraction.id())) {
                startMeetingDisruption(stagedInteraction);
            }
            completeInteraction(stagedInteraction);
            stagedInteraction = null;
        }
    }

    private void completeInteraction(ChaosInteraction interaction) {
        double chaosGain = applyCombo(interaction);
        chaosPercent = Math.min(100, chaosPercent + chaosGain);
        chaosEvents.add(new ChaosEvent(
                interaction.eventLabel(),
                interaction.x(),
                interaction.y(),
                interaction.eventRadius(),
                interaction.eventSeverity(),
                EVENT_DURATION_SECONDS
        ));
        addIncident("Chaos: " + interaction.eventLabel());
        floatingTexts.add(new FloatingText(
                String.format("+%.0f chaos", chaosGain),
                interaction.x(),
                interaction.y() - 18,
                Color.web("#ef4444"),
                1.2
        ));
        if (comboCount >= 2) {
            floatingTexts.add(new FloatingText(
                    "Combo x" + comboCount,
                    interaction.x(),
                    interaction.y() - 40,
                    Color.web("#fbbf24"),
                    1.4
            ));
        }
        addShake(interaction.eventSeverity() >= 7 ? 7.5 : 4.0);
        checkObjectiveCompletion(interaction);
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

    private boolean shouldPreferHideSpot(HideSpot hideSpot, ChaosInteraction interaction, CatSupportSpot supportSpot) {
        if (hideSpot == null) {
            return false;
        }
        double hideDistance = hideSpot.distanceTo(player.centerX(), player.centerY());
        return hideDistance <= distanceToInteraction(interaction)
                && hideDistance <= distanceToSupportSpot(supportSpot);
    }

    private boolean shouldPreferSupportSpot(CatSupportSpot supportSpot, ChaosInteraction interaction, HideSpot hideSpot) {
        if (supportSpot == null) {
            return false;
        }
        double supportDistance = supportSpot.distanceTo(player.centerX(), player.centerY());
        return supportDistance < distanceToInteraction(interaction)
                && supportDistance < distanceToHideSpot(hideSpot);
    }

    private double distanceToInteraction(ChaosInteraction interaction) {
        return interaction == null
                ? Double.MAX_VALUE
                : interaction.distanceTo(player.centerX(), player.centerY());
    }

    private double distanceToSupportSpot(CatSupportSpot supportSpot) {
        return supportSpot == null
                ? Double.MAX_VALUE
                : supportSpot.distanceTo(player.centerX(), player.centerY());
    }

    private double distanceToHideSpot(HideSpot hideSpot) {
        return hideSpot == null
                ? Double.MAX_VALUE
                : hideSpot.distanceTo(player.centerX(), player.centerY());
    }

    private void useSupportSpot(CatSupportSpot supportSpot) {
        supportSpot.use();
        switch (supportSpot.id()) {
            case "snack" -> {
                meowCooldownRemaining = 0;
                player.resetDashCooldown();
                dangerExposureTimer = 0;
                floatingTexts.add(new FloatingText(
                        "Cooldowns refreshed!",
                        supportSpot.x(),
                        supportSpot.y() - 18,
                        Color.web("#fde68a"),
                        1.4
                ));
                addIncident("Kitchen snack restored cat energy");
            }
            case "sunbeam" -> {
                player.activateZoomies(ZOOMIES_DURATION_SECONDS, ZOOMIES_SPEED_MULTIPLIER);
                floatingTexts.add(new FloatingText(
                        "Zoomies activated!",
                        supportSpot.x(),
                        supportSpot.y() - 18,
                        Color.web("#fcd34d"),
                        1.4
                ));
                addIncident("Sunbeam granted temporary zoomies");
            }
            default -> {
                return;
            }
        }
        addShake(2.2);
    }

    private void startKeyboardNap(ChaosInteraction interaction) {
        keyboardNapTimer = 1.45;
        keyboardNapExitPoint = new Point(interaction.x() - 210, interaction.y() + 18);
        floatingTexts.add(new FloatingText(
                "Power nap!",
                interaction.x(),
                interaction.y() - 58,
                Color.web("#fde68a"),
                1.0
        ));
    }

    private void startWifiOutage(ChaosInteraction interaction) {
        wifiOutageTimer = WIFI_OUTAGE_DURATION_SECONDS;
        addIncident("Office Wi-Fi outage triggered");
        floatingTexts.add(new FloatingText(
                "Wi-Fi offline!",
                interaction.x(),
                interaction.y() - 44,
                Color.web("#fca5a5"),
                1.4
        ));
        addShake(5.0);
    }

    private void startMugSpill(ChaosInteraction interaction) {
        mugSpillTimer = MUG_SPILL_DURATION_SECONDS;
        addIncident("Coffee spilled across the kitchen");
        floatingTexts.add(new FloatingText(
                "Coffee flood!",
                interaction.x(),
                interaction.y() - 34,
                Color.web("#fdba74"),
                1.2
        ));
    }

    private void startPaperMess(ChaosInteraction interaction) {
        papersMessTimer = PAPERS_MESS_DURATION_SECONDS;
        addIncident("Director papers launched into chaos");
        floatingTexts.add(new FloatingText(
                "Paper storm!",
                interaction.x(),
                interaction.y() - 34,
                Color.web("#a7f3d0"),
                1.2
        ));
    }

    private void startMeetingDisruption(ChaosInteraction interaction) {
        meetingAlertTimer = MEETING_ALERT_DURATION_SECONDS;
        addIncident("Meeting room thrown into confusion");
        floatingTexts.add(new FloatingText(
                "Meeting ruined!",
                interaction.x(),
                interaction.y() - 34,
                Color.web("#c4b5fd"),
                1.2
        ));
        addShake(3.4);
    }

    private void updateKeyboardNap(double deltaSeconds) {
        if (keyboardNapTimer <= 0) {
            return;
        }
        keyboardNapTimer = Math.max(0, keyboardNapTimer - deltaSeconds);
        if (keyboardNapTimer == 0) {
            player.setPosition(keyboardNapExitPoint.x(), keyboardNapExitPoint.y());
        }
    }

    private boolean isPlayerLocked() {
        return keyboardNapTimer > 0 || stagedInteractionTimer > 0;
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
            employee.update(
                    deltaSeconds,
                    player,
                    resolveNpcTarget(employee.x(), employee.y(), player.centerX(), player.centerY()),
                    resolveEventForNpc(employee.x(), employee.y(), strongestEventNear(employee.x(), employee.y(), 165)),
                    officeAlertLevel(),
                    walls
            );
            if (employee.canReportCat(player)) {
                employee.markReportUsed();
                chaosEvents.add(new ChaosEvent(
                        "employee report",
                        employee.x(),
                        employee.y(),
                        220,
                        6.0,
                        2.6
                ));
                addIncident(employee.name() + " reported the cat");
                floatingTexts.add(new FloatingText(
                        "Manager!",
                        employee.x(),
                        employee.y() - 22,
                        Color.web("#fca5a5"),
                        1.0
                ));
            }
        }
        ChaosEvent managerEvent = resolveManagerEvent(strongestEventNear(manager.x(), manager.y(), 260));
        manager.update(
                deltaSeconds,
                player,
                resolveNpcTarget(manager.x(), manager.y(), player.centerX(), player.centerY()),
                managerEvent,
                currentChaosPressure(),
                walls
        );

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

    private void updateOfficeProductivity(double deltaSeconds) {
        if (employees.isEmpty()) {
            officeProductivity = 1.0;
            return;
        }

        double totalProductivity = 0;
        for (EmployeeNpc employee : employees) {
            totalProductivity += employee.productivity();
        }
        officeProductivity = totalProductivity / employees.size();

        if (wifiOutageTimer > 0) {
            double outagePressure = 1.0 - (wifiOutageTimer / WIFI_OUTAGE_DURATION_SECONDS) * 0.35;
            officeProductivity = Math.min(officeProductivity, Math.max(0.18, outagePressure * officeProductivity));
        }
        if (meetingAlertTimer > 0) {
            officeProductivity *= 0.92;
        }
        if (mugSpillTimer > 0) {
            officeProductivity *= 0.96;
        }
        if (papersMessTimer > 0) {
            officeProductivity *= 0.9;
        }

        if (officeProductivity > PRODUCTIVITY_CASCADE_THRESHOLD) {
            productivityCascadeTicker = 0;
            return;
        }

        double disruptionStrength = 1.0 - officeProductivity;
        chaosPercent = Math.min(100, chaosPercent + disruptionStrength * PRODUCTIVITY_CASCADE_RATE * deltaSeconds);

        productivityCascadeTicker += deltaSeconds;
        if (productivityCascadeTicker >= 2.4) {
            productivityCascadeTicker = 0;
            addIncident(String.format("Office slowdown: %.0f%% productivity", officeProductivity * 100));
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
        if (wifiOutageTimer > 0) {
            pressure += 0.35;
        }
        if (meetingAlertTimer > 0) {
            pressure += 0.18;
        }
        if (papersMessTimer > 0) {
            pressure += 0.14;
        }
        return Math.min(2.0, pressure);
    }

    private double officeAlertLevel() {
        double level = 0;
        if (wifiOutageTimer > 0) {
            level += wifiOutageTimer / WIFI_OUTAGE_DURATION_SECONDS;
        }
        if (meetingAlertTimer > 0) {
            level += 0.45 * (meetingAlertTimer / MEETING_ALERT_DURATION_SECONDS);
        }
        if (papersMessTimer > 0) {
            level += 0.35 * (papersMessTimer / PAPERS_MESS_DURATION_SECONDS);
        }
        if (mugSpillTimer > 0) {
            level += 0.2 * (mugSpillTimer / MUG_SPILL_DURATION_SECONDS);
        }
        return Math.min(1.0, level);
    }

    private ChaosEvent resolveEventForNpc(double actorX, double actorY, ChaosEvent event) {
        if (event == null) {
            return null;
        }
        Point target = resolveNpcTarget(actorX, actorY, event.x(), event.y());
        if (target.x() == event.x() && target.y() == event.y()) {
            return event;
        }
        return new ChaosEvent(event.label(), target.x(), target.y(), event.radius(), event.severity(), event.timeLeft());
    }

    private ChaosEvent resolveManagerEvent(ChaosEvent event) {
        if (event == null) {
            return null;
        }
        Point node = nearestInvestigationNode(event.x(), event.y());
        return new ChaosEvent(event.label(), node.x(), node.y(), event.radius(), event.severity(), event.timeLeft());
    }

    private Point resolveNpcTarget(double actorX, double actorY, double targetX, double targetY) {
        Rect actorRoom = roomAt(actorX, actorY);
        Rect targetRoom = roomAt(targetX, targetY);

        if (actorRoom == targetRoom) {
            return new Point(targetX, targetY);
        }

        if (actorRoom != null) {
            Point insideDoor = roomDoorInsidePoint(actorRoom);
            if (Math.hypot(actorX - insideDoor.x(), actorY - insideDoor.y()) > 26) {
                return insideDoor;
            }
            return roomDoorHallPoint(actorRoom);
        }

        if (targetRoom != null) {
            Point hallPoint = roomDoorHallPoint(targetRoom);
            if (Math.hypot(actorX - hallPoint.x(), actorY - hallPoint.y()) > 32) {
                return hallPoint;
            }
            return roomDoorInsidePoint(targetRoom);
        }

        return new Point(targetX, targetY);
    }

    private Rect roomAt(double x, double y) {
        if (contains(OPEN_SPACE_ROOM, x, y)) {
            return OPEN_SPACE_ROOM;
        }
        if (contains(MEETING_ROOM, x, y)) {
            return MEETING_ROOM;
        }
        if (contains(KITCHEN_ROOM, x, y)) {
            return KITCHEN_ROOM;
        }
        if (contains(DIRECTOR_ROOM, x, y)) {
            return DIRECTOR_ROOM;
        }
        return null;
    }

    private Point roomDoorHallPoint(Rect room) {
        if (room == OPEN_SPACE_ROOM) {
            return new Point(OPEN_SPACE_DOOR_X, OPEN_SPACE_ROOM.y() + OPEN_SPACE_ROOM.height() + 38);
        }
        if (room == MEETING_ROOM) {
            return new Point(MEETING_DOOR_X, MEETING_ROOM.y() + MEETING_ROOM.height() + 38);
        }
        if (room == KITCHEN_ROOM) {
            return new Point(KITCHEN_DOOR_X, KITCHEN_ROOM.y() + KITCHEN_ROOM.height() + 38);
        }
        return new Point(DIRECTOR_DOOR_X, DIRECTOR_ROOM.y() - 34);
    }

    private Point roomDoorInsidePoint(Rect room) {
        if (room == OPEN_SPACE_ROOM) {
            return new Point(OPEN_SPACE_DOOR_X, OPEN_SPACE_ROOM.y() + OPEN_SPACE_ROOM.height() - 40);
        }
        if (room == MEETING_ROOM) {
            return new Point(MEETING_DOOR_X, MEETING_ROOM.y() + MEETING_ROOM.height() - 40);
        }
        if (room == KITCHEN_ROOM) {
            return new Point(KITCHEN_DOOR_X, KITCHEN_ROOM.y() + KITCHEN_ROOM.height() - 40);
        }
        return new Point(DIRECTOR_DOOR_X, DIRECTOR_ROOM.y() + 40);
    }

    private Point nearestInvestigationNode(double x, double y) {
        Point best = new Point(x, y);
        double bestDistance = Double.MAX_VALUE;
        Rect eventRoom = roomAt(x, y);

        for (Point node : investigationNodes) {
            Rect nodeRoom = roomAt(node.x(), node.y());
            boolean compatibleRoom = eventRoom == null || nodeRoom == eventRoom || nodeRoom == null;
            if (!compatibleRoom) {
                continue;
            }
            double distance = Math.hypot(node.x() - x, node.y() - y);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = node;
            }
        }
        return best;
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

    private CatSupportSpot getNearestSupportSpot() {
        if (player.isHidden()) {
            return null;
        }

        CatSupportSpot result = null;
        double bestDistance = Double.MAX_VALUE;

        for (CatSupportSpot supportSpot : supportSpots) {
            double distance = supportSpot.distanceTo(player.centerX(), player.centerY());
            if (distance <= supportSpot.promptRadius() && distance < bestDistance) {
                bestDistance = distance;
                result = supportSpot;
            }
        }

        return result;
    }

    private void render() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setImageSmoothing(false);
        gc.save();
        applyWorldCamera(gc);
        drawOffice(gc);
        drawDangerZones(gc);
        drawChaosEvents(gc);
        drawInteractions(gc);
        drawSupportSpots(gc);
        drawHideSpots(gc);
        drawNpcs(gc);
        drawPlayer(gc);
        drawFloatingTexts(gc);
        gc.restore();
        drawTopRibbon(gc);
        drawHud(gc);
        drawIncidentFeed(gc);

        ChaosInteraction nearest = getNearestInteraction();
        CatSupportSpot nearestSupportSpot = getNearestSupportSpot();
        HideSpot nearestHideSpot = getNearestHideSpot();
        if (gameState == GameState.PLAYING) {
            if (player.isHidden()) {
                drawHidePrompt(gc);
            } else if (nearestHideSpot != null && shouldPreferHideSpot(nearestHideSpot, nearest, nearestSupportSpot)) {
                drawHideSpotPrompt(gc, nearestHideSpot);
            } else if (nearestSupportSpot != null && shouldPreferSupportSpot(nearestSupportSpot, nearest, nearestHideSpot)) {
                drawSupportPrompt(gc, nearestSupportSpot);
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

    private void applyWorldCamera(GraphicsContext gc) {
        double cameraX = cameraX();
        double cameraY = cameraY();
        double shakeX = 0;
        double shakeY = 0;
        if (shakeIntensity > 0) {
            shakeX = Math.sin(shakePhase) * shakeIntensity;
            shakeY = Math.cos(shakePhase * 1.6) * shakeIntensity * 0.7;
        }
        gc.translate(-cameraX + shakeX, -cameraY + shakeY);
    }

    private double cameraX() {
        return clamp(player.centerX() - WIDTH / 2.0, 0, WORLD_WIDTH - WIDTH);
    }

    private double cameraY() {
        return clamp(player.centerY() - HEIGHT / 2.0, 0, WORLD_HEIGHT - HEIGHT);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void drawOffice(GraphicsContext gc) {
        gc.setFill(Color.web("#f3ecdf"));
        gc.fillRect(0, 0, WORLD_WIDTH, WORLD_HEIGHT);
        gc.setFill(Color.rgb(221, 214, 196, 0.42));
        gc.fillRect(0, 900, WORLD_WIDTH, 220);
        gc.setFill(Color.rgb(8, 10, 18, 0.05));
        for (double x = 0; x < WORLD_WIDTH; x += 140) {
            gc.fillRect(x, 0, 2, WORLD_HEIGHT);
        }
        drawCorridors(gc);

        drawGridFloor(gc, OPEN_SPACE_ROOM, Color.web("#8f949f"), Color.web("#d7dae0"), 40);
        drawMeetingFloor(gc, MEETING_ROOM);
        drawKitchenFloor(gc, KITCHEN_ROOM);
        drawWoodFloor(gc, DIRECTOR_ROOM, Color.web("#8c5a2e"), Color.web("#c58c4c"));
        drawRoomDepth(gc, OPEN_SPACE_ROOM);
        drawRoomDepth(gc, MEETING_ROOM);
        drawRoomDepth(gc, KITCHEN_ROOM);
        drawRoomDepth(gc, DIRECTOR_ROOM);

        drawRoomFrame(gc, OPEN_SPACE_ROOM, Color.web("#475569"));
        drawRoomFrame(gc, MEETING_ROOM, Color.web("#475569"));
        drawRoomFrame(gc, KITCHEN_ROOM, Color.web("#475569"));
        drawRoomFrame(gc, DIRECTOR_ROOM, Color.web("#475569"));

        drawRoomBadge(gc, OPEN_SPACE_ROOM.x() + 18, OPEN_SPACE_ROOM.y() + 12, "OPEN SPACE", Color.web("#60a5fa"));
        drawRoomBadge(gc, MEETING_ROOM.x() + 18, MEETING_ROOM.y() + 12, "MEETING ROOM", Color.web("#8b5cf6"));
        drawRoomBadge(gc, KITCHEN_ROOM.x() + 18, KITCHEN_ROOM.y() + 12, "KITCHEN", Color.web("#f59e0b"));
        drawRoomBadge(gc, DIRECTOR_ROOM.x() + 18, DIRECTOR_ROOM.y() + 12, "DIRECTOR'S OFFICE", Color.web("#10b981"));
        drawDoorway(gc, OPEN_SPACE_DOOR_X, OPEN_SPACE_ROOM.y() + OPEN_SPACE_ROOM.height(), false);
        drawDoorway(gc, MEETING_DOOR_X, MEETING_ROOM.y() + MEETING_ROOM.height(), false);
        drawDoorway(gc, KITCHEN_DOOR_X, KITCHEN_ROOM.y() + KITCHEN_ROOM.height(), false);
        drawDoorway(gc, DIRECTOR_DOOR_X, DIRECTOR_ROOM.y(), true);
        if (wifiOutageTimer > 0) {
            gc.setFill(Color.rgb(59, 130, 246, 0.05 + 0.05 * Math.sin(animationClock * 10)));
            gc.fillRect(0, 0, WORLD_WIDTH, WORLD_HEIGHT);
        }

        gc.setStroke(Color.rgb(59, 73, 97, 0.4));
        gc.setLineWidth(2);
        for (Rect wall : walls) {
            gc.strokeRoundRect(wall.x(), wall.y(), wall.width(), wall.height(), 12, 12);
        }

        drawFurniture(gc);
        drawInteractionAftermath(gc);
    }

    private void drawInteractionAftermath(GraphicsContext gc) {
        if (mugSpillTimer > 0) {
            gc.setFill(Color.rgb(92, 58, 28, 0.28 + 0.12 * Math.sin(animationClock * 7)));
            gc.fillOval(1650, 212, 74, 30);
        }
        if (papersMessTimer > 0) {
            gc.setStroke(Color.rgb(240, 249, 255, 0.75));
            gc.setLineWidth(2);
            for (int i = 0; i < 5; i++) {
                double offset = i * 18;
                gc.strokeLine(1470 + offset, 560 + Math.sin(animationClock * 4 + i) * 12, 1490 + offset, 580 + Math.cos(animationClock * 4 + i) * 10);
                gc.strokeLine(1500 + offset, 590 + Math.cos(animationClock * 5 + i) * 10, 1515 + offset, 605 + Math.sin(animationClock * 5 + i) * 12);
            }
        }
        if (meetingAlertTimer > 0) {
            double alpha = 0.18 + 0.12 * Math.sin(animationClock * 9);
            gc.setFill(Color.rgb(139, 92, 246, alpha));
            gc.fillOval(1002, 172, 236, 120);
            gc.setStroke(Color.rgb(196, 181, 253, 0.55));
            gc.setLineWidth(3);
            gc.strokeOval(994, 164, 252, 136);
        }
    }

    private void drawInteractions(GraphicsContext gc) {
        for (ChaosInteraction interaction : interactions) {
            if ("wifi".equals(interaction.id()) && wifiOutageTimer > 0) {
                double pulse = 28 + Math.sin(animationClock * 8) * 5;
                gc.setStroke(Color.rgb(96, 165, 250, 0.5));
                gc.setLineWidth(3);
                gc.strokeOval(interaction.x() - pulse, interaction.y() - pulse, pulse * 2, pulse * 2);
            }
            if (interaction == stagedInteraction && stagedInteractionTimer > 0) {
                double pulse = 24 + Math.sin(animationClock * 12) * 4;
                gc.setStroke(Color.rgb(251, 191, 36, 0.72));
                gc.setLineWidth(4);
                gc.strokeOval(interaction.x() - pulse, interaction.y() - pulse, pulse * 2, pulse * 2);
            }
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

    private void drawSupportSpots(GraphicsContext gc) {
        gc.setTextAlign(TextAlignment.CENTER);
        for (CatSupportSpot supportSpot : supportSpots) {
            double radius = supportSpot.canUse() ? 18 : 14;
            Color fill = supportSpot.canUse()
                    ? supportSpot.color()
                    : supportSpot.color().deriveColor(0, 0.35, 0.85, 0.65);

            gc.setFill(fill.deriveColor(0, 1, 1, supportSpot.canUse() ? 0.85 : 0.42));
            gc.fillOval(supportSpot.x() - radius, supportSpot.y() - radius, radius * 2, radius * 2);
            gc.setStroke(fill.brighter());
            gc.setLineWidth(2);
            gc.strokeOval(supportSpot.x() - radius - 6, supportSpot.y() - radius - 6, (radius + 6) * 2, (radius + 6) * 2);

            gc.setFill(Color.rgb(17, 24, 39, 0.92));
            gc.setFont(Font.font("Verdana", FontWeight.BOLD, 11));
            gc.fillText(supportSpot.id().equals("snack") ? "SNACK" : "SUN", supportSpot.x(), supportSpot.y() + 4);
        }
        gc.setTextAlign(TextAlignment.LEFT);
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
        if (isPlayerLocked()) {
            return;
        }
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
        double urgencyPulse = 0.76 + 0.24 * (Math.sin(animationClock * 5.0) + 1) / 2.0;

        drawPanel(gc, 20, 596, 330, 108, Color.web("#f97316"));
        drawPanel(gc, 364, 596, 248, 108, Color.web("#22c55e"));
        drawPanel(gc, 626, 596, 334, 108, Color.web("#60a5fa"));
        drawPanel(gc, 974, 596, 266, 108, Color.web("#a78bfa"));

        gc.setFill(Color.web("#fff7ed"));
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 17));
        gc.fillText("Office Cat", 36, 622);

        gc.setFont(Font.font("Verdana", 13));
        gc.fillText(String.format("Time %02d:%02d", (int) timeLeft / 60, (int) timeLeft % 60), 36, 646);
        gc.fillText(String.format("Productivity %.0f%%", officeProductivity * 100), 194, 646);
        gc.fillText(player.isHidden() ? "Status Hidden in a box" : "Status Causing trouble", 36, 666);
        gc.fillText(comboCount > 1 && comboTimer > 0
                ? String.format("Combo x%d %.1fs", comboCount, comboTimer)
                : "Chain actions for combo", 36, 686);
        gc.fillText(player.isDashReady()
                ? "Dash Ready [Shift]"
                : String.format("Dash %.1fs", player.dashCooldownRemaining()), 194, 686);
        gc.fillText(insideDangerZone
                ? "Danger Zone Active"
                : (meowCooldownRemaining <= 0
                ? "Meow Ready [Space]"
                : String.format("Meow %.1fs", meowCooldownRemaining)), 36, 700);
        gc.fillText(player.isZoomiesActive()
                ? String.format("Zoomies %.1fs", player.zoomiesTimeRemaining())
                : "Find support spots for bonuses", 194, 700);

        Color chaosColor = chaosPercent >= 80
                ? Color.web("#fb7185").deriveColor(0, 1, urgencyPulse, 1)
                : Color.web("#ef4444");
        drawLabeledBar(gc, 380, 618, 216, 18, officeProductivity, officeProductivity > PRODUCTIVITY_CASCADE_THRESHOLD
                ? Color.web("#22c55e")
                : Color.web("#f59e0b"), "Office Flow");
        drawLabeledBar(gc, 380, 648, 216, 18, chaosPercent / 100.0, chaosColor, "Chaos " + String.format("%.0f%%", chaosPercent));

        gc.setFill(Color.web("#ede9fe"));
        gc.setFont(Font.font("Verdana", 12));
        gc.fillText("Manager Watch", 990, 622);
        gc.fillText(manager.statusText(), 990, 644);
        gc.fillText(String.format("Pressure %.0f%%", currentChaosPressure() * 50), 990, 666);
        gc.fillText("WASD move  E interact", 990, 688);
        gc.fillText("Shift dash  Space meow", 990, 704);

        gc.setFill(Color.web("#dbeafe"));
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 13));
        gc.fillText("Current Objective", 642, 622);
        gc.setFill(Color.web("#93c5fd").deriveColor(0, 1, 0.85 + 0.15 * urgencyPulse, 1));
        gc.fillOval(922, 610, 12, 12);
        gc.setFont(Font.font("Verdana", 12));
        if (currentObjective != null) {
            gc.setFill(Color.web("#dbeafe"));
            gc.fillText(currentObjective.title(), 642, 646);
            gc.fillText(currentObjective.description(), 642, 670);
            gc.setFill(Color.web("#93c5fd"));
            gc.fillText(String.format("Reward +%.0f chaos", currentObjective.bonusChaos()), 642, 694);
        }
    }

    private void drawIncidentFeed(GraphicsContext gc) {
        gc.setFill(Color.web("#ede9fe"));
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 13));
        gc.fillText("Recent Incidents", 990, 622);
        gc.setFont(Font.font("Verdana", 11));

        int line = 0;
        for (IncidentFeedEntry entry : incidentFeed) {
            if (line >= 3) {
                break;
            }
            gc.setFill(Color.web("#f5f3ff").deriveColor(0, 1, 1, entry.alpha()));
            gc.fillText("- " + entry.text(), 990, 640 + line * 18);
            line++;
        }
    }

    private void drawPrompt(GraphicsContext gc, ChaosInteraction interaction) {
        drawBottomPrompt(gc, "[E] " + interaction.promptText(), Color.web("#60a5fa"), 442);
    }

    private void drawHideSpotPrompt(GraphicsContext gc, HideSpot hideSpot) {
        drawBottomPrompt(gc, "[E] Hide in " + hideSpot.label(), Color.web("#22c55e"), 442);
    }

    private void drawSupportPrompt(GraphicsContext gc, CatSupportSpot supportSpot) {
        String suffix = supportSpot.canUse()
                ? supportSpot.promptText()
                : supportSpot.effectText() + " recharging";
        drawBottomPrompt(gc, "[E] " + suffix, Color.web("#f59e0b"), 512);
    }

    private void drawHidePrompt(GraphicsContext gc) {
        drawBottomPrompt(gc, "[E] Sneak out of the box", Color.web("#34d399"), 442);
    }

    private void drawGameOverOverlay(GraphicsContext gc) {
        gc.setFill(Color.rgb(8, 10, 18, 0.42));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        drawPanel(gc, 372, 228, 536, 180, Color.web("#ef4444"));

        gc.setFill(Color.web("#fff7ed"));
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 26));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(endMessage, WIDTH / 2.0, 282);

        gc.setFont(Font.font("Verdana", 16));
        gc.fillText(String.format("Final chaos %.0f%%", chaosPercent), WIDTH / 2.0, 326);
        gc.fillText(String.format("Office productivity %.0f%%", officeProductivity * 100), WIDTH / 2.0, 350);
        gc.fillText("[R] Restart   [Enter] Menu", WIDTH / 2.0, 382);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawMenuOverlay(GraphicsContext gc) {
        gc.setFill(Color.rgb(8, 10, 18, 0.44));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        drawPanel(gc, 334, 146, 612, 292, Color.web("#f97316"));

        gc.setFill(Color.web("#fff7ed"));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 30));
        gc.fillText("Office Cat: Chaos Manager", WIDTH / 2.0, 208);

        gc.setFont(Font.font("Verdana", 17));
        gc.fillText("Turn a productive office into a furry disaster zone.", WIDTH / 2.0, 248);
        gc.setStroke(Color.rgb(255, 255, 255, 0.18));
        gc.setLineWidth(2);
        gc.strokeLine(424, 266, 856, 266);

        gc.setFont(Font.font("Verdana", 14));
        gc.fillText("Reach 100% chaos before the work day ends.", WIDTH / 2.0, 290);
        gc.fillText("Avoid the manager, crash productivity, and chain combos.", WIDTH / 2.0, 316);
        gc.fillText("Shift dash   Space meow   E interact   Esc pause", WIDTH / 2.0, 350);
        gc.setFill(Color.web("#fdba74"));
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 16));
        gc.fillText("[Enter] Start Run", WIDTH / 2.0, 398);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawPauseOverlay(GraphicsContext gc) {
        gc.setFill(Color.rgb(8, 10, 18, 0.34));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        drawPanel(gc, 430, 238, 420, 152, Color.web("#60a5fa"));

        gc.setFill(Color.web("#eff6ff"));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 24));
        gc.fillText("Paused", WIDTH / 2.0, 286);

        gc.setFont(Font.font("Verdana", 16));
        gc.fillText("[Esc / P] Resume", WIDTH / 2.0, 326);
        gc.fillText("[R] Restart run", WIDTH / 2.0, 352);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawTopRibbon(GraphicsContext gc) {
        drawPanel(gc, 18, 10, 344, 42, Color.web("#f97316"));
        drawPanel(gc, 388, 10, 276, 42, Color.web("#34d399"));
        drawPanel(gc, 1038, 10, 224, 42, Color.web("#60a5fa"));

        gc.setFill(Color.web("#fff7ed"));
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 12));
        gc.fillText("OFFICE CAT // CHAOS SHIFT", 34, 36);
        gc.setFill(Color.web("#d1fae5"));
        gc.fillText(currentZoneLabel(), 406, 36);

        gc.setFill(Color.web("#dbeafe"));
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.fillText(dayStageLabel(), 1244, 36);
        gc.setTextAlign(TextAlignment.LEFT);
        if (wifiOutageTimer > 0) {
            drawPanel(gc, 690, 10, 320, 42, Color.web("#3b82f6"));
            gc.setFill(Color.web("#eff6ff"));
            gc.setFont(Font.font("Verdana", FontWeight.BOLD, 12));
            gc.fillText(String.format("WIFI OUTAGE %.0fs", Math.ceil(wifiOutageTimer)), 708, 36);
        }
        if (meetingAlertTimer > 0) {
            drawPanel(gc, 690, 58, 320, 42, Color.web("#8b5cf6"));
            gc.setFill(Color.web("#f5f3ff"));
            gc.setFont(Font.font("Verdana", FontWeight.BOLD, 12));
            gc.fillText(String.format("MEETING MELTDOWN %.0fs", Math.ceil(meetingAlertTimer)), 708, 84);
        }
    }

    private String dayStageLabel() {
        double progress = 1.0 - (timeLeft / DAY_DURATION_SECONDS);
        if (progress < 0.33) {
            return "MORNING CHAOS";
        }
        if (progress < 0.66) {
            return "DEADLINE PANIC";
        }
        return "FINAL HOUR";
    }

    private String currentZoneLabel() {
        double px = player.centerX();
        double py = player.centerY();
        if (contains(OPEN_SPACE_ROOM, px, py)) {
            return "CURRENT ZONE: OPEN SPACE";
        }
        if (contains(MEETING_ROOM, px, py)) {
            return "CURRENT ZONE: MEETING ROOM";
        }
        if (contains(KITCHEN_ROOM, px, py)) {
            return "CURRENT ZONE: KITCHEN";
        }
        if (contains(DIRECTOR_ROOM, px, py)) {
            return "CURRENT ZONE: DIRECTOR'S OFFICE";
        }
        return "CURRENT ZONE: HALLWAY";
    }

    private boolean contains(Rect rect, double x, double y) {
        return x >= rect.x()
                && x <= rect.x() + rect.width()
                && y >= rect.y()
                && y <= rect.y() + rect.height();
    }

    private void drawRoomBadge(GraphicsContext gc, double x, double y, String label, Color accent) {
        double width = Math.max(116, label.length() * 8.3 + 26);
        gc.setFill(Color.rgb(17, 24, 39, 0.88));
        gc.fillRoundRect(x, y, width, 28, 14, 14);
        gc.setFill(accent.deriveColor(0, 1, 1, 0.96));
        gc.fillRoundRect(x, y, width, 6, 14, 14);
        gc.setFill(Color.web("#fff7ed"));
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 12));
        gc.fillText(label, x + 12, y + 20);
    }

    private void drawCorridors(GraphicsContext gc) {
        gc.setFill(Color.web("#e3dccf"));
        gc.fillRoundRect(120, 486, 1740, 126, 18, 18);
        gc.fillRoundRect(1112, 392, 112, 144, 18, 18);
        gc.setStroke(Color.rgb(148, 163, 184, 0.35));
        gc.setLineWidth(2);
        for (double x = 146; x < 1830; x += 54) {
            gc.strokeLine(x, 548, x + 26, 548);
        }
        gc.setStroke(Color.rgb(255, 255, 255, 0.2));
        gc.strokeRoundRect(120, 486, 1740, 126, 18, 18);
    }

    private void drawDoorway(GraphicsContext gc, double centerX, double baseY, boolean topSide) {
        double width = 90;
        double height = 18;
        double y = topSide ? baseY - 4 : baseY - height + 4;
        double jambHeight = 28;
        gc.setFill(Color.web("#5b6470"));
        gc.fillRect(centerX - width / 2.0 - 10, y - (topSide ? 0 : 10), 10, jambHeight);
        gc.fillRect(centerX + width / 2.0, y - (topSide ? 0 : 10), 10, jambHeight);
        gc.setFill(Color.web("#d1b38a"));
        gc.fillRoundRect(centerX - width / 2.0, y, width, height, 8, 8);
        gc.setFill(Color.web("#8b5e34"));
        gc.fillRect(centerX - width / 2.0 + 6, y + 4, width - 12, 4);
        gc.setStroke(Color.rgb(70, 45, 25, 0.35));
        gc.setLineWidth(2);
        gc.strokeLine(centerX, y + 4, centerX, y + height - 4);
        gc.setStroke(Color.rgb(255, 248, 220, 0.34));
        gc.setLineWidth(1);
        gc.strokeRoundRect(centerX - width / 2.0, y, width, height, 8, 8);
    }

    private void drawRoomDepth(GraphicsContext gc, Rect room) {
        gc.setFill(Color.rgb(15, 23, 42, 0.08));
        gc.fillRoundRect(room.x() + 10, room.y() + room.height() - 22, room.width() - 20, 12, 10, 10);
        gc.setFill(Color.rgb(255, 255, 255, 0.08));
        gc.fillRoundRect(room.x() + 10, room.y() + 12, room.width() - 20, 8, 8, 8);
    }

    private void addRoomWalls(Rect room, double doorCenter, double doorSideY) {
        double thickness = 14;
        double doorSize = 92;

        if (doorSideY <= room.y()) {
            walls.add(new Rect(room.x(), room.y(), doorCenter - doorSize / 2.0 - room.x(), thickness));
            walls.add(new Rect(doorCenter + doorSize / 2.0, room.y(),
                    room.x() + room.width() - (doorCenter + doorSize / 2.0), thickness));
            walls.add(new Rect(room.x(), room.y(), thickness, room.height()));
            walls.add(new Rect(room.x() + room.width() - thickness, room.y(), thickness, room.height()));
            walls.add(new Rect(room.x(), room.y() + room.height() - thickness, room.width(), thickness));
        } else {
            walls.add(new Rect(room.x(), room.y(), room.width(), thickness));
            walls.add(new Rect(room.x(), room.y(), thickness, room.height()));
            walls.add(new Rect(room.x() + room.width() - thickness, room.y(), thickness, room.height()));
            walls.add(new Rect(room.x(), room.y() + room.height() - thickness,
                    doorCenter - doorSize / 2.0 - room.x(), thickness));
            walls.add(new Rect(doorCenter + doorSize / 2.0, room.y() + room.height() - thickness,
                    room.x() + room.width() - (doorCenter + doorSize / 2.0), thickness));
        }
    }

    private void drawRoomFrame(GraphicsContext gc, Rect room, Color strokeColor) {
        gc.setStroke(strokeColor);
        gc.setLineWidth(4);
        gc.strokeRect(room.x(), room.y(), room.width(), room.height());
        gc.setStroke(Color.rgb(255, 255, 255, 0.18));
        gc.setLineWidth(1);
        gc.strokeRect(room.x() + 5, room.y() + 5, room.width() - 10, room.height() - 10);
    }

    private void drawGridFloor(GraphicsContext gc, Rect room, Color base, Color lineColor, double tileSize) {
        gc.setFill(base);
        gc.fillRect(room.x(), room.y(), room.width(), room.height());

        gc.setStroke(lineColor);
        gc.setLineWidth(1.4);
        for (double x = room.x(); x <= room.x() + room.width(); x += tileSize) {
            gc.strokeLine(x, room.y(), x, room.y() + room.height());
        }
        for (double y = room.y(); y <= room.y() + room.height(); y += tileSize) {
            gc.strokeLine(room.x(), y, room.x() + room.width(), y);
        }

        gc.setStroke(Color.rgb(30, 41, 59, 0.22));
        gc.setLineWidth(2);
        for (double x = room.x() + tileSize / 2.0; x < room.x() + room.width(); x += tileSize) {
            gc.strokeLine(x, room.y(), x, room.y() + room.height());
        }
    }

    private void drawMeetingFloor(GraphicsContext gc, Rect room) {
        gc.setFill(Color.web("#5b6b87"));
        gc.fillRect(room.x(), room.y(), room.width(), room.height());
        gc.setStroke(Color.rgb(255, 255, 255, 0.12));
        gc.setLineWidth(2);
        for (double y = room.y() + 8; y < room.y() + room.height(); y += 20) {
            gc.strokeLine(room.x() + 8, y, room.x() + room.width() - 8, y);
        }
        gc.setStroke(Color.rgb(17, 24, 39, 0.18));
        for (double x = room.x() + 18; x < room.x() + room.width(); x += 28) {
            gc.strokeLine(x, room.y() + 8, x, room.y() + room.height() - 8);
        }
    }

    private void drawKitchenFloor(GraphicsContext gc, Rect room) {
        gc.setFill(Color.web("#ebf4df"));
        gc.fillRect(room.x(), room.y(), room.width(), room.height());
        gc.setStroke(Color.web("#a3b18a"));
        gc.setLineWidth(2);
        for (double y = room.y(); y <= room.y() + room.height(); y += 30) {
            gc.strokeLine(room.x(), y, room.x() + room.width(), y);
        }
        for (double x = room.x(); x <= room.x() + room.width(); x += 30) {
            gc.strokeLine(x, room.y(), x, room.y() + room.height());
        }
        gc.setFill(Color.rgb(255, 255, 255, 0.12));
        gc.fillRect(room.x(), room.y(), room.width(), room.height() * 0.25);
    }

    private void drawWoodFloor(GraphicsContext gc, Rect room, Color base, Color accent) {
        gc.setFill(base);
        gc.fillRect(room.x(), room.y(), room.width(), room.height());
        gc.setStroke(accent);
        gc.setLineWidth(3);
        for (double x = room.x() + 2; x < room.x() + room.width(); x += 42) {
            gc.strokeLine(x, room.y(), x, room.y() + room.height());
        }
        gc.setStroke(Color.rgb(255, 230, 180, 0.22));
        gc.setLineWidth(1);
        for (double y = room.y() + 18; y < room.y() + room.height(); y += 42) {
            gc.strokeLine(room.x(), y, room.x() + room.width(), y);
        }
    }

    private void drawPanel(GraphicsContext gc, double x, double y, double width, double height, Color accent) {
        gc.setFill(Color.rgb(6, 10, 18, 0.26));
        gc.fillRoundRect(x + 4, y + 6, width, height, 20, 20);
        gc.setFill(Color.rgb(17, 24, 39, 0.93));
        gc.fillRoundRect(x, y, width, height, 20, 20);
        gc.setFill(accent.deriveColor(0, 1, 1, 0.92));
        gc.fillRoundRect(x, y, width, 8, 20, 20);
        gc.setStroke(Color.rgb(255, 255, 255, 0.08));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(x + 0.75, y + 0.75, width - 1.5, height - 1.5, 20, 20);
    }

    private void drawLabeledBar(
            GraphicsContext gc,
            double x,
            double y,
            double width,
            double height,
            double progress,
            Color fillColor,
            String label
    ) {
        double clamped = Math.max(0, Math.min(1, progress));
        gc.setFill(Color.rgb(255, 255, 255, 0.14));
        gc.fillRoundRect(x, y, width, height, 10, 10);
        gc.setFill(fillColor);
        gc.fillRoundRect(x, y, width * clamped, height, 10, 10);
        gc.setFill(Color.web("#111827"));
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 12));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(label, x + width / 2.0, y + 14);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawBottomPrompt(GraphicsContext gc, String text, Color accent, double width) {
        double x = (WIDTH - width) / 2.0;
        drawPanel(gc, x, 546, width, 40, accent);
        gc.setFill(Color.web("#fff7ed"));
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 13));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(text, WIDTH / 2.0, 566);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.BASELINE);
    }

    private void drawSpeechTag(GraphicsContext gc, double centerX, double y, String text, Color background) {
        double width = Math.max(112, text.length() * 6.8 + 24);
        gc.setFill(background);
        gc.fillRoundRect(centerX - width / 2.0, y - 14, width, 24, 12, 12);
        gc.setStroke(Color.rgb(255, 255, 255, 0.1));
        gc.setLineWidth(1);
        gc.strokeRoundRect(centerX - width / 2.0, y - 14, width, 24, 12, 12);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Verdana", 11));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(text, centerX, y - 2);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.BASELINE);
    }

    private void drawFurniture(GraphicsContext gc) {
        drawCenteredSprite(gc, deskSprite, 520, 188, 258);
        drawCenteredSprite(gc, deskSprite, 520, 330, 258);
        drawCenteredSprite(gc, meetingTableSprite, 1120, 232, 336);
        drawCenteredSprite(gc, cabinetSprite, 1618, 178, 156);
        drawCenteredSprite(gc, deskSprite, 1462, 630, 280);
        drawCenteredSprite(gc, cabinetSprite, 1730, 700, 138);
        drawCenteredSprite(gc, cabinetSprite, 1182, 714, 126);

        drawCenteredSprite(gc, chairSprite, 358, 242, 52);
        drawCenteredSprite(gc, chairSprite, 358, 384, 52);
        drawCenteredSprite(gc, chairSprite, 1282, 740, 52);

        drawCenteredSprite(gc, plantSprite, 798, 430, 58);
        drawCenteredSprite(gc, plantSprite, 1760, 344, 56);
        drawCenteredSprite(gc, plantSprite, 1116, 812, 56);
    }

    private void buildOfficeLayout() {
        addRoomWalls(OPEN_SPACE_ROOM, OPEN_SPACE_DOOR_X, OPEN_SPACE_ROOM.y() + OPEN_SPACE_ROOM.height());
        addRoomWalls(MEETING_ROOM, MEETING_DOOR_X, MEETING_ROOM.y() + MEETING_ROOM.height());
        addRoomWalls(KITCHEN_ROOM, KITCHEN_DOOR_X, KITCHEN_ROOM.y() + KITCHEN_ROOM.height());
        addRoomWalls(DIRECTOR_ROOM, DIRECTOR_DOOR_X, DIRECTOR_ROOM.y());

        walls.add(new Rect(432, 170, 198, 42));
        walls.add(new Rect(432, 312, 198, 42));
        walls.add(new Rect(1004, 176, 234, 92));
        walls.add(new Rect(1532, 162, 186, 48));
        walls.add(new Rect(1352, 612, 206, 56));
        walls.add(new Rect(1672, 686, 76, 72));
        walls.add(new Rect(1152, 706, 66, 80));

        hideSpots.add(new HideSpot(130, 428, 68, "storage box"));
        hideSpots.add(new HideSpot(1284, 336, 68, "meeting room box"));
        hideSpots.add(new HideSpot(1716, 786, 68, "director archive box"));

        supportSpots.add(new CatSupportSpot(
                "snack",
                1694,
                246,
                "Grab a kitchen snack to refresh dash and meow",
                "Snack station",
                16.0,
                82,
                Color.web("#f59e0b")
        ));
        supportSpots.add(new CatSupportSpot(
                "sunbeam",
                1124,
                814,
                "Stretch in the sunbeam for temporary zoomies",
                "Sunbeam boost",
                18.0,
                86,
                Color.web("#fde047")
        ));

        interactions.add(new ChaosInteraction(
                "keyboard",
                576, 218,
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
                1690, 196,
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
                1372, 640,
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
                1120, 312,
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
                1512, 604,
                "Scatter the director's paperwork",
                "paper catastrophe",
                20,
                6.2,
                80,
                210,
                7.2,
                Color.web("#10b981")
        ));

        employees.add(new EmployeeNpc(
                "Mila",
                EmployeeNpc.Role.DEVELOPER,
                334,
                244,
                Color.web("#3b82f6"),
                List.of(
                        new Point(334, 244),
                        new Point(250, 244),
                        new Point(334, 300),
                        new Point(420, 244)
                )
        ));
        employees.add(new EmployeeNpc(
                "Jon",
                EmployeeNpc.Role.DESIGNER,
                334,
                386,
                Color.web("#22c55e"),
                List.of(
                        new Point(334, 386),
                        new Point(250, 386),
                        new Point(420, 386),
                        new Point(334, 440)
                )
        ));
        employees.add(new EmployeeNpc(
                "Ava",
                EmployeeNpc.Role.ANALYST,
                1120,
                314,
                Color.web("#f97316"),
                List.of(
                        new Point(1120, 314),
                        new Point(1030, 314),
                        new Point(1210, 314),
                        new Point(1120, 250)
                )
        ));
        employees.add(new EmployeeNpc(
                "Noah",
                EmployeeNpc.Role.LEAD,
                1288,
                742,
                Color.web("#ec4899"),
                List.of(
                        new Point(1288, 742),
                        new Point(1204, 742),
                        new Point(1374, 742),
                        new Point(1288, 680)
                )
        ));
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
            case SEARCHING -> managerAlertFrames.isEmpty() ? managerIdleSprite : frameAt(managerAlertFrames, 6.0);
            case INVESTIGATING -> managerAlertFrames.isEmpty() ? managerIdleSprite : frameAt(managerAlertFrames, 4.5);
            case PATROLLING -> managerWalkFrames.isEmpty() ? managerIdleSprite : frameAt(managerWalkFrames, 6.0);
        };
    }

    private Image interactionSprite(ChaosInteraction interaction) {
        return switch (interaction.id()) {
            case "keyboard" -> keyboardNapTimer > 0 ? sleepingKeyboardSprite : keyboardSprite;
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
