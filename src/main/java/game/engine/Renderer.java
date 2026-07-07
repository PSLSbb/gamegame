package game.engine;

import static org.lwjgl.opengl.GL33.*;

import java.io.InputStream;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import game.scene.Entity;

public class Renderer implements AutoCloseable {
    private final ShaderProgram shader;
    private final ShaderProgram menuShader;
    private int menuVao = -1;
    private int menuVbo = -1;
    private Font font;
    private int atlasTextureId = -1;
    private static final int SCREEN_WIDTH = 1280;
    private static final int SCREEN_HEIGHT = 720;

    public Renderer() {
        String vertexSrc = loadShaderSource("/shaders/vertex.glsl");
        String fragmentSrc = loadShaderSource("/shaders/fragment.glsl");
        shader = new ShaderProgram(vertexSrc, fragmentSrc);

        String menuVertexSrc = loadShaderSource("/shaders/menu_vertex.glsl");
        String menuFragmentSrc = loadShaderSource("/shaders/menu_fragment.glsl");
        menuShader = new ShaderProgram(menuVertexSrc, menuFragmentSrc);

        // Try to load font from system paths
        try {
            String[] fontPaths = {
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
                "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf",
                "/usr/share/fonts/TTF/DejaVuSans.ttf",
                "/usr/share/fonts/truetype/ubuntu/Ubuntu-R.ttf",
                "/usr/share/fonts/TTF/FreeSans.ttf"
            };
            for (String fp : fontPaths) {
                if (new java.io.File(fp).exists()) {
                    font = new Font(fp, 48);
                    System.out.println("Loaded font: " + fp);
                    break;
                }
            }
            if (font == null) {
                System.out.println("Warning: No system font found. Text will be rendered as blocks.");
            }
        } catch (Exception e) {
            System.out.println("Warning: Failed to load font: " + e.getMessage());
        }
    }

