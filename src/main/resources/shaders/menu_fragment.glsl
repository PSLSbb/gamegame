#version 330 core
in vec2 TexCoords;

out vec4 FragColor;

uniform vec4 uColor;
uniform bool uUseTexture;
uniform sampler2D uTexture;

void main() {
    if (uUseTexture) {
        // Multiply texture (white with alpha) by uColor for tinted text
        vec4 texColor = texture(uTexture, TexCoords);
        FragColor = vec4(uColor.rgb, texColor.a * uColor.a);
    } else {
        FragColor = uColor;
    }
}
