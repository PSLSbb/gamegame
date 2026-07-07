package game.gameplay;

import java.util.List;

import org.joml.Vector2f;
import org.joml.Vector3f;

import game.engine.Renderer;

public class MiniMap {
    private static final float X = 20.0f;
    private static final float Y = 70.0f;
    private static final float SIZE = 170.0f;
    private static final float PADDING = 10.0f;

    private final Renderer renderer;

    public MiniMap(Renderer renderer) {
        this.renderer = renderer;
    }

    public void render(
        float minX,
        float maxX,
        float minZ,
        float maxZ,
        CityMap cityMap,
        List<Vector2f> roadSamples,
        Vector3f playerPosition,
        Vector3f playerForward,
        List<TrafficCar> trafficCars,
        List<Passenger> passengers,
        Vector2f playerSpawn
    ) {
        if (maxX <= minX || maxZ <= minZ) return;

        renderer.drawRect(X - 2.0f, Y - 2.0f, SIZE + 4.0f, SIZE + 4.0f, 0.02f, 0.03f, 0.05f, 0.82f);
        renderer.drawRect(X, Y, SIZE, SIZE, 0.06f, 0.08f, 0.10f, 0.78f);

        drawGrid();
        drawRoadSamples(roadSamples, minX, maxX, minZ, maxZ);
        drawRoutes(cityMap, minX, maxX, minZ, maxZ);

        if (playerSpawn != null) {
            drawPoint(playerSpawn.x, playerSpawn.y, minX, maxX, minZ, maxZ, 2.6f, 0.55f, 0.65f, 1.0f, 0.9f);
        }

        drawPassengers(passengers, minX, maxX, minZ, maxZ);
        drawTraffic(trafficCars, minX, maxX, minZ, maxZ);
        drawPlayer(playerPosition, playerForward, minX, maxX, minZ, maxZ);

        renderer.drawRect(X, Y, SIZE, 1.5f, 0.55f, 0.70f, 0.85f, 0.55f);
        renderer.drawRect(X, Y + SIZE - 1.5f, SIZE, 1.5f, 0.55f, 0.70f, 0.85f, 0.55f);
        renderer.drawRect(X, Y, 1.5f, SIZE, 0.55f, 0.70f, 0.85f, 0.55f);
        renderer.drawRect(X + SIZE - 1.5f, Y, 1.5f, SIZE, 0.55f, 0.70f, 0.85f, 0.55f);
    }

    private void drawGrid() {
        float step = SIZE / 4.0f;
        for (int i = 1; i < 4; i++) {
            float pos = i * step;
            renderer.drawLine(X + pos, Y + PADDING, X + pos, Y + SIZE - PADDING, 1.0f, 0.25f, 0.32f, 0.38f, 0.35f);
            renderer.drawLine(X + PADDING, Y + pos, X + SIZE - PADDING, Y + pos, 1.0f, 0.25f, 0.32f, 0.38f, 0.35f);
        }
    }

    private void drawRoadSamples(List<Vector2f> roadSamples, float minX, float maxX, float minZ, float maxZ) {
        if (roadSamples == null) return;

        int step = Math.max(1, roadSamples.size() / 320);
        for (int i = 0; i < roadSamples.size(); i += step) {
            Vector2f sample = roadSamples.get(i);
            float x = mapX(sample.x, minX, maxX);
            float y = mapY(sample.y, minZ, maxZ);
            renderer.drawRect(x - 0.8f, y - 0.8f, 1.6f, 1.6f, 0.70f, 0.76f, 0.78f, 0.70f);
        }
    }

