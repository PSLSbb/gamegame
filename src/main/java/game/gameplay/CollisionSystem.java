package game.gameplay;

import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import game.scene.Entity;

public class CollisionSystem {
    private float carCollisionRadius = 2.0f;
    private float buildingCollisionRadius = 3.0f;
    private static final float CAR_VERTICAL_COLLISION_RANGE = 2.4f;
    private static final float WALL_NORMAL_Y_LIMIT = 0.45f;
    private static final float WALL_MIN_HEIGHT = 1.0f;

    public boolean checkCarCollision(Vector3f pos1, Vector3f pos2) {
        if (Math.abs(pos1.y - pos2.y) > CAR_VERTICAL_COLLISION_RANGE) {
            return false;
        }

        float dx = pos1.x - pos2.x;
        float dz = pos1.z - pos2.z;
        float dist = (float) Math.sqrt(dx * dx + dz * dz);
        return dist < carCollisionRadius * 2;
    }

    public boolean checkBuildingCollision(Vector3f carPos, List<Entity> cityEntities) {
        for (Entity e : cityEntities) {
            if (!e.getType().equals("city")) continue;
            float dx = carPos.x - e.getPosition().x;
            float dz = carPos.z - e.getPosition().z;
            float dist = (float) Math.sqrt(dx * dx + dz * dz);
            if (dist < buildingCollisionRadius) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if player car is within bounds of the city
     */
    public boolean checkCityBounds(Vector3f pos, float minX, float maxX, float minZ, float maxZ) {
        return pos.x >= minX && pos.x <= maxX && pos.z >= minZ && pos.z <= maxZ;
    }

    /**
     * Push car back if it's out of bounds
     */
    public void enforceBounds(Entity car, float minX, float maxX, float minZ, float maxZ) {
        Vector3f pos = car.getPosition();
        pos.x = Math.max(minX, Math.min(maxX, pos.x));
        pos.z = Math.max(minZ, Math.min(maxZ, pos.z));
    }

    public void resolveCityCollisions(Entity car, List<Entity> cityEntities, float carRadius) {
        if (car == null || cityEntities == null) return;

        Vector3f pos = car.getPosition();
        for (Entity entity : cityEntities) {
            if (entity == null || !entity.hasMesh() || !isSolidCityMesh(entity)) continue;

            Bounds bounds = getWorldBounds(entity);
            if (!bounds.isValid()) continue;

            if (pos.x < bounds.min.x - carRadius || pos.x > bounds.max.x + carRadius ||
                pos.z < bounds.min.z - carRadius || pos.z > bounds.max.z + carRadius ||
                bounds.max.y < pos.y + 0.15f || bounds.min.y > pos.y + 4.0f) {
                continue;
            }

            resolveMeshWallCollision(pos, entity, carRadius);
        }
    }

    private void resolveMeshWallCollision(Vector3f carPosition, Entity entity, float carRadius) {
        Matrix4f model = entity.getModelMatrix();
        Vector3f a = new Vector3f();
        Vector3f b = new Vector3f();
        Vector3f c = new Vector3f();
        Vector3f normal = new Vector3f();

        for (int i = 0; i + 2 < entity.getMesh().getIndexCount(); i += 3) {
            entity.getMesh().getPosition(entity.getMesh().getIndex(i), a);
            entity.getMesh().getPosition(entity.getMesh().getIndex(i + 1), b);
            entity.getMesh().getPosition(entity.getMesh().getIndex(i + 2), c);
            model.transformPosition(a);
            model.transformPosition(b);
            model.transformPosition(c);

            float minX = Math.min(a.x, Math.min(b.x, c.x));
            float maxX = Math.max(a.x, Math.max(b.x, c.x));
            float minY = Math.min(a.y, Math.min(b.y, c.y));
            float maxY = Math.max(a.y, Math.max(b.y, c.y));
            float minZ = Math.min(a.z, Math.min(b.z, c.z));
            float maxZ = Math.max(a.z, Math.max(b.z, c.z));
            if (carPosition.x < minX - carRadius || carPosition.x > maxX + carRadius ||
                carPosition.z < minZ - carRadius || carPosition.z > maxZ + carRadius ||
                maxY < carPosition.y + 0.2f || minY > carPosition.y + 2.4f) {
                continue;
            }

            normal.set(b).sub(a).cross(new Vector3f(c).sub(a));
            if (normal.lengthSquared() < 0.0001f) continue;
            normal.normalize();

            if (Math.abs(normal.y) > WALL_NORMAL_Y_LIMIT) continue;

            if (maxY - minY < WALL_MIN_HEIGHT) continue;

            resolveWallEdge(carPosition, a, b, carRadius);
            resolveWallEdge(carPosition, b, c, carRadius);
            resolveWallEdge(carPosition, c, a, carRadius);
        }
    }

    private void resolveWallEdge(Vector3f carPosition, Vector3f a, Vector3f b, float carRadius) {
        Vector3f edge = new Vector3f(b).sub(a);
        if (edge.lengthSquared() < 0.0001f) return;

        // Use only edges that have real horizontal span. Pure vertical edges are corners and are handled by adjacent edges.
        float horizontalLengthSquared = edge.x * edge.x + edge.z * edge.z;
        if (horizontalLengthSquared < 0.0001f) return;

        Vector3f closest = closestPointOnSegment2D(carPosition, a, b);
        float dx = carPosition.x - closest.x;
        float dz = carPosition.z - closest.z;
        float distSq = dx * dx + dz * dz;
        if (distSq >= carRadius * carRadius) return;

        float dist = (float) Math.sqrt(Math.max(distSq, 0.000001f));
        float push = carRadius - dist;
        carPosition.x += (dx / dist) * push;
        carPosition.z += (dz / dist) * push;
    }

    private Vector3f closestPointOnSegment2D(Vector3f point, Vector3f a, Vector3f b) {
        float abx = b.x - a.x;
        float abz = b.z - a.z;
        float lengthSq = abx * abx + abz * abz;
        if (lengthSq < 0.0001f) {
            return new Vector3f(a);
        }

        float t = ((point.x - a.x) * abx + (point.z - a.z) * abz) / lengthSq;
        t = clamp(t, 0.0f, 1.0f);
        return new Vector3f(a.x + abx * t, point.y, a.z + abz * t);
    }

    private boolean isSolidCityMesh(Entity entity) {
        if (!"city".equals(entity.getType())) return false;

        String name = entity.getMesh().getName().toLowerCase();
        return name.contains("building");
    }

    private Bounds getWorldBounds(Entity entity) {
        Vector3f min = entity.getMesh().getMinBounds();
        Vector3f max = entity.getMesh().getMaxBounds();
        Matrix4f model = entity.getModelMatrix();

        Bounds bounds = new Bounds();
        for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = 0; z <= 1; z++) {
                    Vector3f corner = new Vector3f(
                        x == 0 ? min.x : max.x,
                        y == 0 ? min.y : max.y,
                        z == 0 ? min.z : max.z
                    );
                    model.transformPosition(corner);
                    bounds.include(corner);
                }
            }
        }
        return bounds;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static class Bounds {
        final Vector3f min = new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
        final Vector3f max = new Vector3f(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);

        void include(Vector3f point) {
            min.min(point);
            max.max(point);
        }

        boolean isValid() {
            return min.x != Float.MAX_VALUE && max.x != -Float.MAX_VALUE;
        }
    }
}
