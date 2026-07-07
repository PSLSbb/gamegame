package game.scene;

import static org.lwjgl.assimp.Assimp.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIColor4D;
import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIMatrix4x4;
import org.lwjgl.assimp.AIMaterial;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AINode;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIString;
import org.lwjgl.assimp.AITexture;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.system.MemoryStack;

import game.engine.Mesh;
import game.engine.Texture;

public class ModelLoader {
    private static final Map<String, Texture> TEXTURE_CACHE = new HashMap<>();

    public static List<Mesh> loadModel(String filePath) {
        List<Mesh> meshes = new ArrayList<>();
        Path resolvedPath;

        try {
            resolvedPath = resolveModelPath(filePath);
        } catch (IOException e) {
            System.err.println("Failed to resolve model: " + filePath + " - " + e.getMessage());
            return meshes;
        }

        AIScene scene = aiImportFile(resolvedPath.toString(),
            aiProcess_Triangulate |
            aiProcess_FlipUVs |
            aiProcess_GenNormals |
            aiProcess_CalcTangentSpace |
            aiProcess_JoinIdenticalVertices |
            aiProcess_OptimizeMeshes |
            aiProcess_SortByPType
        );

        if (scene == null) {
            String error = aiGetErrorString();
            System.err.println("Failed to load model: " + resolvedPath + " - " + error);
            return meshes;
        }

        if (scene.mMeshes() != null && scene.mRootNode() != null) {
            processNode(scene.mRootNode(), scene, new Matrix4f(), meshes, resolvedPath.getParent());
        }

        aiReleaseImport(scene);
        return meshes;
    }

