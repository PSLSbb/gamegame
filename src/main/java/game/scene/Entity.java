package game.scene;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import game.core.GameObject;
import game.engine.Camera;
import game.engine.Mesh;
import game.engine.Renderer;
import game.engine.Transform;

public class Entity extends GameObject {
    private static int nextId = 1;

    private Mesh mesh;
    private final Transform transform = new Transform();
    private final Vector3f color = new Vector3f(0.8f, 0.8f, 0.8f);
    private boolean visible = true;

    // Movement
    private final Vector3f velocity = new Vector3f();
    private float speed = 0;

    // Entity type
    private String type = "generic";

    public Entity() {
        this(null, "generic");
    }

    public Entity(Mesh mesh) {
        this(mesh, "generic");
    }

    public Entity(Mesh mesh, String type) {
        super("entity-" + nextId++, type != null ? type : "generic");
        this.mesh = mesh;
        this.type = type != null ? type : "generic";
    }

    @Override
    public void update(float dt) {
        if (!isActive() || velocity.lengthSquared() < 0.0001f || speed <= 0.0f) {
            return;
        }
        translate(new Vector3f(velocity).mul(speed * dt));
    }

    @Override
    public void render(Renderer renderer, Camera camera, Vector3f viewPosition) {
        if (visible && renderer != null && camera != null) {
            renderer.renderEntity(this, camera, viewPosition);
        }
    }

    public void setMesh(Mesh mesh) {
        this.mesh = mesh;
    }

    public boolean hasMesh() {
        return mesh != null;
    }

    public Mesh getMesh() {
        return mesh;
    }

    public Matrix4f getModelMatrix() {
        return transform.getModelMatrix();
    }

    public Transform getTransform() {
        return transform;
    }

    public Vector3f getColor() {
        return color;
    }

    public void setColor(float r, float g, float b) {
        color.set(r, g, b);
    }

    public void setColor(Vector3f color) {
        this.color.set(color != null ? color : new Vector3f(0.8f, 0.8f, 0.8f));
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        setActive(visible);
    }

    public Vector3f getVelocity() {
        return velocity;
    }

    public void setVelocity(Vector3f velocity) {
        this.velocity.set(velocity != null ? velocity : new Vector3f());
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = Math.max(0.0f, speed);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type != null ? type : "generic";
        setDisplayName(this.type);
    }

    @Override
    public Vector3f getPosition() {
        return transform.position;
    }

    @Override
    public void setPosition(float x, float y, float z) {
        transform.position.set(x, y, z);
    }
}
