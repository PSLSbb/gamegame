package game.core;

import org.joml.Vector3f;

/**
 * INHERITANCE PILLAR:
 * GameObject is the abstract parent for shared entity data and behavior.
 *
 * It implements Updatable and Renderable, but it does not provide final
 * gameplay behavior. Subclasses must override update() and render().
 *
 * ENCAPSULATION PILLAR:
 * Every field is private. Other files must use getters/setters instead of
 * reaching directly into this object's state.
 */
public abstract class GameObject implements Updatable, Renderable {
    private String id;
    private String displayName;
    private boolean active;

    protected GameObject(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
        this.active = true;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public abstract Vector3f getPosition();

    public abstract void setPosition(float x, float y, float z);

    public void setPosition(Vector3f position) {
        if (position != null) {
            setPosition(position.x, position.y, position.z);
        }
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    protected float distanceTo(GameObject other) {
        return other == null ? Float.MAX_VALUE : getPosition().distance(other.getPosition());
    }

    protected float distanceTo(Vector3f target) {
        return target == null ? Float.MAX_VALUE : getPosition().distance(target);
    }

    protected Vector3f directionTo(Vector3f target) {
        if (target == null) {
            return new Vector3f();
        }
        Vector3f direction = new Vector3f(target).sub(getPosition());
        if (direction.lengthSquared() < 0.0001f) {
            return new Vector3f();
        }
        return direction.normalize();
    }

    protected void translate(Vector3f movement) {
        if (movement != null) {
            getPosition().add(movement);
        }
    }
}
