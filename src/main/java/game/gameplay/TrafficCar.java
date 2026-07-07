package game.gameplay;

import java.util.List;

import org.joml.Vector3f;
import org.joml.Vector2f;

import game.core.Updatable;
import game.scene.Entity;

public class TrafficCar implements Updatable {
    private final Entity entity;
    private final List<Entity> entities;
    private List<Vector2f> waypoints;
    private int currentWaypoint = 0;
    private int waypointDirection = 1;
    private float speed;
    private float rotation = 0;
    private float progress = 0;
    private float modelYawOffset = 0;
    private boolean collidable = true;

    public TrafficCar(Entity entity, float speed) {
        this(List.of(entity), speed);
    }

    public TrafficCar(List<Entity> entities, float speed) {
        if (entities == null || entities.isEmpty()) {
            throw new IllegalArgumentException("TrafficCar requires at least one entity");
        }
        this.entities = entities;
        this.entity = entities.get(0);
        this.speed = speed;
    }

    public void setWaypoints(List<Vector2f> waypoints) {
        this.waypoints = waypoints;
        this.currentWaypoint = waypoints != null && waypoints.size() > 1 ? 1 : 0;
        this.waypointDirection = 1;
        if (waypoints != null && !waypoints.isEmpty()) {
            Vector2f start = waypoints.get(0);
            entity.setPosition(start.x, 0, start.y);
            syncPartTransforms();
            updateRotationToWaypoint();
        }
    }

    public void setRoutePosition(Vector2f position, int nextWaypoint) {
        if (waypoints == null || waypoints.isEmpty()) return;

        currentWaypoint = clampWaypoint(nextWaypoint);
        waypointDirection = currentWaypoint == 0 ? 1 : waypointDirection;
        entity.setPosition(position.x, 0, position.y);
        syncPartTransforms();
        updateRotationToWaypoint();
    }

    public void setRouteIndex(int startWaypoint) {
        if (waypoints == null || waypoints.isEmpty()) return;

        int start = Math.floorMod(startWaypoint, waypoints.size());
        int next = start + 1;
        waypointDirection = 1;
        if (next >= waypoints.size()) {
            next = start - 1;
            waypointDirection = -1;
        }
        if (next < 0) {
            next = start;
        }

        Vector2f position = waypoints.get(start);
        currentWaypoint = next;
        entity.setPosition(position.x, entity.getPosition().y, position.y);
        syncPartTransforms();
        updateRotationToWaypoint();
    }

    public void setModelYawOffset(float modelYawOffset) {
        this.modelYawOffset = modelYawOffset;
        entity.getTransform().rotation.identity();
        entity.getTransform().rotation.rotateY(rotation + modelYawOffset);
        syncPartTransforms();
    }

    public Entity getEntity() { return entity; }

    public List<Entity> getEntities() { return entities; }

    public void update(float deltaTime) {
        if (waypoints == null || waypoints.size() < 2 || currentWaypoint >= waypoints.size()) {
            return;
        }

        Vector2f target = waypoints.get(currentWaypoint);
        Vector2f current = new Vector2f(entity.getPosition().x, entity.getPosition().z);

        Vector2f dir = new Vector2f(target).sub(current);
        float dist = dir.length();

        if (dist < 0.5f) {
            advanceWaypoint();
            updateRotationToWaypoint();
            return;
        }

        dir.normalize();
        progress += speed * deltaTime;

        // Move towards waypoint
        float moveDist = speed * deltaTime;
        if (moveDist > dist) moveDist = dist;

        entity.getPosition().x += dir.x * moveDist;
        entity.getPosition().z += dir.y * moveDist;

        // Update rotation to face movement direction
        faceDirection(dir);
    }

    private void advanceWaypoint() {
        currentWaypoint += waypointDirection;
        if (currentWaypoint >= waypoints.size()) {
            waypointDirection = -1;
            currentWaypoint = waypoints.size() - 2;
        } else if (currentWaypoint < 0) {
            waypointDirection = 1;
            currentWaypoint = Math.min(1, waypoints.size() - 1);
        }
    }

    private int clampWaypoint(int waypoint) {
        if (waypoints == null || waypoints.isEmpty()) return 0;
        return Math.max(0, Math.min(waypoints.size() - 1, waypoint));
    }

    private void updateRotationToWaypoint() {
        if (waypoints == null || waypoints.isEmpty()) return;

        Vector2f target = waypoints.get(currentWaypoint);
        Vector2f current = new Vector2f(entity.getPosition().x, entity.getPosition().z);
        Vector2f dir = new Vector2f(target).sub(current);
        if (dir.lengthSquared() < 0.0001f) return;

        faceDirection(dir.normalize());
    }

    private void faceDirection(Vector2f dir) {
        rotation = (float) Math.atan2(-dir.x, -dir.y);
        entity.getTransform().rotation.identity();
        entity.getTransform().rotation.rotateY(rotation + modelYawOffset);
        syncPartTransforms();
    }

    private void syncPartTransforms() {
        for (int i = 1; i < entities.size(); i++) {
            Entity part = entities.get(i);
            part.getTransform().position.set(entity.getTransform().position);
            part.getTransform().rotation.set(entity.getTransform().rotation);
        }
    }

    public Vector3f getPosition() {
        return entity.getPosition();
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getSpeed() {
        return speed;
    }

    public boolean isCollidable() {
        return collidable;
    }

    public void setCollidable(boolean collidable) {
        this.collidable = collidable;
    }
}
