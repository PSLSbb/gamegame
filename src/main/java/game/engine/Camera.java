package game.engine;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {
    private final Matrix4f view = new Matrix4f();
    private final Matrix4f projection = new Matrix4f();

    private final Vector3f position = new Vector3f();
    private final Vector3f target = new Vector3f();
    private final Vector3f up = new Vector3f(0, 1, 0);

    private float fov = (float) Math.toRadians(60);
    private float aspect = 16.0f / 9.0f;
    private float nearPlane = 0.1f;
    private float farPlane = 2000.0f;

    // Third-person camera parameters
    private float distance = 8.0f;
    private float height = 4.0f;
    private float lookHeight = 1.5f;

    public void setAspect(float aspect) {
        this.aspect = aspect;
    }

    public void updateThirdPerson(Vector3f carPosition, Vector3f carForward) {
        // Camera sits behind and above the car
        Vector3f camOffset = new Vector3f(carForward).mul(-distance);
        camOffset.y = height;

        position.set(carPosition).add(camOffset);

        // Look at a point slightly ahead and above the car
        target.set(carPosition);
        target.y += lookHeight;

        view.setLookAt(position, target, up);
    }

    public Matrix4f getViewMatrix() {
        return view;
    }

    public Matrix4f getProjectionMatrix() {
        projection.setPerspective(fov, aspect, nearPlane, farPlane);
        return projection;
    }

    public Vector3f getPosition() {
        return position;
    }
}
