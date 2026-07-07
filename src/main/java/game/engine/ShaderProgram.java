package game.engine;

import static org.lwjgl.opengl.GL33.*;

public class ShaderProgram implements AutoCloseable {
    private final int programId;

    public ShaderProgram(String vertexSource, String fragmentSource) {
        int vertex = compileShader(GL_VERTEX_SHADER, vertexSource);
        int fragment = compileShader(GL_FRAGMENT_SHADER, fragmentSource);

        programId = glCreateProgram();
        glAttachShader(programId, vertex);
        glAttachShader(programId, fragment);
        glLinkProgram(programId);

        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(programId);
            glDeleteShader(vertex);
            glDeleteShader(fragment);
            glDeleteProgram(programId);
            throw new RuntimeException("Shader program linking failed: " + log);
        }

        glDeleteShader(vertex);
        glDeleteShader(fragment);
    }

    private int compileShader(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);

        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            String typeName = type == GL_VERTEX_SHADER ? "vertex" : "fragment";
            throw new RuntimeException(typeName + " shader compilation failed: " + log);
        }
        return shader;
    }

    public void bind() {
        glUseProgram(programId);
    }

    public void unbind() {
        glUseProgram(0);
    }

    public int getUniformLocation(String name) {
        return glGetUniformLocation(programId, name);
    }

    public void setMat4(String name, org.joml.Matrix4f matrix) {
        try (var stack = org.lwjgl.system.MemoryStack.stackPush()) {
            glUniformMatrix4fv(getUniformLocation(name), false, matrix.get(stack.mallocFloat(16)));
        }
    }

    public void setVec3(String name, org.joml.Vector3f vector) {
        glUniform3f(getUniformLocation(name), vector.x, vector.y, vector.z);
    }

    public void setVec4(String name, org.joml.Vector4f vector) {
        glUniform4f(getUniformLocation(name), vector.x, vector.y, vector.z, vector.w);
    }

    public void setFloat(String name, float value) {
        glUniform1f(getUniformLocation(name), value);
    }

    public void setInt(String name, int value) {
        glUniform1i(getUniformLocation(name), value);
    }

    public void setBool(String name, boolean value) {
        glUniform1i(getUniformLocation(name), value ? 1 : 0);
    }

    @Override
    public void close() {
        glDeleteProgram(programId);
    }
}
