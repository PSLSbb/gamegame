package game.engine;

import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.stb.STBTruetype.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.system.MemoryStack;

public class Font implements AutoCloseable {
    private static final int FIRST_CHAR = 32;
    private static final int LAST_CHAR = 126;
    private static final int FONT_HEIGHT = 48;
    private static final int ATLAS_WIDTH = 512;

    private final int textureId;
    private final int atlasWidth;
    private final int atlasHeight;
    private final Map<Character, CharInfo> charMap = new HashMap<>();
    private float scaleY;
    private boolean loaded = false;

    public Font(String ttfPath) {
        this(ttfPath, FONT_HEIGHT);
    }

    public Font(String ttfPath, int fontSize) {
        byte[] ttfBytes;
        try {
            ttfBytes = Files.readAllBytes(Paths.get(ttfPath));
        } catch (Exception e) {
            System.err.println("Failed to load font file: " + ttfPath + " - " + e.getMessage());
            textureId = -1;
            atlasWidth = 0;
            atlasHeight = 0;
            loaded = false;
            return;
        }

        ByteBuffer ttfBuffer = BufferUtils.createByteBuffer(ttfBytes.length);
        ttfBuffer.put(ttfBytes).flip();

        // Create STBTTFontinfo from the raw TTF data
        STBTTFontinfo fontInfo = STBTTFontinfo.create();
        if (!stbtt_InitFont(fontInfo, ttfBuffer)) {
            System.err.println("Failed to initialize font: " + ttfPath);
            textureId = -1;
            atlasWidth = 0;
            atlasHeight = 0;
            loaded = false;
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Calculate font scale
            scaleY = stbtt_ScaleForPixelHeight(fontInfo, fontSize);

            // Get font metrics
            IntBuffer ascent = stack.mallocInt(1);
            IntBuffer descent = stack.mallocInt(1);
            IntBuffer lineGap = stack.mallocInt(1);
            stbtt_GetFontVMetrics(fontInfo, ascent, descent, lineGap);

            int ascentVal = ascent.get(0);
            int descentVal = descent.get(0);

            // Calculate total width for all characters
            int totalWidth = 0;
            for (char c = FIRST_CHAR; c <= LAST_CHAR; c++) {
                IntBuffer ax = stack.mallocInt(1);
                IntBuffer lsb = stack.mallocInt(1);
                stbtt_GetCodepointHMetrics(fontInfo, c, ax, lsb);
                int cw = (int)(ax.get(0) * scaleY);
                totalWidth += cw + 4;
            }

            // Use fixed atlas size
            int atlasW = ATLAS_WIDTH;
            int rows = (totalWidth + atlasW - 1) / atlasW;
            int atlasH = Math.max(fontSize + 20, rows * (fontSize + 20));

            ByteBuffer bitmap = BufferUtils.createByteBuffer(atlasW * atlasH);
            // Fill with zeros
            for (int i = 0; i < atlasW * atlasH; i++) {
                bitmap.put(i, (byte)0);
            }

            int currentX = 0;
            int currentY = 2;
            int maxY = 0;

            for (char c = FIRST_CHAR; c <= LAST_CHAR; c++) {
                IntBuffer ax = stack.mallocInt(1);
                IntBuffer lsb = stack.mallocInt(1);
                stbtt_GetCodepointHMetrics(fontInfo, c, ax, lsb);

                int charWidth = (int)(ax.get(0) * scaleY);

                // Wrap to next row if needed
                if (currentX + charWidth + 4 >= atlasW) {
                    currentX = 0;
                    currentY += fontSize + 8;
                }

                // Render glyph to bitmap
                IntBuffer gW = stack.mallocInt(1);
                IntBuffer gH = stack.mallocInt(1);
                IntBuffer gXOff = stack.mallocInt(1);
                IntBuffer gYOff = stack.mallocInt(1);

                ByteBuffer glyphBitmap = stbtt_GetCodepointBitmap(
                    fontInfo, 0, scaleY, c, gW, gH, gXOff, gYOff
                );

                int gw = gW.get(0);
                int gh = gH.get(0);
                int gox = gXOff.get(0);
                int goy = gYOff.get(0);

                // Copy glyph to atlas
                int baseX = Math.max(0, currentX + gox);
                int baseY = Math.max(0, currentY + goy + fontSize / 2);

                for (int y = 0; y < gh && y + baseY < atlasH; y++) {
                    for (int x = 0; x < gw && x + baseX < atlasW; x++) {
                        int srcIdx = y * gw + x;
                        int dstIdx = (baseY + y) * atlasW + (baseX + x);
                        if (srcIdx >= 0 && srcIdx < gw * gh && dstIdx >= 0 && dstIdx < atlasW * atlasH) {
                            byte pixel = glyphBitmap != null && srcIdx < glyphBitmap.capacity() 
                                ? glyphBitmap.get(srcIdx) : 0;
                            bitmap.put(dstIdx, pixel);
                        }
                    }
                }

                // Store character info
                CharInfo info = new CharInfo();
                info.x = baseX;
                info.y = baseY;
                info.width = gw;
                info.height = gh;
                info.advance = charWidth;
                info.xOffset = gox;
                info.yOffset = goy + fontSize / 2;
                charMap.put(c, info);

                currentX += Math.max(charWidth + 4, gw + 4);
                maxY = Math.max(maxY, baseY + gh);

                if (glyphBitmap != null) {
                    stbtt_FreeBitmap(glyphBitmap, 0L);
                }
            }

            atlasWidth = atlasW;
            atlasHeight = Math.min(atlasH, maxY + 10);

            // Create OpenGL texture from bitmap
            textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);

            // Convert single channel (alpha) to RGBA
            ByteBuffer rgba = BufferUtils.createByteBuffer(atlasWidth * atlasHeight * 4);
            for (int i = 0; i < atlasWidth * atlasHeight; i++) {
                byte alpha = i < bitmap.capacity() ? bitmap.get(i) : 0;
                rgba.put((byte)255); // R
                rgba.put((byte)255); // G
                rgba.put((byte)255); // B
                rgba.put(alpha);     // A
            }
            rgba.flip();

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, atlasWidth, atlasHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, rgba);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            loaded = true;
        } finally {
            // Free font info (but not the TTF buffer - LWJGL manages it)
            // Font info object needs to be freed
            fontInfo.free();
        }
    }

    public int getTextureId() {
        return textureId;
    }

    public int getAtlasWidth() { return atlasWidth; }
    public int getAtlasHeight() { return atlasHeight; }
    public boolean isLoaded() { return loaded; }

    public CharInfo getCharInfo(char c) {
        if (c < FIRST_CHAR || c > LAST_CHAR) {
            return charMap.getOrDefault(' ', null);
        }
        return charMap.get(c);
    }

    public float getScaleY() {
        return scaleY;
    }

    public static class CharInfo {
        public int x, y, width, height;
        public int advance;
        public int xOffset, yOffset;
    }

    @Override
    public void close() {
        if (textureId != -1) {
            glDeleteTextures(textureId);
        }
    }
}
