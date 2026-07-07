package game;

import static org.lwjgl.glfw.GLFW.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import game.core.Updatable;
import game.engine.*;
import game.gameplay.*;
import game.scoring.Scoreboard;
import game.scoring.ScoreEntry;
import game.scene.*;
import game.ui.Menu;

public class Main {
    public static void main(String[] args) {
        new Main().run();
    }

    // Engine
    private Window window;
    private Renderer renderer;
    private Camera camera;

    // Game systems
    private GameState gameState;
    private PlayerController playerController;
    private List<TrafficCar> trafficCars;
    private Menu menu;
    private HUD hud;
    private CollisionSystem collisionSystem;
    private CityMap cityMap;
    private Scoreboard scoreboard;
    private List<ScoreEntry> highScores = new ArrayList<>();
    private boolean scoreSavedForCurrentGame = false;
    private boolean waitingForScoreName = false;
    private final StringBuilder playerNameInput = new StringBuilder();
    private static final int MAX_PLAYER_NAME_LENGTH = 14;

    // Scene
    private List<Entity> cityEntities;
    private Entity playerCarEntity;
    private List<Entity> playerCarEntities;
    private List<Passenger> passengers;
    private Passenger activePassenger;
    private List<Vector2f> roadSamples = new ArrayList<>();
    private final Map<Long, List<RoadTriangle>> roadHeightGrid = new HashMap<>();
    private final List<RoadTriangle> roadHeightTriangles = new ArrayList<>();

    // Models
    private List<Mesh> carMeshes;
    private List<Mesh> trafficCarMeshes;
    private List<Mesh> passengerMeshes;
    private List<Mesh> cityMeshes;
    private Vector3f cityOriginOffset = new Vector3f();
    private float cityScale = 1.0f;
    private Vector3f carOriginOffset = new Vector3f();
    private Vector3f carScale = new Vector3f(1.0f);
    private Vector3f trafficCarOriginOffset = new Vector3f();
    private Vector3f trafficCarScale = new Vector3f(1.0f);
    private Vector3f passengerOriginOffset = new Vector3f();
    private Vector3f passengerScale = new Vector3f(1.0f);
    private Mesh ambientTrafficMesh;
    private static final float CAR_MODEL_YAW_OFFSET = (float) Math.PI;
    private static final float TRAFFIC_MODEL_YAW_OFFSET = (float) Math.PI;

    // Game loop
    private boolean running = true;

    // Model paths
    private static final String CAR_MODEL_PATH = "1982_toyota_hiace_combi.glb";
    private static final String TRAFFIC_CAR_MODEL_PATH = "bmw_m4_competition_m_package.glb";
    private static final String PASSENGER_MODEL_PATH = "assets/passengers/passenger.glb";
    private static final String CITY_MODEL_PATH = "source/burnin_rubber_crash_n_burn_city.glb";

    // City bounds (will be adjusted based on the loaded model)
    private float cityMinX = -100, cityMaxX = 100;
    private float cityMinZ = -100, cityMaxZ = 100;
    private final Vector2f playerSpawn = new Vector2f();
    private float crashCooldown = 0.0f;
    private int trafficUpdateFrame = 0;
    private static final float CRASH_COOLDOWN_SECONDS = 2.0f;
    private static final float ROAD_CLEARANCE = 0.16f;
    private static final float ROAD_HEIGHT_BAND_BELOW = 5.0f;
    private static final float ROAD_HEIGHT_BAND_ABOVE = 4.0f;
    private static final float ROAD_LAYER_EPSILON = 0.03f;
    private static final float ROAD_HEIGHT_GRID_CELL_SIZE = 36.0f;
    private static final float ROAD_MIN_SURFACE_NORMAL_Y = 0.5f;
    private static final float ROAD_SAMPLE_NORMAL_Y = 0.75f;
    private static final int MAX_ROAD_SAMPLES = 900;
    private static final float CITY_TARGET_WORLD_SIZE = 1250.0f;
    private static final float SPAWN_BUILDING_CLEARANCE = 5.0f;
    private static final float SPAWN_RAY_INTERSECTION_EPSILON = 0.35f;
    private static final int INTERACTIVE_TRAFFIC_COUNT = 10;
    private static final int AMBIENT_TRAFFIC_COUNT = 38;

