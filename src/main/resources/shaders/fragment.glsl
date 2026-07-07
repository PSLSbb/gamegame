#version 330 core
in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;

out vec4 FragColor;

uniform vec3 uViewPos;
uniform vec3 uColor;
uniform float uAlpha;
uniform bool uUseTexture;
uniform sampler2D uTexture;

struct Light {
    vec3 direction;
    vec3 ambient;
    vec3 diffuse;
    vec3 specular;
};

uniform Light uLight;

void main() {
    vec3 norm = normalize(Normal);
    vec3 lightDir = normalize(-uLight.direction);

    // Ambient
    vec3 ambient = uLight.ambient * uColor;

    // Diffuse
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = uLight.diffuse * diff * uColor;

    // Specular
    vec3 viewDir = normalize(uViewPos - FragPos);
    vec3 reflectDir = reflect(-lightDir, norm);
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), 32.0);
    vec3 specular = uLight.specular * spec * vec3(0.5);

    vec3 result = ambient + diffuse + specular;

    if (uUseTexture) {
        vec4 texColor = texture(uTexture, TexCoords);
        FragColor = vec4(result * texColor.rgb, texColor.a * uAlpha);
    } else {
        FragColor = vec4(result, uAlpha);
    }
}
