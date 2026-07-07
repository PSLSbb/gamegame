package game.gameplay;

import org.joml.Vector3f;

import game.core.Updatable;
import game.engine.Camera;
import game.scene.Entity;
import static org.lwjgl.glfw.GLFW.*;

public class PlayerController implements Updatable {
    private final Entity car;
    private final Camera camera;

    // Physics
    private float acceleration = 12.0f;
    private float maxSpeed = 30.0f;
    private float braking = 8.0f;
    private float turnSpeed = 2.5f;
    private float friction = 4.0f;
    private float currentSpeed = 0;
    private float currentRotation = 0; // Y-axis rotation in radians
    private float modelYawOffset = 0;

    // Input state
    private boolean forward = false;
    private boolean backward = false;
    private boolean left = false;
    private boolean right = false;

    // Camera
    private float cameraSmooth = 3.0f;
    private Vector3f smoothForward = new Vector3f(0, 0, -1);

    public PlayerController(Entity car, Camera camera) {
        this.car = car;
        this.camera = camera;
    }

    public void handleKey(int key, int action) {
        boolean pressed = action == GLFW_PRESS || action == GLFW_REPEAT;
        switch (key) {
            case GLFW_KEY_W: case GLFW_KEY_UP: forward = pressed; break;
            case GLFW_KEY_S: case GLFW_KEY_DOWN: backward = pressed; break;
            case GLFW_KEY_A: case GLFW_KEY_LEFT: left = pressed; break;
            case GLFW_KEY_D: case GLFW_KEY_RIGHT: right = pressed; break;
        }
    }

    public void update(float deltaTime) {
        // Calculate forward direction based on rotation
        float sinR = (float) Math.sin(currentRotation);
        float cosR = (float) Math.cos(currentRotation);
        Vector3f forwardDir = new Vector3f(-sinR, 0, -cosR);

        // Turning
        if (left) {
            currentRotation += turnSpeed * deltaTime;
        }
        if (right) {
            currentRotation -= turnSpeed * deltaTime;
        }

        // Acceleration/Braking
        if (forward) {
            currentSpeed += acceleration * deltaTime;
            if (currentSpeed > maxSpeed) currentSpeed = maxSpeed;
        } else if (backward) {
            currentSpeed -= braking * deltaTime;
            if (currentSpeed < -maxSpeed * 0.4f) currentSpeed = -maxSpeed * 0.4f;
        } else {
            // Friction
            if (Math.abs(currentSpeed) < 0.1f) {
                currentSpeed = 0;
            } else {
                float frictionForce = friction * deltaTime;
                if (Math.abs(currentSpeed) < frictionForce) {
                    currentSpeed = 0;
                } else {
                    currentSpeed -= Math.signum(currentSpeed) * frictionForce;
                }
            }
        }

        // Apply movement
        car.getTransform().position.x += forwardDir.x * currentSpeed * deltaTime;
        car.getTransform().position.z += forwardDir.z * currentSpeed * deltaTime;

        // Update car rotation
        car.getTransform().rotation.identity();
        car.getTransform().rotation.rotateY(currentRotation + modelYawOffset);

        // Update camera with smoothing
        Vector3f targetForward = forwardDir;
        smoothForward.lerp(targetForward, Math.min(1.0f, cameraSmooth * deltaTime));
        smoothForward.normalize();
        camera.updateThirdPerson(car.getPosition(), smoothForward);
    }

    public Entity getCar() { return car; }
    public float getSpeed() { return currentSpeed; }
    public float getRotation() { return currentRotation; }

    public Vector3f getForwardDirection() {
        float sinR = (float) Math.sin(currentRotation);
        float cosR = (float) Math.cos(currentRotation);
        return new Vector3f(-sinR, 0, -cosR);
    }

    public void setModelYawOffset(float modelYawOffset) {
        this.modelYawOffset = modelYawOffset;
    }

    public void resetPosition(float x, float z) {
        car.setPosition(x, 0, z);
        currentSpeed = 0;
        currentRotation = 0;
        car.getTransform().rotation.identity();
        car.getTransform().rotation.rotateY(modelYawOffset);
    }

}