    private String loadShaderSource(String path) {
        try {
            InputStream is = getClass().getResourceAsStream(path);
            if (is == null) throw new RuntimeException("Shader not found: " + path);
            return new String(is.readAllBytes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load shader: " + path, e);
        }
    }

    public void beginFrame() {
        glClearColor(0.05f, 0.05f, 0.15f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        // Enable blending for text and UI rendering
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        // Default to 2D mode. 3D rendering calls begin3D().
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
    }

    public void begin3D() {
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
    }

    public void begin2D() {
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
    }

    public void renderEntity(Entity entity, Camera camera, Vector3f viewPos) {
        shader.bind();

        Matrix4f projection = camera.getProjectionMatrix();
        Matrix4f view = camera.getViewMatrix();

        shader.setMat4("uProjection", projection);
        shader.setMat4("uView", view);
        shader.setVec3("uViewPos", viewPos);

        // Light
        shader.setVec3("uLight.direction", new Vector3f(-0.5f, -1.0f, -0.3f).normalize());
        shader.setVec3("uLight.ambient", new Vector3f(0.3f));
        shader.setVec3("uLight.diffuse", new Vector3f(0.7f));
        shader.setVec3("uLight.specular", new Vector3f(0.5f));

        shader.setMat4("uModel", entity.getModelMatrix());
        shader.setVec3("uColor", resolveEntityColor(entity));
        shader.setFloat("uAlpha", resolveEntityAlpha(entity));

        if (entity.hasMesh() && entity.getMesh().hasTexture()) {
            shader.setBool("uUseTexture", true);
            glBindTexture(GL_TEXTURE_2D, entity.getMesh().getTextureId());
        } else if (entity.hasMesh()) {
            shader.setBool("uUseTexture", false);
        }

        if (entity.hasMesh()) {
            entity.getMesh().render();
        }

        shader.unbind();
    }

    public void renderEntities(List<Entity> entities, Camera camera, Vector3f viewPos) {
        shader.bind();

        Matrix4f projection = camera.getProjectionMatrix();
        Matrix4f view = camera.getViewMatrix();

        shader.setMat4("uProjection", projection);
        shader.setMat4("uView", view);
        shader.setVec3("uViewPos", viewPos);

        // Light
        shader.setVec3("uLight.direction", new Vector3f(-0.5f, -1.0f, -0.3f).normalize());
        shader.setVec3("uLight.ambient", new Vector3f(0.3f));
        shader.setVec3("uLight.diffuse", new Vector3f(0.7f));
        shader.setVec3("uLight.specular", new Vector3f(0.5f));

        for (Entity entity : entities) {
            if (isTransparent(entity)) continue;
            renderBoundEntity(entity);
        }

        glDepthMask(false);
        glDepthFunc(GL_LEQUAL);
        for (Entity entity : entities) {
            if (!isTransparent(entity)) continue;
            renderBoundEntity(entity);
        }
        glDepthFunc(GL_LESS);
        glDepthMask(true);

        shader.unbind();
    }

    private void renderBoundEntity(Entity entity) {
        if (!entity.hasMesh()) return;

        if (isTransparent(entity)) {
            glEnable(GL_BLEND);
        }

        shader.setMat4("uModel", entity.getModelMatrix());
        shader.setVec3("uColor", resolveEntityColor(entity));
        shader.setFloat("uAlpha", resolveEntityAlpha(entity));

        if (entity.hasMesh() && entity.getMesh().hasTexture()) {
            shader.setBool("uUseTexture", true);
            glBindTexture(GL_TEXTURE_2D, entity.getMesh().getTextureId());
        } else {
            shader.setBool("uUseTexture", false);
        }

        entity.getMesh().render();
    }

    private boolean isTransparent(Entity entity) {
        if (!entity.hasMesh()) return false;

        String name = entity.getMesh().getName().toLowerCase();
        return entity.getMesh().getMaterialAlpha() < 0.99f ||
            name.contains("transparent") ||
            name.contains("lightmap");
    }

    private Vector3f resolveEntityColor(Entity entity) {
        if (entity.hasMesh()) {
            return entity.getMesh().getMaterialColor().mul(entity.getColor(), new Vector3f());
        }
        return entity.getColor();
    }

    private float resolveEntityAlpha(Entity entity) {
        if (entity.hasMesh()) {
            return entity.getMesh().getMaterialAlpha();
        }
        return 1.0f;
    }

    // Draw a filled rectangle for the menu with alpha blending
    public void drawRect(float x, float y, float w, float h, float r, float g, float b, float a) {
        if (menuVao == -1) initMenuQuad();

        Matrix4f ortho = new Matrix4f().setOrtho2D(0, SCREEN_WIDTH, SCREEN_HEIGHT, 0);

        float[] verts = {
            x, y, 0, 0,
            x+w, y, 1, 0,
            x+w, y+h, 1, 1,
            x, y+h, 0, 1
        };

        glBindVertexArray(menuVao);
        glBindBuffer(GL_ARRAY_BUFFER, menuVbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, verts);

        menuShader.bind();
        menuShader.setMat4("uProjection", ortho);
        menuShader.setBool("uUseTexture", false);
        menuShader.setVec4("uColor", new Vector4f(r, g, b, a));

        glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
        menuShader.unbind();
        glBindVertexArray(0);
    }

    public void drawLine(float x1, float y1, float x2, float y2, float width, float r, float g, float b, float a) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 0.001f) return;

        float nx = -dy / length;
        float ny = dx / length;
        float halfWidth = width * 0.5f;

        drawQuad(
            x1 + nx * halfWidth, y1 + ny * halfWidth,
            x2 + nx * halfWidth, y2 + ny * halfWidth,
            x2 - nx * halfWidth, y2 - ny * halfWidth,
            x1 - nx * halfWidth, y1 - ny * halfWidth,
            r, g, b, a
        );
    }

    public void drawDiamond(float cx, float cy, float radius, float r, float g, float b, float a) {
        drawQuad(
            cx, cy - radius,
            cx + radius, cy,
            cx, cy + radius,
            cx - radius, cy,
            r, g, b, a
        );
    }

    private void drawQuad(
        float x1, float y1,
        float x2, float y2,
        float x3, float y3,
        float x4, float y4,
        float r, float g, float b, float a
    ) {
        if (menuVao == -1) initMenuQuad();

        Matrix4f ortho = new Matrix4f().setOrtho2D(0, SCREEN_WIDTH, SCREEN_HEIGHT, 0);

        float[] verts = {
            x1, y1, 0, 0,
            x2, y2, 1, 0,
            x3, y3, 1, 1,
            x4, y4, 0, 1
        };

        glBindVertexArray(menuVao);
        glBindBuffer(GL_ARRAY_BUFFER, menuVbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, verts);

        menuShader.bind();
        menuShader.setMat4("uProjection", ortho);
        menuShader.setBool("uUseTexture", false);
        menuShader.setVec4("uColor", new Vector4f(r, g, b, a));

        glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
        menuShader.unbind();
        glBindVertexArray(0);
    }

