package game.engine;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Transform {
    public final Vector3f position = new Vector3f();
    public final Quaternionf rotation = new Quaternionf();
    public final Vector3f scale = new Vector3f(1.0f);
    public final Vector3f originOffset = new Vector3f();

    public Matrix4f getModelMatrix() {
        return new Matrix4f()
            .translate(position)
            .rotate(rotation)
            .scale(scale)
            .translate(originOffset);
    }

    public void lookAt(Vector3f target, Vector3f up) {
        Vector3f dir = new Vector3f(target).sub(position).normalize();
        Vector3f right = new Vector3f(up).cross(dir).normalize();
        Vector3f newUp = new Vector3f(dir).cross(right).normalize();
        rotation.setFromNormalized(new Matrix4f(
            right.x, newUp.x, dir.x, 0,
            right.y, newUp.y, dir.y, 0,
            right.z, newUp.z, dir.z, 0,
            0, 0, 0, 1
        ));
    }
}
