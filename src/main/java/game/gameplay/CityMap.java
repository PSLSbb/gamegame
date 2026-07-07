package game.gameplay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.joml.Vector2f;

public class CityMap {
    // Road waypoints for traffic cars to follow
    private List<List<Vector2f>> trafficRoutes;
    private List<Vector2f> passengerPickups;
    private List<Vector2f> passengerDropoffs;
    private List<String> passengerNames;
    private List<String> passengerDestinations;
    private List<Vector2f> roadSamples;
    private static final int ROAD_ROUTE_SIDE_POINTS = 34;

    // City bounds
    private float minX, maxX, minZ, maxZ;

    public CityMap(float minX, float maxX, float minZ, float maxZ) {
        this(minX, maxX, minZ, maxZ, null);
    }

    public CityMap(float minX, float maxX, float minZ, float maxZ, List<Vector2f> roadSamples) {
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.roadSamples = roadSamples != null ? new ArrayList<>(roadSamples) : new ArrayList<>();
        generateRoutes();
        generatePassengerLocations();
    }

    private void generateRoutes() {
        trafficRoutes = new ArrayList<>();

        if (roadSamples != null && roadSamples.size() >= 8) {
            generateRoadSampleRoutes();
            return;
        }

        // Generate several rectangular routes around the city
        float cx = (minX + maxX) / 2;
        float cz = (minZ + maxZ) / 2;
        float hw = (maxX - minX) * 0.35f;
        float hz = (maxZ - minZ) * 0.35f;

        // Outer loop
        List<Vector2f> route1 = new ArrayList<>();
        route1.add(new Vector2f(cx - hw, cz - hz));
        route1.add(new Vector2f(cx + hw, cz - hz));
        route1.add(new Vector2f(cx + hw, cz + hz));
        route1.add(new Vector2f(cx - hw, cz + hz));
        trafficRoutes.add(route1);

        // Inner loop
        List<Vector2f> route2 = new ArrayList<>();
        route2.add(new Vector2f(cx - hw * 0.5f, cz - hz * 0.5f));
        route2.add(new Vector2f(cx + hw * 0.5f, cz - hz * 0.5f));
        route2.add(new Vector2f(cx + hw * 0.5f, cz + hz * 0.5f));
        route2.add(new Vector2f(cx - hw * 0.5f, cz + hz * 0.5f));
        trafficRoutes.add(route2);

        // Cross route 1
        List<Vector2f> route3 = new ArrayList<>();
        route3.add(new Vector2f(cx - hw, cz));
        route3.add(new Vector2f(cx + hw, cz));
        trafficRoutes.add(route3);

        // Cross route 2
        List<Vector2f> route4 = new ArrayList<>();
        route4.add(new Vector2f(cx, cz - hz));
        route4.add(new Vector2f(cx, cz + hz));
        trafficRoutes.add(route4);
    }

    private void generateRoadSampleRoutes() {
        float maxStep = estimateRoadStep();
        int routeCount = Math.min(32, Math.max(8, roadSamples.size() / 28));
        List<Integer> seeds = sortedRoadSampleIndexesByAngle();

        for (int i = 0; i < routeCount && !seeds.isEmpty(); i++) {
            int seedIndex = seeds.get((i * seeds.size()) / routeCount);
            float angle = (float) (Math.PI * 2.0 * i / routeCount);
            Vector2f heading = new Vector2f((float) Math.cos(angle), (float) Math.sin(angle));
            addRouteIfUseful(traceRoadRoute(seedIndex, heading, maxStep));
        }

        if (trafficRoutes.isEmpty()) {
            List<Vector2f> fallback = new ArrayList<>();
            fallback.add(new Vector2f(roadSamples.get(0)));
            fallback.add(new Vector2f(roadSamples.get(roadSamples.size() / 2)));
            trafficRoutes.add(fallback);
        }
    }