    private static Path resolveModelPath(String filePath) throws IOException {
        Path direct = Path.of(filePath);
        if (Files.isRegularFile(direct)) {
            return direct.toAbsolutePath().normalize();
        }

        Path cwd = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        String fileName = direct.getFileName().toString();
        Path[] candidates = {
            cwd.resolve(filePath),
            cwd.resolve(fileName),
            cwd.resolve("..").resolve(fileName),
            cwd.resolve("..").resolve("..").resolve(fileName)
        };

        for (Path candidate : candidates) {
            Path normalized = candidate.normalize();
            if (Files.isRegularFile(normalized)) {
                return normalized.toAbsolutePath();
            }
        }

        InputStream resource = ModelLoader.class.getClassLoader().getResourceAsStream(filePath);
        if (resource == null) {
            resource = ModelLoader.class.getClassLoader().getResourceAsStream(fileName);
        }
        if (resource == null) {
            resource = ModelLoader.class.getResourceAsStream("/" + filePath);
        }
        if (resource == null) {
            resource = ModelLoader.class.getResourceAsStream("/" + fileName);
        }
        if (resource != null) {
            Path tempFile = Files.createTempFile("city-racer-", "-" + fileName);
            tempFile.toFile().deleteOnExit();
            try (InputStream is = resource) {
                Files.copy(is, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return tempFile.toAbsolutePath();
        }

        throw new IOException("file not found in working directory, nearby project directories, or packaged resources");
    }

    private static void processNode(AINode node, AIScene scene, Matrix4f parentTransform, List<Mesh> meshes, Path modelDirectory) {
        Matrix4f nodeTransform = new Matrix4f(parentTransform).mul(toMatrix(node.mTransformation()));

        IntBuffer nodeMeshes = node.mMeshes();
        PointerBuffer sceneMeshes = scene.mMeshes();
        if (nodeMeshes != null && sceneMeshes != null) {
            for (int i = 0; i < node.mNumMeshes(); i++) {
                int meshIndex = nodeMeshes.get(i);
                AIMesh aiMesh = AIMesh.create(sceneMeshes.get(meshIndex));
                Mesh mesh = processMesh(aiMesh, scene, nodeTransform, modelDirectory);
                if (mesh != null) {
                    meshes.add(mesh);
                }
            }
        }

        PointerBuffer children = node.mChildren();
        if (children != null) {
            for (int i = 0; i < node.mNumChildren(); i++) {
                processNode(AINode.create(children.get(i)), scene, nodeTransform, meshes, modelDirectory);
            }
        }
    }

    private static Matrix4f toMatrix(AIMatrix4x4 m) {
        return new Matrix4f(
            m.a1(), m.b1(), m.c1(), m.d1(),
            m.a2(), m.b2(), m.c2(), m.d2(),
            m.a3(), m.b3(), m.c3(), m.d3(),
            m.a4(), m.b4(), m.c4(), m.d4()
        );
    }

    private static Mesh processMesh(AIMesh aiMesh, AIScene scene, Matrix4f transform, Path modelDirectory) {
        int vertexCount = aiMesh.mNumVertices();
        int faceCount = aiMesh.mNumFaces();

        if (vertexCount == 0 || faceCount == 0) return null;

        boolean hasNormals = aiMesh.mNormals() != null;
        boolean hasTexCoords = aiMesh.mTextureCoords(0) != null;

        // Count total indices
        int indexCount = 0;
        AIFace.Buffer faces = aiMesh.mFaces();
        for (int i = 0; i < faceCount; i++) {
            indexCount += faces.get(i).mNumIndices();
        }

        // Build vertex array
        // Each vertex: pos(3) + normal(3) + tex(2) = 8 floats, or pos(3) + normal(3) = 6
        int stride = hasTexCoords ? 8 : 6;
        float[] vertices = new float[vertexCount * stride];
        int[] indices = new int[indexCount];

        // Extract vertices
        AIVector3D.Buffer aiVertices = aiMesh.mVertices();
        AIVector3D.Buffer aiNormals = aiMesh.mNormals();
        AIVector3D.Buffer aiTexCoords = hasTexCoords ? aiMesh.mTextureCoords(0) : null;
        Matrix4f normalTransform = new Matrix4f(transform).invert().transpose();
        Vector3f transformedPosition = new Vector3f();
        Vector3f transformedNormal = new Vector3f();

        for (int i = 0; i < vertexCount; i++) {
            AIVector3D v = aiVertices.get(i);
            transform.transformPosition(v.x(), v.y(), v.z(), transformedPosition);

            int offset = i * stride;
            vertices[offset] = transformedPosition.x;
            vertices[offset + 1] = transformedPosition.y;
            vertices[offset + 2] = transformedPosition.z;

            if (hasNormals) {
                AIVector3D n = aiNormals.get(i);
                normalTransform.transformDirection(n.x(), n.y(), n.z(), transformedNormal).normalize();
                vertices[offset + 3] = transformedNormal.x;
                vertices[offset + 4] = transformedNormal.y;
                vertices[offset + 5] = transformedNormal.z;
            }

            if (hasTexCoords) {
                AIVector3D t = aiTexCoords.get(i);
                vertices[offset + 6] = t.x();
                vertices[offset + 7] = t.y();
            }
        }

        // Extract indices
        int idx = 0;
        faces = aiMesh.mFaces();
        for (int i = 0; i < faceCount; i++) {
            AIFace face = faces.get(i);
            IntBuffer faceIndices = face.mIndices();
            while (faceIndices.hasRemaining()) {
                indices[idx++] = faceIndices.get();
            }
        }

        Mesh mesh = new Mesh(vertices, indices, hasTexCoords, aiMesh.mName().dataString());
        applyMaterial(mesh, aiMesh, scene, modelDirectory);
        return mesh;
    }

    private static void applyMaterial(Mesh mesh, AIMesh aiMesh, AIScene scene, Path modelDirectory) {
        PointerBuffer materials = scene.mMaterials();
        int materialIndex = aiMesh.mMaterialIndex();
        if (materials == null || materialIndex < 0 || materialIndex >= scene.mNumMaterials()) {
            return;
        }

        AIMaterial material = AIMaterial.create(materials.get(materialIndex));
        MaterialColor color = readMaterialColor(material);
        mesh.setMaterialColor(new Vector3f(color.r, color.g, color.b));
        mesh.setMaterialAlpha(color.a);

        Texture texture = readMaterialTexture(material, scene, modelDirectory, aiTextureType_BASE_COLOR);
        if (texture == null) {
            texture = readMaterialTexture(material, scene, modelDirectory, aiTextureType_DIFFUSE);
        }
        if (texture != null) {
            mesh.setTexture(texture.getId());
        }
    }

    private static MaterialColor readMaterialColor(AIMaterial material) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            AIColor4D color = AIColor4D.malloc(stack);
            if (aiGetMaterialColor(material, AI_MATKEY_BASE_COLOR, aiTextureType_NONE, 0, color) == aiReturn_SUCCESS) {
                return new MaterialColor(color.r(), color.g(), color.b(), color.a());
            }
            if (aiGetMaterialColor(material, AI_MATKEY_COLOR_DIFFUSE, aiTextureType_NONE, 0, color) == aiReturn_SUCCESS) {
                return new MaterialColor(color.r(), color.g(), color.b(), color.a());
            }
        }
        return new MaterialColor(0.8f, 0.8f, 0.8f, 1.0f);
    }

