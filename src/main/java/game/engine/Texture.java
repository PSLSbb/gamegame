package game.engine;

import static org.lwjgl.opengl.GL33.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.lwjgl.BufferUtils;
import org.lwjgl.assimp.AITexel;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

public class Texture {
    private final int id;

    public Texture(String path) throws IOException {
        ByteBuffer imageData;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer comp = stack.mallocInt(1);

            // Read file bytes
            byte[] fileBytes = Files.readAllBytes(Paths.get(path));
            ByteBuffer fileBuffer = BufferUtils.createByteBuffer(fileBytes.length);
            fileBuffer.put(fileBytes).flip();

            imageData = STBImage.stbi_load_from_memory(fileBuffer, w, h, comp, 4);
            if (imageData == null) {
                throw new IOException("Failed to load texture: " + path + " - " + STBImage.stbi_failure_reason());
            }

            id = createOpenGLTexture(imageData, w.get(0), h.get(0));
            STBImage.stbi_image_free(imageData);
        }
    }

    public Texture(ByteBuffer encodedImage, String label) throws IOException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer comp = stack.mallocInt(1);

            ByteBuffer imageData = STBImage.stbi_load_from_memory(encodedImage, w, h, comp, 4);
            if (imageData == null) {
                throw new IOException("Failed to load embedded texture: " + label + " - " + STBImage.stbi_failure_reason());
            }

            id = createOpenGLTexture(imageData, w.get(0), h.get(0));
            STBImage.stbi_image_free(imageData);
        }
    }

    public Texture(AITexel.Buffer texels, int width, int height) throws IOException {
        if (texels == null || width <= 0 || height <= 0) {
            throw new IOException("Invalid raw embedded texture");
        }

        ByteBuffer rgba = BufferUtils.createByteBuffer(width * height * 4);
        for (int i = 0; i < width * height; i++) {
            AITexel texel = texels.get(i);
            rgba.put((byte) texel.r());
            rgba.put((byte) texel.g());
            rgba.put((byte) texel.b());
            rgba.put((byte) texel.a());
        }
        rgba.flip();

        id = createOpenGLTexture(rgba, width, height);
    }

    private static int createOpenGLTexture(ByteBuffer imageData, int width, int height) {
        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, imageData);
        glGenerateMipmap(GL_TEXTURE_2D);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        return textureId;
    }

    public int getId() {
        return id;
    }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, id);
    }

    public void close() {
        glDeleteTextures(id);
    }
}