    private void run() {
        try {
            init();
            loop();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private void init() {
        // Create window
        window = new Window(1280, 720, "City Racer - Burnin Rubber Variation");
        renderer = new Renderer();
        camera = new Camera();
        camera.setAspect(1280.0f / 720.0f);

        // Game state
        gameState = new GameState();
        scoreboard = new Scoreboard();
        highScores = scoreboard.loadTopScores();

        // Collision system
        collisionSystem = new CollisionSystem();

        // Initialize menu and HUD
        menu = new Menu(renderer, gameState);
        hud = new HUD(renderer, gameState);

        // Load models
        System.out.println("Loading city model: " + CITY_MODEL_PATH);
        cityMeshes = ModelLoader.loadModel(CITY_MODEL_PATH);
        if (cityMeshes.isEmpty()) {
            System.out.println("Warning: City model loaded with 0 meshes, using fallback");
            generateFallbackCity();
        } else {
            System.out.println("Loaded " + cityMeshes.size() + " city meshes");
            createCityScene();
        }

        System.out.println("Loading car model: " + CAR_MODEL_PATH);
        carMeshes = ModelLoader.loadModel(CAR_MODEL_PATH);
        if (carMeshes.isEmpty()) {
            System.out.println("Warning: Car model loaded with 0 meshes, generating fallback");
            generateFallbackCar();
        } else {
            System.out.println("Loaded " + carMeshes.size() + " car meshes");
            createPlayerCar();
        }

        System.out.println("Loading traffic car model: " + TRAFFIC_CAR_MODEL_PATH);
        trafficCarMeshes = ModelLoader.loadModel(TRAFFIC_CAR_MODEL_PATH);
        if (trafficCarMeshes.isEmpty()) {
            System.out.println("Warning: Traffic car model loaded with 0 meshes, using player car for traffic");
        } else {
            System.out.println("Loaded " + trafficCarMeshes.size() + " traffic car meshes");
            prepareTrafficCarModel();
        }

        System.out.println("Loading passenger model: " + PASSENGER_MODEL_PATH);
        passengerMeshes = ModelLoader.loadModel(PASSENGER_MODEL_PATH);
        if (passengerMeshes.isEmpty()) {
            System.out.println("Warning: Passenger model loaded with 0 meshes, generating fallback");
            generateFallbackPassengerModel();
        } else {
            System.out.println("Loaded " + passengerMeshes.size() + " passenger meshes");
            preparePassengerModel();
        }

        roadSamples = collectRoadSamples();

        // Calculate city bounds from entities
        calculateCityBounds();
        buildRoadHeightIndex();

        // Create city map
        cityMap = new CityMap(cityMinX, cityMaxX, cityMinZ, cityMaxZ, roadSamples);
        calculatePlayerSpawn();

        // Create traffic
        createTraffic();

        // Create passengers
        createPassengers();

        // Setup input callbacks
        setupInput();

        System.out.println("Game initialized. City bounds: " +
            cityMinX + " to " + cityMaxX + " / " + cityMinZ + " to " + cityMaxZ);
    }

    private void calculateCityBounds() {
        if (roadSamples != null && roadSamples.size() >= 8) {
            MeshBounds roadBounds = new MeshBounds();
            for (Vector2f sample : roadSamples) {
                roadBounds.include(new Vector3f(sample.x, 0.0f, sample.y));
            }
            if (roadBounds.isValid()) {
                float padX = Math.max(20.0f, roadBounds.getWidth() * 0.05f);
                float padZ = Math.max(20.0f, roadBounds.getDepth() * 0.05f);
                cityMinX = roadBounds.min.x - padX;
                cityMaxX = roadBounds.max.x + padX;
                cityMinZ = roadBounds.min.z - padZ;
                cityMaxZ = roadBounds.max.z + padZ;
                return;
            }
        }

        if (cityMeshes != null && !cityMeshes.isEmpty()) {
            MeshBounds bounds = calculateRenderableCityBounds();
            if (bounds.isValid()) {
                cityMinX = (bounds.min.x + cityOriginOffset.x) * cityScale;
                cityMaxX = (bounds.max.x + cityOriginOffset.x) * cityScale;
                cityMinZ = (bounds.min.z + cityOriginOffset.z) * cityScale;
                cityMaxZ = (bounds.max.z + cityOriginOffset.z) * cityScale;
                return;
            }
        }

        if (cityEntities != null && !cityEntities.isEmpty()) {
            float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
            float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
            for (Entity e : cityEntities) {
                if (e.getPosition().x < minX) minX = e.getPosition().x;
                if (e.getPosition().x > maxX) maxX = e.getPosition().x;
                if (e.getPosition().z < minZ) minZ = e.getPosition().z;
                if (e.getPosition().z > maxZ) maxZ = e.getPosition().z;
            }
            if (minX != Float.MAX_VALUE) {
                cityMinX = minX - 5;
                cityMaxX = maxX + 5;
                cityMinZ = minZ - 5;
                cityMaxZ = maxZ + 5;
            }
        }
    }

    private List<Vector2f> collectRoadSamples() {
        List<Vector3f> candidates = new ArrayList<>();
        if (cityMeshes == null || cityMeshes.isEmpty()) return new ArrayList<>();

        Vector3f a = new Vector3f();
        Vector3f b = new Vector3f();
        Vector3f c = new Vector3f();
        Vector3f ab = new Vector3f();
        Vector3f ac = new Vector3f();
        Vector3f normal = new Vector3f();

        for (Mesh mesh : cityMeshes) {
            if (mesh == null || !isRoadSampleMesh(mesh)) continue;

            for (int i = 0; i + 2 < mesh.getIndexCount(); i += 3) {
                mesh.getPosition(mesh.getIndex(i), a);
                mesh.getPosition(mesh.getIndex(i + 1), b);
                mesh.getPosition(mesh.getIndex(i + 2), c);
                transformCityPoint(a);
                transformCityPoint(b);
                transformCityPoint(c);

                if (surfaceVerticalShare(a, b, c, ab, ac, normal) < ROAD_SAMPLE_NORMAL_Y) continue;

                float y = (a.y + b.y + c.y) / 3.0f;
                if (y < -35.0f || y > 55.0f) continue;

                candidates.add(new Vector3f(
                    (a.x + b.x + c.x) / 3.0f,
                    y,
                    (a.z + b.z + c.z) / 3.0f
                ));
            }
        }

        if (candidates.isEmpty()) return new ArrayList<>();

        candidates.sort((p1, p2) -> Float.compare(p1.y, p2.y));
        float medianY = candidates.get(candidates.size() / 2).y;
        List<Vector2f> samples = new ArrayList<>();
        int step = Math.max(1, candidates.size() / MAX_ROAD_SAMPLES);

        for (int i = 0; i < candidates.size() && samples.size() < MAX_ROAD_SAMPLES; i += step) {
            Vector3f point = candidates.get(i);
            if (Math.abs(point.y - medianY) > 28.0f) continue;
            samples.add(new Vector2f(point.x, point.z));
        }

        System.out.println("Collected " + samples.size() + " road samples from city geometry");
        return samples;
    }

    private void transformCityPoint(Vector3f point) {
        point.add(cityOriginOffset).mul(cityScale);
    }

    private boolean isRoadSampleMesh(Mesh mesh) {
        String name = mesh.getName().toLowerCase();
        boolean roadLike = name.contains("street") ||
            name.contains("collisiontraffic") ||
            name.contains("crosses") ||
            name.contains("parking") ||
            name.contains("bridge") ||
            name.contains("tunnel");
        return roadLike &&
            !name.contains("lightmap") &&
            !name.contains("invisible");
    }

    private void createCityScene() {
        cityEntities = new ArrayList<>();
        MeshBounds bounds = calculateRenderableCityBounds();
        MeshBounds streetBounds = calculateStreetBounds();
        cityOriginOffset.zero();
        cityScale = 1.0f;

        if (bounds.isValid()) {
            Vector3f center = bounds.getCenter();
            float groundY = calculateStreetGroundY(streetBounds.isValid() ? streetBounds : bounds);
            cityOriginOffset.set(-center.x, -groundY, -center.z);

            float width = Math.max(bounds.getWidth(), bounds.getDepth());
            if (width > 0.0f) {
                cityScale = CITY_TARGET_WORLD_SIZE / width;
            }

            System.out.println("City model bounds: " + bounds.min + " to " + bounds.max +
                ", scale=" + cityScale + ", originOffset=" + cityOriginOffset);
        }

        for (Mesh mesh : cityMeshes) {
            if (shouldSkipCityMesh(mesh)) {
                System.out.println("Skipping non-visual city mesh: " + mesh.getName());
                continue;
            }
            Entity entity = new Entity(mesh, "city");
            entity.getTransform().originOffset.set(cityOriginOffset);
            entity.getTransform().scale.set(cityScale);
            cityEntities.add(entity);
        }
    }

    private void createPlayerCar() {
        if (carMeshes.isEmpty()) return;

        playerCarEntities = new ArrayList<>();
        MeshBounds bounds = calculateMeshBounds(carMeshes);
        carOriginOffset.zero();
        carScale.set(1.0f);

        if (bounds.isValid()) {
            Vector3f center = bounds.getCenter();
            carOriginOffset.set(-center.x, -bounds.min.y, -center.z);

            float length = Math.max(bounds.getWidth(), bounds.getDepth());
            float height = Math.max(bounds.getHeight(), 0.001f);
            float uniformScale = length > 0.0f ? 4.5f / length : 1.0f;
            carScale.set(uniformScale, uniformScale, uniformScale);

            // Keep very tall or very flat source models in a useful gameplay size.
            float scaledHeight = height * uniformScale;
            if (scaledHeight > 2.2f) {
                carScale.mul(2.2f / scaledHeight);
            } else if (scaledHeight < 0.8f) {
                carScale.mul(0.8f / scaledHeight);
            }

            System.out.println("Car model bounds: " + bounds.min + " to " + bounds.max +
                ", scale=" + carScale + ", originOffset=" + carOriginOffset);
        }

        for (Mesh mesh : carMeshes) {
            Entity carPart = new Entity(mesh, "player");
            carPart.setPosition(0, 0, 0);
            carPart.getTransform().originOffset.set(carOriginOffset);
            carPart.getTransform().scale.set(carScale);
            carPart.getColor().set(1.0f, 1.0f, 1.0f);
            playerCarEntities.add(carPart);
        }

        playerCarEntity = playerCarEntities.get(0);

        camera = new Camera();
        camera.setAspect(1280.0f / 720.0f);

        playerController = new PlayerController(playerCarEntity, camera);
        playerController.setModelYawOffset(CAR_MODEL_YAW_OFFSET);
    }

    private void createTraffic() {
        trafficCars = new ArrayList<>();
        List<Mesh> modelMeshes = trafficCarMeshes != null && !trafficCarMeshes.isEmpty() ? trafficCarMeshes : carMeshes;
        Vector3f modelOriginOffset = modelMeshes == trafficCarMeshes ? trafficCarOriginOffset : carOriginOffset;
        Vector3f modelScale = modelMeshes == trafficCarMeshes ? trafficCarScale : carScale;
        float modelYawOffset = modelMeshes == trafficCarMeshes ? TRAFFIC_MODEL_YAW_OFFSET : CAR_MODEL_YAW_OFFSET;
        if (modelMeshes == null || modelMeshes.isEmpty()) return;

        ambientTrafficMesh = createAmbientTrafficMesh();

        List<List<Vector2f>> routes = cityMap.getTrafficRoutes();
        if (routes.isEmpty()) return;

        int interactiveCount = Math.min(INTERACTIVE_TRAFFIC_COUNT, Math.max(4, routes.size()));
        int totalTraffic = interactiveCount + AMBIENT_TRAFFIC_COUNT;

        for (int i = 0; i < totalTraffic; i++) {
            boolean ambient = i >= interactiveCount;
            int routeIndex = (i * 7) % routes.size();
            List<Vector2f> route = routes.get(routeIndex);
            if (route == null || route.size() < 2) continue;

            int startIndex = (i * 5 + route.size() / 3) % route.size();
            Vector2f start = route.get(startIndex);
            float speed = ambient
                ? 3.0f + (i % 9) * 0.35f
                : 4.5f + (i % 5) * 0.8f;

            List<Entity> trafficEntities = new ArrayList<>();
            if (ambient && ambientTrafficMesh != null) {
                Entity trafficEntity = new Entity(ambientTrafficMesh, "traffic");
                trafficEntity.getColor().set(ambientTrafficColor(i));
                trafficEntity.setPosition(start.x, 0, start.y);
                trafficEntities.add(trafficEntity);
            } else {
                for (Mesh mesh : modelMeshes) {
                    Entity trafficEntity = new Entity(mesh, "traffic");
                    trafficEntity.getTransform().originOffset.set(modelOriginOffset);
                    trafficEntity.getTransform().scale.set(modelScale);
                    trafficEntity.getColor().set(ambientTrafficColor(i));
                    trafficEntity.setPosition(start.x, 0, start.y);
                    trafficEntities.add(trafficEntity);
                }
            }

            TrafficCar trafficCar = new TrafficCar(trafficEntities, speed);
            trafficCar.setModelYawOffset(modelYawOffset);
            trafficCar.setWaypoints(route);
            trafficCar.setRouteIndex(startIndex);
            trafficCar.setCollidable(!ambient);
            applyRoadHeight(trafficCar);
            trafficCars.add(trafficCar);
        }

        System.out.println("Created " + trafficCars.size() + " traffic cars using " +
            routes.size() + " road-following routes");
    }

    private Vector3f ambientTrafficColor(int index) {
        Vector3f[] colors = {
            new Vector3f(0.95f, 0.95f, 1.0f),
            new Vector3f(0.85f, 0.12f, 0.12f),
            new Vector3f(0.12f, 0.25f, 0.85f),
            new Vector3f(0.95f, 0.82f, 0.18f),
            new Vector3f(0.12f, 0.65f, 0.35f),
            new Vector3f(0.18f, 0.18f, 0.20f)
        };
        return new Vector3f(colors[Math.floorMod(index, colors.length)]);
    }

    private Mesh createAmbientTrafficMesh() {
        return new Mesh(createBoxVertices(0, 0.45f, 0, 2.0f, 0.9f, 4.0f), createBoxIndices(), false, "ambient_traffic_car");
    }

    private void prepareTrafficCarModel() {
        if (trafficCarMeshes == null || trafficCarMeshes.isEmpty()) return;

        MeshBounds bounds = calculateMeshBounds(trafficCarMeshes);
        trafficCarOriginOffset.zero();
        trafficCarScale.set(1.0f);

        if (bounds.isValid()) {
            Vector3f center = bounds.getCenter();
            trafficCarOriginOffset.set(-center.x, -bounds.min.y, -center.z);

            float length = Math.max(bounds.getWidth(), bounds.getDepth());
            float height = Math.max(bounds.getHeight(), 0.001f);
            float uniformScale = length > 0.0f ? 4.3f / length : 1.0f;
            trafficCarScale.set(uniformScale, uniformScale, uniformScale);

            float scaledHeight = height * uniformScale;
            if (scaledHeight > 1.7f) {
                trafficCarScale.mul(1.7f / scaledHeight);
            } else if (scaledHeight < 0.65f) {
                trafficCarScale.mul(0.65f / scaledHeight);
            }

            System.out.println("Traffic car model bounds: " + bounds.min + " to " + bounds.max +
                ", scale=" + trafficCarScale + ", originOffset=" + trafficCarOriginOffset);
        }
    }

    private void preparePassengerModel() {
        if (passengerMeshes == null || passengerMeshes.isEmpty()) return;

        MeshBounds bounds = calculateMeshBounds(passengerMeshes);
        passengerOriginOffset.zero();
        passengerScale.set(1.0f);

        if (bounds.isValid()) {
            Vector3f center = bounds.getCenter();
            passengerOriginOffset.set(-center.x, -bounds.min.y, -center.z);

            float height = Math.max(bounds.getHeight(), 0.001f);
            float uniformScale = 2.0f / height;
            passengerScale.set(uniformScale, uniformScale, uniformScale);

            System.out.println("Passenger model bounds: " + bounds.min + " to " + bounds.max +
                ", scale=" + passengerScale + ", originOffset=" + passengerOriginOffset);
        }
    }

    private void calculatePlayerSpawn() {
        Vector2f geometrySpawn = findStreetGeometrySpawn();
        if (geometrySpawn != null) {
            playerSpawn.set(geometrySpawn);
            clampSpawnToCity();
            System.out.println("Player spawn from street geometry: " + playerSpawn);
            return;
        }
        System.out.println("Warning: No street geometry spawn found; using fallback spawn");

        float width = cityMaxX - cityMinX;
        float depth = cityMaxZ - cityMinZ;

        playerSpawn.set(
            cityMinX + width * 0.2f,
            cityMinZ + depth * 0.2f
        );

        List<List<Vector2f>> routes = cityMap.getTrafficRoutes();
        if (routes == null || routes.isEmpty()) return;

        float bestDistance = -1.0f;
        Vector2f bestSpawn = new Vector2f(playerSpawn);
        float[][] candidates = {
            {0.2f, 0.2f},
            {0.2f, 0.8f},
            {0.8f, 0.2f},
            {0.8f, 0.8f},
            {0.5f, 0.2f},
            {0.2f, 0.5f}
        };

        for (float[] candidate : candidates) {
            Vector2f point = new Vector2f(
                cityMinX + width * candidate[0],
                cityMinZ + depth * candidate[1]
            );
            float nearestRoute = nearestRouteDistance(point, routes);
            if (nearestRoute > bestDistance) {
                bestDistance = nearestRoute;
                bestSpawn.set(point);
            }
        }

        playerSpawn.set(bestSpawn);
        clampSpawnToCity();
    }

    private Vector2f findStreetGeometrySpawn() {
        if (roadSamples != null && !roadSamples.isEmpty()) {
            Vector2f center = new Vector2f((cityMinX + cityMaxX) * 0.5f, (cityMinZ + cityMaxZ) * 0.5f);
            Vector2f best = findSafeRoadSampleNear(center);
            if (best != null) return new Vector2f(best);
        }

        if (cityEntities == null || cityEntities.isEmpty()) return null;

        Vector2f center = new Vector2f((cityMinX + cityMaxX) * 0.5f, (cityMinZ + cityMaxZ) * 0.5f);
        Vector2f best = null;
        float bestScore = Float.MAX_VALUE;
        Vector3f local = new Vector3f();
        Vector3f world = new Vector3f();

        for (Entity entity : cityEntities) {
            if (entity == null || !entity.hasMesh() || !isStreetSpawnMesh(entity.getMesh())) continue;

            var model = entity.getModelMatrix();
            Mesh mesh = entity.getMesh();
            int step = Math.max(1, mesh.getPositionCount() / 2500);

            for (int i = 0; i < mesh.getPositionCount(); i += step) {
                mesh.getPosition(i, local);
                if (Math.abs(local.y - mesh.getMedianY()) > 25.0f) continue;

                world.set(local);
                model.transformPosition(world);
                if (world.y < -3.0f || world.y > 8.0f) continue;

                Vector2f point = new Vector2f(world.x, world.z);

                float centerScore = point.distanceSquared(center);
                float edgePenalty = edgeDistancePenalty(point);
                float score = centerScore + edgePenalty;
                if (score < bestScore && isSpawnClear(point)) {
                    bestScore = score;
                    if (best == null) best = new Vector2f();
                    best.set(point);
                }
            }
        }

        return best;
    }

    private void clampSpawnToCity() {
        playerSpawn.x = Math.max(cityMinX + 5.0f, Math.min(cityMaxX - 5.0f, playerSpawn.x));
        playerSpawn.y = Math.max(cityMinZ + 5.0f, Math.min(cityMaxZ - 5.0f, playerSpawn.y));
    }

    private boolean isStreetSpawnMesh(Mesh mesh) {
        String name = mesh.getName().toLowerCase();
        return name.contains("street") || name.contains("crosses") || name.contains("parking");
    }

    private Vector2f findSafeRoadSampleNear(Vector2f target) {
        if (roadSamples == null || roadSamples.isEmpty() || target == null) return null;

        Vector2f best = null;
        float bestScore = Float.MAX_VALUE;
        for (Vector2f sample : roadSamples) {
            float score = sample.distanceSquared(target) + edgeDistancePenalty(sample);
            if (score >= bestScore || !isSpawnClear(sample)) continue;

            bestScore = score;
            best = sample;
        }
        return best != null ? new Vector2f(best) : null;
    }

    private boolean isSpawnClear(Vector2f point) {
        return point != null &&
            !isInsideSolidCityObject(point, SPAWN_BUILDING_CLEARANCE);
    }

    private boolean isInsideSolidCityObject(Vector2f point, float padding) {
        if (cityEntities == null) return false;

        if (isInsideBuildingFootprint(point)) {
            return true;
        }

        if (padding <= 0.0f) {
            return false;
        }

        return nearestSolidCityWallDistance(point, padding) < padding;
    }

    private boolean isInsideBuildingFootprint(Vector2f point) {
        List<Float> intersections = new ArrayList<>();
        Vector3f a = new Vector3f();
        Vector3f b = new Vector3f();
        Vector3f c = new Vector3f();
        Vector3f ab = new Vector3f();
        Vector3f ac = new Vector3f();
        Vector3f normal = new Vector3f();

        for (Entity entity : cityEntities) {
            if (entity == null || !entity.hasMesh() || !isBuildingMesh(entity.getMesh())) continue;

            var model = entity.getModelMatrix();
            Mesh mesh = entity.getMesh();
            for (int i = 0; i + 2 < mesh.getIndexCount(); i += 3) {
                mesh.getPosition(mesh.getIndex(i), a);
                mesh.getPosition(mesh.getIndex(i + 1), b);
                mesh.getPosition(mesh.getIndex(i + 2), c);
                model.transformPosition(a);
                model.transformPosition(b);
                model.transformPosition(c);

                if (!isBuildingWallTriangle(a, b, c, ab, ac, normal)) continue;

                addRayIntersection(point, a, b, intersections);
                addRayIntersection(point, b, c, intersections);
                addRayIntersection(point, c, a, intersections);
            }
        }

        intersections.sort(Float::compare);
        int uniqueIntersections = 0;
        Float previous = null;
        for (Float x : intersections) {
            if (previous == null || Math.abs(x - previous) > SPAWN_RAY_INTERSECTION_EPSILON) {
                uniqueIntersections++;
                previous = x;
            }
        }

        return uniqueIntersections % 2 == 1;
    }

    private float nearestSolidCityWallDistance(Vector2f point, float maxDistance) {
        float nearest = maxDistance;
        Vector3f a = new Vector3f();
        Vector3f b = new Vector3f();
        Vector3f c = new Vector3f();
        Vector3f ab = new Vector3f();
        Vector3f ac = new Vector3f();
        Vector3f normal = new Vector3f();

        for (Entity entity : cityEntities) {
            if (entity == null || !entity.hasMesh() || !isBuildingMesh(entity.getMesh())) continue;

            var model = entity.getModelMatrix();
            Mesh mesh = entity.getMesh();
            for (int i = 0; i + 2 < mesh.getIndexCount(); i += 3) {
                mesh.getPosition(mesh.getIndex(i), a);
                mesh.getPosition(mesh.getIndex(i + 1), b);
                mesh.getPosition(mesh.getIndex(i + 2), c);
                model.transformPosition(a);
                model.transformPosition(b);
                model.transformPosition(c);

                float minX = Math.min(a.x, Math.min(b.x, c.x));
                float maxX = Math.max(a.x, Math.max(b.x, c.x));
                float minZ = Math.min(a.z, Math.min(b.z, c.z));
                float maxZ = Math.max(a.z, Math.max(b.z, c.z));
                if (point.x < minX - nearest || point.x > maxX + nearest ||
                    point.y < minZ - nearest || point.y > maxZ + nearest) {
                    continue;
                }

                if (!isBuildingWallTriangle(a, b, c, ab, ac, normal)) continue;

                nearest = Math.min(nearest, distanceToSegment2D(point, a, b));
                nearest = Math.min(nearest, distanceToSegment2D(point, b, c));
                nearest = Math.min(nearest, distanceToSegment2D(point, c, a));
                if (nearest <= 0.001f) return nearest;
            }
        }

        return nearest;
    }

    private boolean isBuildingMesh(Mesh mesh) {
        return mesh.getName().toLowerCase().contains("building");
    }

    private boolean isBuildingWallTriangle(
        Vector3f a,
        Vector3f b,
        Vector3f c,
        Vector3f ab,
        Vector3f ac,
        Vector3f normal
    ) {
        float minY = Math.min(a.y, Math.min(b.y, c.y));
        float maxY = Math.max(a.y, Math.max(b.y, c.y));
        return maxY > ROAD_CLEARANCE &&
            maxY - minY >= 1.0f &&
            surfaceVerticalShare(a, b, c, ab, ac, normal) <= 0.45f;
    }

    private void addRayIntersection(Vector2f point, Vector3f a, Vector3f b, List<Float> intersections) {
        if ((a.z > point.y) == (b.z > point.y)) return;

        float dz = b.z - a.z;
        if (Math.abs(dz) < 0.0001f) return;

        float t = (point.y - a.z) / dz;
        if (t < 0.0f || t > 1.0f) return;

        float x = a.x + t * (b.x - a.x);
        if (x > point.x) {
            intersections.add(x);
        }
    }

    private float distanceToSegment2D(Vector2f point, Vector3f a, Vector3f b) {
        float abx = b.x - a.x;
        float abz = b.z - a.z;
        float lengthSquared = abx * abx + abz * abz;
        if (lengthSquared < 0.0001f) {
            float dx = point.x - a.x;
            float dz = point.y - a.z;
            return (float) Math.sqrt(dx * dx + dz * dz);
        }

        float t = ((point.x - a.x) * abx + (point.y - a.z) * abz) / lengthSquared;
        t = Math.max(0.0f, Math.min(1.0f, t));
        float closestX = a.x + abx * t;
        float closestZ = a.z + abz * t;
        float dx = point.x - closestX;
        float dz = point.y - closestZ;
        return (float) Math.sqrt(dx * dx + dz * dz);
    }

    private float edgeDistancePenalty(Vector2f point) {
        float distanceToEdge = Math.min(
            Math.min(point.x - cityMinX, cityMaxX - point.x),
            Math.min(point.y - cityMinZ, cityMaxZ - point.y)
        );
        if (distanceToEdge >= 40.0f) return 0.0f;
        return (40.0f - distanceToEdge) * (40.0f - distanceToEdge) * 50.0f;
    }

    private float nearestRouteDistance(Vector2f point, List<List<Vector2f>> routes) {
        float nearest = Float.MAX_VALUE;
        for (List<Vector2f> route : routes) {
            if (route == null || route.isEmpty()) continue;
            for (int i = 0; i + 1 < route.size(); i++) {
                Vector2f start = route.get(i);
                Vector2f end = route.get(i + 1);
                nearest = Math.min(nearest, distanceToSegment(point, start, end));
            }
        }
        return nearest;
    }

    private float distanceToSegment(Vector2f point, Vector2f start, Vector2f end) {
        Vector2f segment = new Vector2f(end).sub(start);
        float lengthSquared = segment.lengthSquared();
        if (lengthSquared < 0.0001f) {
            return point.distance(start);
        }

        float t = new Vector2f(point).sub(start).dot(segment) / lengthSquared;
        t = Math.max(0.0f, Math.min(1.0f, t));
        Vector2f closest = new Vector2f(start).fma(t, segment);
        return point.distance(closest);
    }

    private void resetPlayerToSpawn() {
        if (playerController == null) return;

        playerController.resetPosition(playerSpawn.x, playerSpawn.y);
        applyRoadHeight(playerController.getCar());
        syncPlayerCarParts();
    }

    private void createPassengers() {
        passengers = new ArrayList<>();
        activePassenger = null;
        int count = cityMap.getPassengerCount();
        for (int i = 0; i < count; i++) {
            Vector2f pickup = cityMap.getPickup(i);
            Vector2f dropoff = cityMap.getDropoff(i);
            String name = cityMap.getPassengerName(i);
            String destination = cityMap.getPassengerDestination(i);
            if (pickup != null && dropoff != null) {
                Passenger passenger = new Passenger(pickup, dropoff, name, destination);
                passenger.setVisualEntities(
                    createPassengerVisuals(pickup, false, i),
                    createPassengerVisuals(dropoff, true, i)
                );
                passengers.add(passenger);
            }
        }
    }

    private List<Entity> createPassengerVisuals(Vector2f point, boolean destination, int index) {
        List<Entity> entities = new ArrayList<>();
        if (point == null) return entities;

        List<Mesh> meshes = passengerMeshes != null ? passengerMeshes : List.of();
        for (Mesh mesh : meshes) {
            Entity entity = new Entity(mesh, destination ? "dropoff" : "passenger");
            entity.getTransform().originOffset.set(passengerOriginOffset);
            entity.getTransform().scale.set(passengerScale);
            entity.getColor().set(destination ? new Vector3f(1.0f, 0.88f, 0.25f) : passengerColor(index));
            entity.setPosition(point.x, 0, point.y);
            applyRoadHeight(entity);
            entities.add(entity);
        }

        return entities;
    }

    private Vector3f passengerColor(int index) {
        Vector3f[] colors = {
            new Vector3f(0.95f, 1.0f, 0.85f),
            new Vector3f(0.85f, 0.95f, 1.0f),
            new Vector3f(1.0f, 0.88f, 0.88f),
            new Vector3f(0.92f, 0.86f, 1.0f),
            new Vector3f(0.90f, 1.0f, 0.92f)
        };
        return new Vector3f(colors[Math.floorMod(index, colors.length)]);
    }

    private void setupInput() {
        window.setKeyCallback((w, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS && key == GLFW_KEY_ESCAPE) {
                if (gameState.getScreen() == GameState.GameScreen.MENU && gameState.isShowInstructions()) {
                    gameState.setShowInstructions(false);
                } else if (gameState.getScreen() == GameState.GameScreen.PLAYING) {
                    gameState.setScreen(GameState.GameScreen.MENU);
                } else if (gameState.getScreen() == GameState.GameScreen.GAME_OVER) {
                    saveTypedScoreAndReturnToMenu();
                    return;
                }
                return;
            }

            if (gameState.getScreen() == GameState.GameScreen.GAME_OVER) {
                handleGameOverInput(key, action);
                return;
            }

            if (gameState.getScreen() == GameState.GameScreen.MENU) {
                handleMenuInput(key, action);
            } else if (gameState.getScreen() == GameState.GameScreen.PLAYING) {
                if (playerController != null) {
                    playerController.handleKey(key, action);
                }
            }
        });

        window.setCharCallback((w, codepoint) -> {
            if (gameState.getScreen() != GameState.GameScreen.GAME_OVER || !waitingForScoreName) {
                return;
            }

            char typed = (char) codepoint;
            if (isAllowedNameCharacter(typed) && playerNameInput.length() < MAX_PLAYER_NAME_LENGTH) {
                playerNameInput.append(typed);
            }
        });
    }

    private void handleGameOverInput(int key, int action) {
        if (action != GLFW_PRESS && action != GLFW_REPEAT) return;

        if (key == GLFW_KEY_ENTER) {
            saveTypedScoreAndReturnToMenu();
        } else if (key == GLFW_KEY_BACKSPACE && playerNameInput.length() > 0) {
            playerNameInput.deleteCharAt(playerNameInput.length() - 1);
        }
    }

    private boolean isAllowedNameCharacter(char typed) {
        return Character.isLetterOrDigit(typed) || typed == ' ' || typed == '_' || typed == '-';
    }

    private void saveTypedScoreAndReturnToMenu() {
        if (!scoreSavedForCurrentGame) {
            saveFinalScoreWithName(getTypedPlayerName());
        }
        gameState.setScreen(GameState.GameScreen.MENU);
    }

    private String getTypedPlayerName() {
        String name = playerNameInput.toString().trim();
        return name.isEmpty() ? "Player" : name;
    }

    private void handleMenuInput(int key, int action) {
        if (action != GLFW_PRESS) return;

        if (gameState.isShowInstructions()) {
            if (key == GLFW_KEY_ESCAPE) {
                gameState.setShowInstructions(false);
            }
            return;
        }

        switch (key) {
            case GLFW_KEY_UP:
                gameState.menuUp();
                break;
            case GLFW_KEY_DOWN:
                gameState.menuDown();
                break;
            case GLFW_KEY_ENTER:
                String selected = gameState.getSelectedMenuItem();
                switch (selected) {
                    case "START GAME":
                        gameState.startGame();
                        scoreSavedForCurrentGame = false;
                        waitingForScoreName = false;
                        playerNameInput.setLength(0);
                        highScores = scoreboard.loadTopScores();
                        crashCooldown = CRASH_COOLDOWN_SECONDS;
                        resetPlayerToSpawn();
                        createPassengers();
                        break;
                    case "HOW TO PLAY":
                        gameState.setShowInstructions(true);
                        break;
                    case "QUIT":
                        running = false;
                        break;
                }
                break;
        }
    }

    private void loop() {
        long lastTime = System.nanoTime();

        while (running && !window.shouldClose()) {
            long now = System.nanoTime();
            float deltaTime = (now - lastTime) / 1_000_000_000.0f;
            if (deltaTime > 0.05f) deltaTime = 0.05f; // Cap delta time
            lastTime = now;

            window.pollEvents();

            renderer.beginFrame();

            switch (gameState.getScreen()) {
                case MENU:
                    updateMenu(deltaTime);
                    break;
                case PLAYING:
                    updatePlaying(deltaTime);
                    break;
                case GAME_OVER:
                    renderGameOver();
                    break;
            }

            window.swapBuffers();
        }
    }

    private void updateMenu(float deltaTime) {
        updateObject(menu, deltaTime);
        menu.render();
    }

    private void updatePlaying(float deltaTime) {
        // Update game state
        updateObject(gameState, deltaTime);
        if (crashCooldown > 0.0f) {
            crashCooldown = Math.max(0.0f, crashCooldown - deltaTime);
        }

        // Update player
        if (playerController != null) {
            updateObject(playerController, deltaTime);
            applyRoadHeight(playerController.getCar());
            syncPlayerCarParts();
        }

        // Update traffic
        trafficUpdateFrame++;
        for (int i = 0; i < trafficCars.size(); i++) {
            TrafficCar traffic = trafficCars.get(i);
            updateObject(traffic, deltaTime);
            if (traffic.isCollidable() || ((trafficUpdateFrame + i) & 3) == 0) {
                applyRoadHeight(traffic);
            }
        }

        // Check collisions
        if (playerController != null) {
            // City bounds
            Vector3f carPos = playerController.getCar().getPosition();
            collisionSystem.enforceBounds(playerController.getCar(),
                cityMinX, cityMaxX, cityMinZ, cityMaxZ);
            collisionSystem.resolveCityCollisions(playerController.getCar(), cityEntities, 1.35f);
            applyRoadHeight(playerController.getCar());
            syncPlayerCarParts();

            // Traffic collision
            if (crashCooldown <= 0.0f) {
                for (TrafficCar traffic : trafficCars) {
                    if (!traffic.isCollidable()) continue;
                    if (collisionSystem.checkCarCollision(carPos, traffic.getPosition())) {
                        gameState.loseLife();
                        resetPlayerToSpawn();
                        crashCooldown = CRASH_COOLDOWN_SECONDS;
                        break;
                    }
                }
            }

            // Check passenger pickup/dropoff
            Vector2f playerPos2D = new Vector2f(carPos.x, carPos.z);
            for (Passenger p : passengers) {
                if (activePassenger == null && p.isAvailable() && p.isInPickupRange(playerPos2D)) {
                    p.pickUp();
                    activePassenger = p;
                    gameState.pickupPassenger(p.getDestinationName());
                } else if (p == activePassenger && p.isInDropoffRange(playerPos2D)) {
                    p.deliver();
                    activePassenger = null;
                    gameState.deliverPassenger();
                }
            }

            // Check if game is over
            if (gameState.isGameOver()) {
                prepareScoreNameEntry();
                return; // loop will render game over screen
            }
        }

        // Render scene
        renderScene();
    }

    private void updateObject(Updatable object, float deltaTime) {
        if (object != null) {
            object.update(deltaTime);
        }
    }

    private void syncPlayerCarParts() {
        if (playerCarEntities == null || playerCarEntity == null) return;

        for (Entity part : playerCarEntities) {
            if (part == playerCarEntity) continue;
            part.getTransform().position.set(playerCarEntity.getTransform().position);
            part.getTransform().rotation.set(playerCarEntity.getTransform().rotation);
        }
    }

    private void applyRoadHeight(Entity car) {
        if (car == null) return;

        Float roadY = sampleRoadHeight(
            car.getPosition().x,
            car.getPosition().z,
            car.getPosition().y - ROAD_CLEARANCE
        );
        if (roadY != null) {
            car.getTransform().position.y = roadY + ROAD_CLEARANCE;
        }
    }

    private void applyRoadHeight(TrafficCar traffic) {
        if (traffic == null || traffic.getEntity() == null) return;

        Entity anchor = traffic.getEntity();
        Float roadY = sampleRoadHeight(
            anchor.getPosition().x,
            anchor.getPosition().z,
            anchor.getPosition().y - ROAD_CLEARANCE
        );
        if (roadY == null) return;

        float y = roadY + ROAD_CLEARANCE;
        for (Entity part : traffic.getEntities()) {
            part.getTransform().position.y = y;
        }
    }

    private Float sampleRoadHeight(float x, float z, float currentRoadY) {
        List<RoadTriangle> candidates = getRoadHeightCandidates(x, z);
        if (candidates == null || candidates.isEmpty()) return null;

        Float bestLocalY = null;
        float bestLocalDelta = Float.MAX_VALUE;
        Float closestY = null;
        float closestDelta = Float.MAX_VALUE;

        for (RoadTriangle triangle : candidates) {
            if (!triangle.containsXZ(x, z)) continue;

            Float y = triangleHeightAt(x, z, triangle.a, triangle.b, triangle.c);
            if (y == null) continue;

            float delta = Math.abs(y - currentRoadY);
            if (delta < closestDelta) {
                closestDelta = delta;
                closestY = y;
            }

            boolean nearCurrentLayer = y >= currentRoadY - ROAD_HEIGHT_BAND_BELOW &&
                y <= currentRoadY + ROAD_HEIGHT_BAND_ABOVE;
            if (!nearCurrentLayer) continue;

            if (bestLocalY == null ||
                delta < bestLocalDelta - ROAD_LAYER_EPSILON ||
                (Math.abs(delta - bestLocalDelta) <= ROAD_LAYER_EPSILON && y > bestLocalY)) {
                bestLocalDelta = delta;
                bestLocalY = y;
            }
        }

        return bestLocalY != null ? bestLocalY : closestY;
    }

    private List<RoadTriangle> getRoadHeightCandidates(float x, float z) {
        if (roadHeightGrid.isEmpty()) return roadHeightTriangles;

        int cellX = roadCell(x);
        int cellZ = roadCell(z);
        List<RoadTriangle> candidates = roadHeightGrid.get(roadCellKey(cellX, cellZ));
        if (candidates != null && !candidates.isEmpty()) {
            return candidates;
        }

        List<RoadTriangle> nearby = new ArrayList<>();
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                List<RoadTriangle> cell = roadHeightGrid.get(roadCellKey(cellX + dx, cellZ + dz));
                if (cell != null) {
                    nearby.addAll(cell);
                }
            }
        }
        return nearby;
    }

    private void buildRoadHeightIndex() {
        roadHeightGrid.clear();
        roadHeightTriangles.clear();

        if (cityEntities == null) return;

        Vector3f a = new Vector3f();
        Vector3f b = new Vector3f();
        Vector3f c = new Vector3f();
        Vector3f ab = new Vector3f();
        Vector3f ac = new Vector3f();
        Vector3f normal = new Vector3f();

        for (Entity entity : cityEntities) {
            if (entity == null || !entity.hasMesh() || !isRoadHeightMesh(entity.getMesh())) continue;

            Mesh mesh = entity.getMesh();
            var model = entity.getModelMatrix();
            for (int i = 0; i + 2 < mesh.getIndexCount(); i += 3) {
                mesh.getPosition(mesh.getIndex(i), a);
                mesh.getPosition(mesh.getIndex(i + 1), b);
                mesh.getPosition(mesh.getIndex(i + 2), c);
                model.transformPosition(a);
                model.transformPosition(b);
                model.transformPosition(c);

                if (!isRoadSurfaceTriangle(a, b, c, ab, ac, normal)) continue;

                RoadTriangle triangle = new RoadTriangle(a, b, c);
                if (!triangle.isUsable()) continue;

                roadHeightTriangles.add(triangle);
                addRoadTriangleToGrid(triangle);
            }
        }

        System.out.println("Indexed " + roadHeightTriangles.size() +
            " road triangles across " + roadHeightGrid.size() + " height cells");
    }

    private void addRoadTriangleToGrid(RoadTriangle triangle) {
        int minCellX = roadCell(triangle.minX);
        int maxCellX = roadCell(triangle.maxX);
        int minCellZ = roadCell(triangle.minZ);
        int maxCellZ = roadCell(triangle.maxZ);

        for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
            for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
                long key = roadCellKey(cellX, cellZ);
                roadHeightGrid.computeIfAbsent(key, ignored -> new ArrayList<>()).add(triangle);
            }
        }
    }

    private int roadCell(float value) {
        return (int) Math.floor(value / ROAD_HEIGHT_GRID_CELL_SIZE);
    }

    private long roadCellKey(int cellX, int cellZ) {
        return (((long) cellX) << 32) ^ (cellZ & 0xffffffffL);
    }

    private boolean isRoadSurfaceTriangle(
        Vector3f a,
        Vector3f b,
        Vector3f c,
        Vector3f ab,
        Vector3f ac,
        Vector3f normal
    ) {
        return surfaceVerticalShare(a, b, c, ab, ac, normal) >= ROAD_MIN_SURFACE_NORMAL_Y;
    }

    private float surfaceVerticalShare(
        Vector3f a,
        Vector3f b,
        Vector3f c,
        Vector3f ab,
        Vector3f ac,
        Vector3f normal
    ) {
        ab.set(b).sub(a);
        ac.set(c).sub(a);
        normal.set(ab).cross(ac);

        float lengthSquared = normal.lengthSquared();
        if (lengthSquared < 0.0001f) return 0.0f;

        return Math.abs(normal.y) / (float) Math.sqrt(lengthSquared);
    }

    private Float triangleHeightAt(float x, float z, Vector3f a, Vector3f b, Vector3f c) {
        float denom = (b.z - c.z) * (a.x - c.x) + (c.x - b.x) * (a.z - c.z);
        if (Math.abs(denom) < 0.0001f) return null;

        float u = ((b.z - c.z) * (x - c.x) + (c.x - b.x) * (z - c.z)) / denom;
        float v = ((c.z - a.z) * (x - c.x) + (a.x - c.x) * (z - c.z)) / denom;
        float w = 1.0f - u - v;

        float epsilon = 0.02f;
        if (u < -epsilon || v < -epsilon || w < -epsilon) return null;
        return u * a.y + v * b.y + w * c.y;
    }

    private boolean isRoadHeightMesh(Mesh mesh) {
        String name = mesh.getName().toLowerCase();
        boolean roadLike = name.contains("street") ||
            name.contains("crosses") ||
            name.contains("parking") ||
            name.contains("bridge") ||
            name.contains("tunnel");
        return roadLike &&
            !name.contains("lightmap") &&
            !name.contains("collision") &&
            !name.contains("invisible");
    }

    private void renderScene() {
        // Switch to 3D mode for the scene (enables depth test)
        renderer.begin3D();

        // Render all city entities first
        if (cityEntities != null) {
            renderer.renderEntities(cityEntities, camera, camera.getPosition());
        }

        // Render player car
        if (playerCarEntities != null) {
            renderer.renderEntities(playerCarEntities, camera, camera.getPosition());
        } else if (playerCarEntity != null) {
            renderer.renderEntity(playerCarEntity, camera, camera.getPosition());
        }

        // Render traffic cars
        if (trafficCars != null) {
            for (TrafficCar traffic : trafficCars) {
                renderer.renderEntities(traffic.getEntities(), camera, camera.getPosition());
            }
        }

        renderPassengerModels();

        String passengerInfo = buildPassengerInfo();

        // Switch to 2D mode for HUD overlay
        renderer.begin2D();

        // Render passenger pickup and dropoff markers projected onto the HUD layer.
        renderPassengerMarkers();

        hud.render(playerController != null ? playerController.getSpeed() : 0, passengerInfo);
    }

    private void renderPassengerModels() {
        if (passengers == null) return;

        for (Passenger passenger : passengers) {
            List<Entity> visibleEntities = passenger.getVisibleEntities();
            if (!visibleEntities.isEmpty()) {
                renderer.renderEntities(visibleEntities, camera, camera.getPosition());
            }
        }
    }

    private String buildPassengerInfo() {
        if (playerController == null) return null;

        if (activePassenger != null && !activePassenger.isDelivered()) {
            Vector2f playerPos = new Vector2f(
                playerController.getCar().getPosition().x,
                playerController.getCar().getPosition().z
            );
            float distance = playerPos.distance(activePassenger.getDropoffLocation());
            return String.format("DROP OFF: %s -> %s (%.0fm)",
                activePassenger.getName(),
                activePassenger.getDestinationName(),
                distance * 2.0f
            );
        }

        return findNearestPassengerInfo();
    }

    private String findNearestPassengerInfo() {
        if (playerController == null || passengers == null) return null;

        Vector2f playerPos = new Vector2f(
            playerController.getCar().getPosition().x,
            playerController.getCar().getPosition().z
        );

        float nearestDist = Float.MAX_VALUE;
        Passenger nearest = null;

        for (Passenger p : passengers) {
            if (!p.isAvailable()) continue;
            Vector2f pickup = p.getPickupLocation();
            float dist = playerPos.distance(pickup);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = p;
            }
        }

        if (nearest != null) {
            String distStr = String.format(" (%.0fm away)", nearestDist * 2);
            return "PICK UP: " + nearest.getName() + distStr;
        }
        return null;
    }

    private void renderPassengerMarkers() {
        if (passengers == null || camera == null) return;

        for (Passenger p : passengers) {
            if (p.isDelivered()) continue;

            if (p == activePassenger) {
                render3DMarker(getPassengerMarkerPosition(p), 1.0f, 1.0f, 0.2f);
            } else if (!p.isPickedUp()) {
                // Render pickup marker - green
                render3DMarker(getPassengerMarkerPosition(p), 0.2f, 1.0f, 0.2f);
            }
        }
    }

    private Vector3f getPassengerMarkerPosition(Passenger passenger) {
        List<Entity> visibleEntities = passenger.getVisibleEntities();
        if (!visibleEntities.isEmpty()) {
            return new Vector3f(visibleEntities.get(0).getPosition()).add(0.0f, 2.4f, 0.0f);
        }
        return passenger.isPickedUp() ? passenger.getDropoffPosition3D() : passenger.getPickupPosition3D();
    }

    private void render3DMarker(Vector3f worldPos, float r, float g, float b) {
        if (camera == null) return;

        // Project 3D position to screen
        var view = camera.getViewMatrix();
        var proj = camera.getProjectionMatrix();

        var clip = new org.joml.Matrix4f(proj).mul(view);
        var screenPos = new org.joml.Vector4f(worldPos.x, worldPos.y, worldPos.z, 1.0f);
        clip.transform(screenPos);

        if (screenPos.w <= 0) return;

        float ndcX = screenPos.x / screenPos.w;
        float ndcY = screenPos.y / screenPos.w;

        // Convert to screen coordinates
        float screenX = (ndcX + 1.0f) * 0.5f * 1280;
        float screenY = (1.0f - ndcY) * 0.5f * 720;

        // Draw a small diamond marker
        float markerSize = 6;
        // Draw diamond shape using rotated rect
        renderer.drawRect(screenX - markerSize, screenY - markerSize * 0.5f,
            markerSize * 2, markerSize, r, g, b, 0.8f);
        renderer.drawRect(screenX - markerSize * 0.5f, screenY - markerSize,
            markerSize, markerSize * 2, r, g, b, 0.8f);
    }

    private void renderGameOver() {
        prepareScoreNameEntry();
        menu.renderGameOver(highScores, playerNameInput.toString(), scoreSavedForCurrentGame);
    }

    private void prepareScoreNameEntry() {
        if (waitingForScoreName || scoreSavedForCurrentGame || scoreboard == null || !gameState.isGameOver()) {
            return;
        }

        highScores = scoreboard.loadTopScores();
        playerNameInput.setLength(0);
        waitingForScoreName = true;
    }

    private void saveFinalScoreWithName(String playerName) {
        if (scoreSavedForCurrentGame || scoreboard == null) {
            return;
        }

        ScoreEntry scoreEntry = new ScoreEntry(playerName, gameState.getScore(), gameState.getGameTime());
        scoreboard.addScore(scoreEntry);
        highScores = scoreboard.loadTopScores();
        waitingForScoreName = false;
        scoreSavedForCurrentGame = true;
    }

    private void generateFallbackCity() {
        // Generate a simple flat grid as fallback city
        System.out.println("Generating fallback city geometry");
        cityEntities = new ArrayList<>();

        // Create a large ground plane
        float size = 100;
        float[] vertices = {
            -size, -0.1f, -size,  0, 1, 0,  0, 0,
             size, -0.1f, -size,  0, 1, 0,  1, 0,
             size, -0.1f,  size,  0, 1, 0,  1, 1,
            -size, -0.1f,  size,  0, 1, 0,  0, 1
        };
        int[] indices = { 0, 1, 2, 0, 2, 3 };
        Mesh groundMesh = new Mesh(vertices, indices, true);
        cityMeshes = new ArrayList<>();
        cityMeshes.add(groundMesh);
        Entity ground = new Entity(groundMesh, "city");
        ground.getColor().set(0.2f, 0.3f, 0.2f);
        cityEntities.add(ground);

        // Add some simple buildings as boxes
        float[][] buildingPositions = {
            {-20, 0, -20}, {-20, 0, -10}, {-20, 0, 0}, {-20, 0, 10}, {-20, 0, 20},
            {-10, 0, -20}, {-10, 0, 20},
            {0, 0, -20}, {0, 0, 20},
            {10, 0, -20}, {10, 0, 20},
            {20, 0, -20}, {20, 0, -10}, {20, 0, 0}, {20, 0, 10}, {20, 0, 20},
            {-15, 0, -15}, {-15, 0, 15}, {15, 0, -15}, {15, 0, 15}
        };

        for (float[] pos : buildingPositions) {
            float w = 2 + (float) Math.random() * 4;
            float h = 2 + (float) Math.random() * 8;
            float d = 2 + (float) Math.random() * 4;

            float[] bv = createBoxVertices(pos[0], 0, pos[2], w, h, d);
            int[] bi = createBoxIndices();
            Mesh buildingMesh = new Mesh(bv, bi, false);
            cityMeshes.add(buildingMesh);
            Entity building = new Entity(buildingMesh, "city");
            building.getColor().set(
                0.4f + (float) Math.random() * 0.3f,
                0.4f + (float) Math.random() * 0.3f,
                0.5f + (float) Math.random() * 0.3f
            );
            cityEntities.add(building);
        }
    }

    private MeshBounds calculateMeshBounds(List<Mesh> meshes) {
        MeshBounds bounds = new MeshBounds();
        if (meshes == null) return bounds;

        for (Mesh mesh : meshes) {
            if (mesh == null) continue;
            bounds.include(mesh.getMinBounds());
            bounds.include(mesh.getMaxBounds());
        }
        return bounds;
    }

    private MeshBounds calculateRenderableCityBounds() {
        MeshBounds bounds = new MeshBounds();
        if (cityMeshes == null) return bounds;

        for (Mesh mesh : cityMeshes) {
            if (mesh == null || shouldSkipCityMesh(mesh)) continue;
            bounds.include(mesh.getMinBounds());
            bounds.include(mesh.getMaxBounds());
        }

        if (!bounds.isValid()) {
            return calculateMeshBounds(cityMeshes);
        }
        return bounds;
    }

    private MeshBounds calculateStreetBounds() {
        MeshBounds bounds = new MeshBounds();
        if (cityMeshes == null) return bounds;

        for (Mesh mesh : cityMeshes) {
            if (mesh == null) continue;
            String name = mesh.getName().toLowerCase();
            if (name.contains("street") || name.contains("crosses") || name.contains("parking")) {
                bounds.include(mesh.getMinBounds());
                bounds.include(mesh.getMaxBounds());
            }
        }
        return bounds;
    }

    private float calculateStreetGroundY(MeshBounds fallbackBounds) {
        float weightedY = 0.0f;
        int totalVertices = 0;

        if (cityMeshes != null) {
            for (Mesh mesh : cityMeshes) {
                if (mesh == null) continue;

                String name = mesh.getName().toLowerCase();
                if (name.contains("lightmap")) continue;
                if (name.contains("street") || name.contains("crosses") || name.contains("parking")) {
                    int weight = Math.max(1, mesh.getVertexCount());
                    weightedY += mesh.getMedianY() * weight;
                    totalVertices += weight;
                }
            }
        }

        if (totalVertices > 0) {
            return weightedY / totalVertices;
        }
        return fallbackBounds.min.y;
    }

    private boolean shouldSkipCityMesh(Mesh mesh) {
        String name = mesh.getName().toLowerCase();
        return name.contains("collision") ||
            name.contains("invisible");
    }

    private static class MeshBounds {
        final Vector3f min = new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
        final Vector3f max = new Vector3f(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);

        void include(Vector3f point) {
            if (point == null) return;
            if (point.x < min.x) min.x = point.x;
            if (point.y < min.y) min.y = point.y;
            if (point.z < min.z) min.z = point.z;
            if (point.x > max.x) max.x = point.x;
            if (point.y > max.y) max.y = point.y;
            if (point.z > max.z) max.z = point.z;
        }

        boolean isValid() {
            return min.x != Float.MAX_VALUE && max.x != -Float.MAX_VALUE;
        }

        Vector3f getCenter() {
            return new Vector3f(min).add(max).mul(0.5f);
        }

        float getWidth() {
            return max.x - min.x;
        }

        float getHeight() {
            return max.y - min.y;
        }

        float getDepth() {
            return max.z - min.z;
        }
    }

    private static class RoadTriangle {
        final Vector3f a;
        final Vector3f b;
        final Vector3f c;
        final float minX;
        final float maxX;
        final float minZ;
        final float maxZ;

        RoadTriangle(Vector3f a, Vector3f b, Vector3f c) {
            this.a = new Vector3f(a);
            this.b = new Vector3f(b);
            this.c = new Vector3f(c);
            minX = Math.min(a.x, Math.min(b.x, c.x));
            maxX = Math.max(a.x, Math.max(b.x, c.x));
            minZ = Math.min(a.z, Math.min(b.z, c.z));
            maxZ = Math.max(a.z, Math.max(b.z, c.z));
        }

        boolean isUsable() {
            return maxX - minX > 0.001f && maxZ - minZ > 0.001f;
        }

        boolean containsXZ(float x, float z) {
            return x >= minX - 0.02f &&
                x <= maxX + 0.02f &&
                z >= minZ - 0.02f &&
                z <= maxZ + 0.02f;
        }
    }

    private float[] createBoxVertices(float cx, float cy, float cz, float w, float h, float d) {
        float hw = w / 2, hh = h / 2, hd = d / 2;
        // 6 floats per vertex: pos(3) + normal(3) — no texcoords (meshes use hasTexCoords=false)
        return new float[] {
            // Front face
            cx - hw, cy - hh, cz + hd,  0, 0, 1,
            cx + hw, cy - hh, cz + hd,  0, 0, 1,
            cx + hw, cy + hh, cz + hd,  0, 0, 1,
            cx - hw, cy + hh, cz + hd,  0, 0, 1,
            // Back face
            cx + hw, cy - hh, cz - hd,  0, 0, -1,
            cx - hw, cy - hh, cz - hd,  0, 0, -1,
            cx - hw, cy + hh, cz - hd,  0, 0, -1,
            cx + hw, cy + hh, cz - hd,  0, 0, -1,
            // Top face
            cx - hw, cy + hh, cz + hd,  0, 1, 0,
            cx + hw, cy + hh, cz + hd,  0, 1, 0,
            cx + hw, cy + hh, cz - hd,  0, 1, 0,
            cx - hw, cy + hh, cz - hd,  0, 1, 0,
            // Bottom face
            cx - hw, cy - hh, cz - hd,  0, -1, 0,
            cx + hw, cy - hh, cz - hd,  0, -1, 0,
            cx + hw, cy - hh, cz + hd,  0, -1, 0,
            cx - hw, cy - hh, cz + hd,  0, -1, 0,
            // Right face
            cx + hw, cy - hh, cz + hd,  1, 0, 0,
            cx + hw, cy - hh, cz - hd,  1, 0, 0,
            cx + hw, cy + hh, cz - hd,  1, 0, 0,
            cx + hw, cy + hh, cz + hd,  1, 0, 0,
            // Left face
            cx - hw, cy - hh, cz - hd,  -1, 0, 0,
            cx - hw, cy - hh, cz + hd,  -1, 0, 0,
            cx - hw, cy + hh, cz + hd,  -1, 0, 0,
            cx - hw, cy + hh, cz - hd,  -1, 0, 0
        };
    }

    private int[] createBoxIndices() {
        return new int[] {
            0, 1, 2, 0, 2, 3,
            4, 5, 6, 4, 6, 7,
            8, 9, 10, 8, 10, 11,
            12, 13, 14, 12, 14, 15,
            16, 17, 18, 16, 18, 19,
            20, 21, 22, 20, 22, 23
        };
    }

    private void generateFallbackCar() {
        System.out.println("Generating fallback car geometry");
        // Generate a simple box car (vertices use 6 floats: pos+normal, hasTexCoords=false)
        float[] vertices = createBoxVertices(0, 0, 0, 1.5f, 0.8f, 3.0f);
        int[] indices = createBoxIndices();
        Mesh carMesh = new Mesh(vertices, indices, false);
        carMeshes = new ArrayList<>();
        carMeshes.add(carMesh);
        createPlayerCar();
    }

    private void generateFallbackPassengerModel() {
        float[] vertices = createBoxVertices(0, 0.9f, 0, 0.45f, 1.8f, 0.35f);
        int[] indices = createBoxIndices();
        Mesh passengerMesh = new Mesh(vertices, indices, false, "fallback_passenger");
        passengerMeshes = new ArrayList<>();
        passengerMeshes.add(passengerMesh);
        preparePassengerModel();
    }

    private void cleanup() {
        // Release resources
        if (cityEntities != null) {
            for (Entity e : cityEntities) {
                if (e.hasMesh()) e.getMesh().close();
            }
        }
        if (carMeshes != null) {
            for (Mesh m : carMeshes) m.close();
        }
        if (trafficCarMeshes != null) {
            for (Mesh m : trafficCarMeshes) m.close();
        }
        if (passengerMeshes != null) {
            for (Mesh m : passengerMeshes) m.close();
        }
        if (ambientTrafficMesh != null) {
            ambientTrafficMesh.close();
        }
        if (renderer != null) renderer.close();
        if (window != null) window.close();
    }
}