    private static Texture readMaterialTexture(AIMaterial material, AIScene scene, Path modelDirectory, int textureType) {
        if (aiGetMaterialTextureCount(material, textureType) <= 0) {
            return null;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            AIString path = AIString.malloc(stack);
            int result = aiGetMaterialTexture(material, textureType, 0, path, (IntBuffer) null, null, null, null, null, null);
            if (result != aiReturn_SUCCESS) {
                return null;
            }

            String texturePath = path.dataString();
            if (texturePath == null || texturePath.isBlank()) {
                return null;
            }

            Texture embeddedTexture = loadEmbeddedTexture(texturePath, scene);
            if (embeddedTexture != null) {
                return embeddedTexture;
            }

            Path externalPath = modelDirectory != null ? modelDirectory.resolve(texturePath).normalize() : Path.of(texturePath);
            if (Files.isRegularFile(externalPath)) {
                String cacheKey = externalPath.toAbsolutePath().normalize().toString();
                Texture cached = TEXTURE_CACHE.get(cacheKey);
                if (cached != null) return cached;

                try {
                    Texture texture = new Texture(cacheKey);
                    TEXTURE_CACHE.put(cacheKey, texture);
                    return texture;
                } catch (IOException e) {
                    System.err.println("Failed to load texture " + cacheKey + ": " + e.getMessage());
                }
            }
        }

        return null;
    }

    private static Texture loadEmbeddedTexture(String texturePath, AIScene scene) {
        PointerBuffer textures = scene.mTextures();
        if (textures == null || scene.mNumTextures() == 0) {
            return null;
        }

        int textureIndex = parseEmbeddedTextureIndex(texturePath);
        if (textureIndex < 0 || textureIndex >= scene.mNumTextures()) {
            return null;
        }

        String cacheKey = "embedded:" + textureIndex;
        Texture cached = TEXTURE_CACHE.get(cacheKey);
        if (cached != null) return cached;

        AITexture aiTexture = AITexture.create(textures.get(textureIndex));
        try {
            Texture texture;
            if (aiTexture.mHeight() == 0) {
                ByteBuffer data = aiTexture.pcDataCompressed();
                texture = new Texture(data, cacheKey + ":" + aiTexture.achFormatHintString());
            } else {
                texture = new Texture(aiTexture.pcData(), aiTexture.mWidth(), aiTexture.mHeight());
            }
            TEXTURE_CACHE.put(cacheKey, texture);
            return texture;
        } catch (IOException e) {
            System.err.println("Failed to load embedded texture " + texturePath + ": " + e.getMessage());
            return null;
        }
    }

    private static int parseEmbeddedTextureIndex(String texturePath) {
        String path = texturePath.trim();
        if (path.startsWith("*")) {
            path = path.substring(1);
        }
        try {
            return Integer.parseInt(path);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    /**
     * Analyze the loaded meshes to determine scene boundaries
     */
    public static SceneBounds calculateBounds(List<Entity> entities) {
        SceneBounds bounds = new SceneBounds();
        for (Entity entity : entities) {
            if (entity.hasMesh()) {
                // Approximate bounds from the entity's stored mesh data
                // For simplicity, just use entity positions
                bounds.minX = Math.min(bounds.minX, entity.getPosition().x);
                bounds.maxX = Math.max(bounds.maxX, entity.getPosition().x);
                bounds.minZ = Math.min(bounds.minZ, entity.getPosition().z);
                bounds.maxZ = Math.max(bounds.maxZ, entity.getPosition().z);
            }
        }
        return bounds;
    }

    public static class SceneBounds {
        public float minX = Float.MAX_VALUE;
        public float maxX = -Float.MAX_VALUE;
        public float minZ = Float.MAX_VALUE;
        public float maxZ = -Float.MAX_VALUE;
    }

    private static class MaterialColor {
        final float r;
        final float g;
        final float b;
        final float a;

        MaterialColor(float r, float g, float b, float a) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }
    }
}
