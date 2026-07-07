package game.engine;

import static org.lwjgl.opengl.GL33.*;
import java.util.Arrays;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

public class Mesh implements AutoCloseable {
    private final int vao;
    private final int vbo;
    private final int ebo;
    private final int indexCount;
    private final int[] indices;
    private final boolean hasTexCoords;
    private final Vector3f minBounds = new Vector3f();
    private final Vector3f maxBounds = new Vector3f();
    private final Vector3f materialColor = new Vector3f(0.8f, 0.8f, 0.8f);
    private final float[] positions;
    private final String name;
    private float medianY = 0.0f;
    private int vertexCount = 0;
    private int textureId = -1;
    private float materialAlpha = 1.0f;

    public Mesh(float[] vertices, int[] indices, boolean hasTexCoords) {
        this(vertices, indices, hasTexCoords, "");
    }

    public Mesh(float[] vertices, int[] indices, boolean hasTexCoords, String name) {
        this.hasTexCoords = hasTexCoords;
        this.indexCount = indices.length;
        this.indices = Arrays.copyOf(indices, indices.length);
        this.name = name != null ? name : "";

        int stride = hasTexCoords ? 8 : 6; // pos(3) + normal(3) + tex(2) or pos(3)+normal(3)
        this.positions = extractPositions(vertices, stride);
        calculateBounds(vertices, stride);

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();

        glBindVertexArray(vao);

        // Vertex buffer
        FloatBuffer vb = MemoryUtil.memAllocFloat(vertices.length);
        vb.put(vertices).flip();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vb, GL_STATIC_DRAW);
        MemoryUtil.memFree(vb);

        // Index buffer
        IntBuffer ib = MemoryUtil.memAllocInt(indices.length);
        ib.put(indices).flip();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_STATIC_DRAW);
        MemoryUtil.memFree(ib);

        // Position attribute
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        // Normal attribute
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        // Texture coordinate attribute
        if (hasTexCoords) {
            glVertexAttribPointer(2, 2, GL_FLOAT, false, stride * Float.BYTES, 6 * Float.BYTES);
            glEnableVertexAttribArray(2);
        }

        glBindVertexArray(0);
    }

    private float[] extractPositions(float[] vertices, int stride) {
        int count = vertices.length / stride;
        float[] result = new float[count * 3];
        for (int i = 0; i < count; i++) {
            int src = i * stride;
            int dst = i * 3;
            result[dst] = vertices[src];
            result[dst + 1] = vertices[src + 1];
            result[dst + 2] = vertices[src + 2];
        }
        return result;
    }

    private void calculateBounds(float[] vertices, int stride) {
        if (vertices.length < 3) {
            minBounds.set(0.0f);
            maxBounds.set(0.0f);
            medianY = 0.0f;
            vertexCount = 0;
            return;
        }

        minBounds.set(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
        maxBounds.set(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
        vertexCount = vertices.length / stride;
        float[] yValues = new float[vertexCount];
        int yIndex = 0;

        for (int i = 0; i + 2 < vertices.length; i += stride) {
            float x = vertices[i];
            float y = vertices[i + 1];
            float z = vertices[i + 2];
            yValues[yIndex++] = y;

            if (x < minBounds.x) minBounds.x = x;
            if (y < minBounds.y) minBounds.y = y;
            if (z < minBounds.z) minBounds.z = z;
            if (x > maxBounds.x) maxBounds.x = x;
            if (y > maxBounds.y) maxBounds.y = y;
            if (z > maxBounds.z) maxBounds.z = z;
        }

        if (yIndex > 0) {
            Arrays.sort(yValues, 0, yIndex);
            medianY = yValues[yIndex / 2];
        }
    }

    public void setTexture(int textureId) {
        this.textureId = textureId;
    }

    public boolean hasTexture() {
        return textureId != -1;
    }

    public int getTextureId() {
        return textureId;
    }

    public boolean hasTexCoords() {
        return hasTexCoords;
    }

    public int getIndexCount() {
        return indexCount;
    }

    public int getIndex(int index) {
        return indices[index];
    }

    public int getVao() {
        return vao;
    }

    public Vector3f getMinBounds() {
        return new Vector3f(minBounds);
    }

    public Vector3f getMaxBounds() {
        return new Vector3f(maxBounds);
    }

    public String getName() {
        return name;
    }

    public Vector3f getMaterialColor() {
        return new Vector3f(materialColor);
    }

    public void setMaterialColor(Vector3f color) {
        if (color != null) {
            materialColor.set(color);
        }
    }

    public float getMaterialAlpha() {
        return materialAlpha;
    }

    public void setMaterialAlpha(float alpha) {
        materialAlpha = Math.max(0.0f, Math.min(1.0f, alpha));
    }

    public float getMedianY() {
        return medianY;
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public int getPositionCount() {
        return positions.length / 3;
    }

    public Vector3f getPosition(int index, Vector3f dest) {
        int offset = index * 3;
        return dest.set(positions[offset], positions[offset + 1], positions[offset + 2]);
    }

    public void render() {
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    @Override
    public void close() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
    }
}