    private void drawRoutes(CityMap cityMap, float minX, float maxX, float minZ, float maxZ) {
        if (cityMap == null || cityMap.getTrafficRoutes() == null) return;

        for (List<Vector2f> route : cityMap.getTrafficRoutes()) {
            if (route == null || route.size() < 2) continue;

            for (int i = 0; i < route.size(); i++) {
                Vector2f a = route.get(i);
                Vector2f b = route.get((i + 1) % route.size());
                float ax = mapX(a.x, minX, maxX);
                float ay = mapY(a.y, minZ, maxZ);
                float bx = mapX(b.x, minX, maxX);
                float by = mapY(b.y, minZ, maxZ);
                renderer.drawLine(ax, ay, bx, by, 2.0f, 0.82f, 0.86f, 0.82f, 0.42f);
            }
        }
    }

    private void drawPassengers(List<Passenger> passengers, float minX, float maxX, float minZ, float maxZ) {
        if (passengers == null) return;

        for (Passenger passenger : passengers) {
            if (passenger == null || passenger.isDelivered()) continue;

            if (passenger.isPickedUp()) {
                Vector2f dropoff = passenger.getDropoffLocation();
                drawDiamond(dropoff.x, dropoff.y, minX, maxX, minZ, maxZ, 4.0f, 1.0f, 0.88f, 0.22f, 0.95f);
            } else {
                Vector2f pickup = passenger.getPickupLocation();
                drawDiamond(pickup.x, pickup.y, minX, maxX, minZ, maxZ, 3.4f, 0.20f, 1.0f, 0.40f, 0.9f);
            }
        }
    }

    private void drawTraffic(List<TrafficCar> trafficCars, float minX, float maxX, float minZ, float maxZ) {
        if (trafficCars == null) return;

        for (TrafficCar traffic : trafficCars) {
            if (traffic == null || traffic.getPosition() == null) continue;
            Vector3f pos = traffic.getPosition();
            drawPoint(pos.x, pos.z, minX, maxX, minZ, maxZ, 2.4f, 1.0f, 0.35f, 0.28f, 0.9f);
        }
    }

    private void drawPlayer(
        Vector3f playerPosition,
        Vector3f playerForward,
        float minX,
        float maxX,
        float minZ,
        float maxZ
    ) {
        if (playerPosition == null) return;

        float px = mapX(playerPosition.x, minX, maxX);
        float py = mapY(playerPosition.z, minZ, maxZ);
        renderer.drawDiamond(px, py, 5.2f, 0.24f, 0.72f, 1.0f, 1.0f);

        if (playerForward != null && playerForward.lengthSquared() > 0.0001f) {
            Vector3f forward = new Vector3f(playerForward).normalize();
            float noseX = px + forward.x * 10.0f;
            float noseY = py - forward.z * 10.0f;
            renderer.drawLine(px, py, noseX, noseY, 2.0f, 0.90f, 0.97f, 1.0f, 1.0f);
        }
    }

    private void drawPoint(
        float worldX,
        float worldZ,
        float minX,
        float maxX,
        float minZ,
        float maxZ,
        float radius,
        float r,
        float g,
        float b,
        float a
    ) {
        float x = mapX(worldX, minX, maxX);
        float y = mapY(worldZ, minZ, maxZ);
        renderer.drawRect(x - radius, y - radius, radius * 2.0f, radius * 2.0f, r, g, b, a);
    }

    private void drawDiamond(
        float worldX,
        float worldZ,
        float minX,
        float maxX,
        float minZ,
        float maxZ,
        float radius,
        float r,
        float g,
        float b,
        float a
    ) {
        renderer.drawDiamond(mapX(worldX, minX, maxX), mapY(worldZ, minZ, maxZ), radius, r, g, b, a);
    }

    private float mapX(float worldX, float minX, float maxX) {
        float normalized = (worldX - minX) / (maxX - minX);
        return X + PADDING + clamp(normalized) * (SIZE - PADDING * 2.0f);
    }

    private float mapY(float worldZ, float minZ, float maxZ) {
        float normalized = (worldZ - minZ) / (maxZ - minZ);
        return Y + PADDING + (1.0f - clamp(normalized)) * (SIZE - PADDING * 2.0f);
    }

    private float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