    /**
     * Draw text using the loaded bitmap font with alpha blending
     */
    public void drawText(float x, float y, float scale, String text, float r, float g, float b, float a) {
        if (font == null || !font.isLoaded() || text == null || text.isEmpty()) {
            // Fallback: draw colored blocks when no font available
            float cw = 6 * scale;
            float ch = 12 * scale;
            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) != ' ') {
                    drawRect(x + i * cw, y, cw, ch, r * 0.3f, g * 0.3f, b * 0.3f, a * 0.5f);
                }
            }
            return;
        }

        if (menuVao == -1) initMenuQuad();

        Matrix4f ortho = new Matrix4f().setOrtho2D(0, SCREEN_WIDTH, SCREEN_HEIGHT, 0);

        menuShader.bind();
        menuShader.setMat4("uProjection", ortho);
        menuShader.setBool("uUseTexture", true);
        menuShader.setVec4("uColor", new Vector4f(r, g, b, a));

        glBindTexture(GL_TEXTURE_2D, font.getTextureId());

        float drawX = x;
        float baselineY = y;
        // scale is a direct multiplier of the 48px font baking size
        // scale=1.0 → text renders at ~48px height, scale=0.5 → ~24px, etc.
        float charScale = scale;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == ' ') {
                Font.CharInfo spaceInfo = font.getCharInfo(' ');
                drawX += (spaceInfo != null ? spaceInfo.advance : 10) * charScale;
                continue;
            }

            Font.CharInfo info = font.getCharInfo(c);
            if (info == null) continue;

            float charX = drawX + info.xOffset * charScale;
            float charY = baselineY + info.yOffset * charScale;
            float charW = info.width * charScale;
            float charH = info.height * charScale;

            // UV coordinates from font atlas
            float u0 = (float) info.x / font.getAtlasWidth();
            float v0 = (float) info.y / font.getAtlasHeight();
            float u1 = (float) (info.x + info.width) / font.getAtlasWidth();
            float v1 = (float) (info.y + info.height) / font.getAtlasHeight();

            float[] verts = {
                charX, charY, u0, v0,
                charX + charW, charY, u1, v0,
                charX + charW, charY + charH, u1, v1,
                charX, charY + charH, u0, v1
            };

            glBindVertexArray(menuVao);
            glBindBuffer(GL_ARRAY_BUFFER, menuVbo);
            glBufferSubData(GL_ARRAY_BUFFER, 0, verts);

            glDrawArrays(GL_TRIANGLE_FAN, 0, 4);

            drawX += info.advance * charScale;
        }

        glBindVertexArray(0);
        menuShader.unbind();
    }

    public float measureTextWidth(String text, float scale) {
        if (text == null || text.isEmpty()) {
            return 0.0f;
        }

        if (font == null || !font.isLoaded()) {
            return text.length() * 6.0f * scale;
        }

        float width = 0.0f;
        for (int i = 0; i < text.length(); i++) {
            Font.CharInfo info = font.getCharInfo(text.charAt(i));
            width += (info != null ? info.advance : 10) * scale;
        }
        return width;
    }

    private void initMenuQuad() {
        menuVao = glGenVertexArrays();
        menuVbo = glGenBuffers();

        float[] defaultVerts = {
            0, 0, 0, 0,
            1, 0, 1, 0,
            1, 1, 1, 1,
            0, 1, 0, 1
        };

        glBindVertexArray(menuVao);
        glBindBuffer(GL_ARRAY_BUFFER, menuVbo);
        glBufferData(GL_ARRAY_BUFFER, defaultVerts, GL_DYNAMIC_DRAW);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
    }

    @Override
    public void close() {
        shader.close();
        menuShader.close();
        if (font != null) font.close();
        if (menuVao != -1) {
            glDeleteVertexArrays(menuVao);
            glDeleteBuffers(menuVbo);
        }
    }
}