    private List<Integer> sortedRoadSampleIndexesByAngle() {
        Vector2f center = getRoadCenter();
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < roadSamples.size(); i++) {
            indexes.add(i);
        }
        Collections.sort(indexes, (a, b) -> {
            Vector2f pa = roadSamples.get(a);
            Vector2f pb = roadSamples.get(b);
            float angleA = (float) Math.atan2(pa.y - center.y, pa.x - center.x);
            float angleB = (float) Math.atan2(pb.y - center.y, pb.x - center.x);
            int angleCompare = Float.compare(angleA, angleB);
            if (angleCompare != 0) return angleCompare;
            return Float.compare(pa.distanceSquared(center), pb.distanceSquared(center));
        });
        return indexes;
    }

    private List<Vector2f> traceRoadRoute(int seedIndex, Vector2f heading, float maxStep) {
        List<Integer> backward = traceRoadIndexes(seedIndex, new Vector2f(heading).negate(), maxStep);
        List<Integer> forward = traceRoadIndexes(seedIndex, heading, maxStep);

        List<Vector2f> route = new ArrayList<>();
        for (int i = backward.size() - 1; i >= 1; i--) {
            route.add(new Vector2f(roadSamples.get(backward.get(i))));
        }
        for (Integer index : forward) {
            route.add(new Vector2f(roadSamples.get(index)));
        }

        return simplifyRoute(route, Math.max(4.0f, maxStep * 0.35f));
    }

    private List<Integer> traceRoadIndexes(int seedIndex, Vector2f heading, float maxStep) {
        List<Integer> route = new ArrayList<>();
        boolean[] visited = new boolean[roadSamples.size()];
        Vector2f direction = new Vector2f(heading);
        if (direction.lengthSquared() < 0.0001f) {
            direction.set(1.0f, 0.0f);
        } else {
            direction.normalize();
        }

        int current = seedIndex;
        route.add(current);
        visited[current] = true;

        for (int i = 0; i < ROAD_ROUTE_SIDE_POINTS; i++) {
            int next = findNextRoadSample(current, visited, direction, maxStep);
            if (next < 0) break;

            Vector2f currentPoint = roadSamples.get(current);
            Vector2f nextPoint = roadSamples.get(next);
            direction.set(nextPoint).sub(currentPoint);
            if (direction.lengthSquared() > 0.0001f) {
                direction.normalize();
            }

            current = next;
            visited[current] = true;
            route.add(current);
        }

        return route;
    }

    private int findNextRoadSample(int currentIndex, boolean[] visited, Vector2f direction, float maxStep) {
        Vector2f current = roadSamples.get(currentIndex);
        float maxStepSquared = maxStep * maxStep;
        float minStep = Math.max(2.5f, maxStep * 0.18f);
        float minStepSquared = minStep * minStep;
        int best = -1;
        float bestScore = Float.MAX_VALUE;
        int relaxedBest = -1;
        float relaxedBestScore = Float.MAX_VALUE;
        Vector2f candidateDirection = new Vector2f();

        for (int i = 0; i < roadSamples.size(); i++) {
            if (visited[i] || i == currentIndex) continue;

            Vector2f candidate = roadSamples.get(i);
            float distanceSquared = candidate.distanceSquared(current);
            if (distanceSquared < minStepSquared || distanceSquared > maxStepSquared) continue;

            float distance = (float) Math.sqrt(distanceSquared);
            candidateDirection.set(candidate).sub(current).normalize();
            float forwardness = candidateDirection.dot(direction);
            float score = distance * (1.0f + Math.max(0.0f, 0.75f - forwardness));

            if (forwardness >= -0.15f && score < bestScore) {
                bestScore = score;
                best = i;
            }
            if (forwardness >= -0.75f && score < relaxedBestScore) {
                relaxedBestScore = score;
                relaxedBest = i;
            }
        }

        return best >= 0 ? best : relaxedBest;
    }

    private float estimateRoadStep() {
        List<Float> nearestDistances = new ArrayList<>();
        int stride = Math.max(1, roadSamples.size() / 220);

        for (int i = 0; i < roadSamples.size(); i += stride) {
            Vector2f sample = roadSamples.get(i);
            float nearest = Float.MAX_VALUE;
            for (int j = 0; j < roadSamples.size(); j++) {
                if (i == j) continue;
                nearest = Math.min(nearest, sample.distanceSquared(roadSamples.get(j)));
            }
            if (nearest < Float.MAX_VALUE) {
                nearestDistances.add((float) Math.sqrt(nearest));
            }
        }

        if (nearestDistances.isEmpty()) {
            return 25.0f;
        }

        Collections.sort(nearestDistances);
        float median = nearestDistances.get(nearestDistances.size() / 2);
        return clamp(median * 5.0f, 12.0f, 55.0f);
    }

    private List<Vector2f> simplifyRoute(List<Vector2f> route, float minSpacing) {
        if (route == null || route.size() < 2) return route;

        List<Vector2f> simplified = new ArrayList<>();
        float minSpacingSquared = minSpacing * minSpacing;
        simplified.add(route.get(0));

        for (int i = 1; i < route.size(); i++) {
            Vector2f point = route.get(i);
            Vector2f previous = simplified.get(simplified.size() - 1);
            if (point.distanceSquared(previous) >= minSpacingSquared) {
                simplified.add(point);
            }
        }

        Vector2f last = route.get(route.size() - 1);
        if (!last.equals(simplified.get(simplified.size() - 1))) {
            simplified.add(last);
        }
        return simplified;
    }

    private void addRouteIfUseful(List<Vector2f> route) {
        if (route == null || route.size() < 2) return;

        List<Vector2f> unique = new ArrayList<>();
        for (Vector2f point : route) {
            if (point == null) continue;
            boolean duplicate = false;
            for (Vector2f existing : unique) {
                if (existing.distanceSquared(point) < 25.0f) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                unique.add(point);
            }
        }

        if (unique.size() >= 4 && routeLength(unique) >= 35.0f) {
            trafficRoutes.add(unique);
        }
    }

    private float routeLength(List<Vector2f> route) {
        float length = 0.0f;
        for (int i = 1; i < route.size(); i++) {
            length += route.get(i).distance(route.get(i - 1));
        }
        return length;
    }

    private Vector2f getRoadCenter() {
        if (roadSamples == null || roadSamples.isEmpty()) {
            return new Vector2f((minX + maxX) * 0.5f, (minZ + maxZ) * 0.5f);
        }

        float sumX = 0.0f;
        float sumZ = 0.0f;
        for (Vector2f sample : roadSamples) {
            sumX += sample.x;
            sumZ += sample.y;
        }
        return new Vector2f(sumX / roadSamples.size(), sumZ / roadSamples.size());
    }

    private Vector2f findRoadPoint(Vector2f center, float normalizedX, float normalizedZ) {
        float targetX = center.x + normalizedX * (maxX - minX) * 0.5f;
        float targetZ = center.y + normalizedZ * (maxZ - minZ) * 0.5f;
        Vector2f target = new Vector2f(targetX, targetZ);

        Vector2f best = null;
        float bestDistance = Float.MAX_VALUE;
        for (Vector2f sample : roadSamples) {
            float distance = sample.distanceSquared(target);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = sample;
            }
        }
        return best != null ? new Vector2f(best) : target;
    }

    private void generatePassengerLocations() {
        passengerPickups = new ArrayList<>();
        passengerDropoffs = new ArrayList<>();
        passengerNames = new ArrayList<>();
        passengerDestinations = new ArrayList<>();

        if (roadSamples != null && roadSamples.size() >= 10) {
            generateRoadSamplePassengerLocations();
            return;
        }

        float cx = (minX + maxX) / 2;
        float cz = (minZ + maxZ) / 2;
        float hw = (maxX - minX) * 0.4f;
        float hz = (maxZ - minZ) * 0.4f;

        passengerPickups.add(new Vector2f(cx - hw * 0.7f, cz - hz * 0.3f));
        passengerDropoffs.add(new Vector2f(cx + hw * 0.5f, cz + hz * 0.6f));
        passengerNames.add("Alice");
        passengerDestinations.add("Harbor terminal");

        passengerPickups.add(new Vector2f(cx + hw * 0.6f, cz - hz * 0.5f));
        passengerDropoffs.add(new Vector2f(cx - hw * 0.4f, cz + hz * 0.4f));
        passengerNames.add("Bob");
        passengerDestinations.add("Market square");

        passengerPickups.add(new Vector2f(cx - hw * 0.2f, cz + hz * 0.7f));
        passengerDropoffs.add(new Vector2f(cx + hw * 0.3f, cz - hz * 0.6f));
        passengerNames.add("Charlie");
        passengerDestinations.add("South station");

        passengerPickups.add(new Vector2f(cx + hw * 0.3f, cz + hz * 0.2f));
        passengerDropoffs.add(new Vector2f(cx - hw * 0.6f, cz - hz * 0.4f));
        passengerNames.add("Diana");
        passengerDestinations.add("Old town");

        passengerPickups.add(new Vector2f(cx - hw * 0.5f, cz + hz * 0.5f));
        passengerDropoffs.add(new Vector2f(cx + hw * 0.7f, cz - hz * 0.3f));
        passengerNames.add("Eve");
        passengerDestinations.add("East garages");
    }

    private void generateRoadSamplePassengerLocations() {
        Vector2f center = getRoadCenter();
        float[][] pickupTargets = {
            {-0.60f, -0.18f},
            {0.55f, -0.32f},
            {-0.20f, 0.55f},
            {0.28f, 0.18f},
            {-0.48f, 0.36f}
        };
        float[][] dropoffTargets = {
            {0.44f, 0.45f},
            {-0.36f, 0.28f},
            {0.25f, -0.45f},
            {-0.55f, -0.28f},
            {0.62f, -0.18f}
        };
        String[] names = {"Alice", "Bob", "Charlie", "Diana", "Eve"};
        String[] destinations = {
            "Harbor terminal",
            "Market square",
            "South station",
            "Old town",
            "East garages"
        };

        for (int i = 0; i < names.length; i++) {
            passengerPickups.add(findRoadPoint(center, pickupTargets[i][0], pickupTargets[i][1]));
            passengerDropoffs.add(findRoadPoint(center, dropoffTargets[i][0], dropoffTargets[i][1]));
            passengerNames.add(names[i]);
            passengerDestinations.add(destinations[i]);
        }
    }

    public List<List<Vector2f>> getTrafficRoutes() { return trafficRoutes; }
    public List<Vector2f> getRoadSamples() { return roadSamples; }
    public int getPassengerCount() { return passengerPickups.size(); }

    public Vector2f getPickup(int index) {
        return index < passengerPickups.size() ? passengerPickups.get(index) : null;
    }

    public Vector2f getDropoff(int index) {
        return index < passengerDropoffs.size() ? passengerDropoffs.get(index) : null;
    }

    public String getPassengerName(int index) {
        return index < passengerNames.size() ? passengerNames.get(index) : "Unknown";
    }

    public String getPassengerDestination(int index) {
        return index < passengerDestinations.size() ? passengerDestinations.get(index) : "Destination";
    }

    /**
     * Get random spawn position for traffic cars along routes
     */
    public Vector2f getTrafficSpawnPoint(int routeIndex) {
        if (routeIndex >= trafficRoutes.size()) routeIndex = 0;
        List<Vector2f> route = trafficRoutes.get(routeIndex);
        if (route.isEmpty()) return new Vector2f(0, 0);
        return route.get(0);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
