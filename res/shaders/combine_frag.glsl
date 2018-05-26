#version 420 core

#define     PI 3.1415926535897932384626433832795

layout (std140, binding = 1) uniform camera_block
{
    mat4 view_matrix;
    mat4 projection_matrix;
};

in vec2 uv_frag;

layout (binding = 0) uniform sampler2D position;
layout (binding = 1) uniform sampler2D albedo;
layout (binding = 2) uniform sampler2D normal;
layout (binding = 3) uniform sampler2D metalness;
layout (binding = 4) uniform sampler2D roughness;
layout (binding = 5) uniform sampler2D emission;
layout (binding = 6) uniform sampler2D ambient_occlusion;

layout (location = 0) out vec4 fragment;

// Schlick fresnel
vec3 fresnel(float cosTheta, vec3 F0)
{
    return F0 + (1.0 - F0) * pow(1.0 - cosTheta, 5.0);
}

// GGX distribution
float distribution(vec3 N, vec3 H, float rough)
{
    float a = rough * rough;
    float a2 = a * a;
    float NdotH = max(dot(N, H), 0.0);
    float NdotH2 = NdotH*NdotH;
    float nom = a2;
    float denom = (NdotH2 * (a2 - 1.0) + 1.0);
    denom = PI * denom * denom;

    return nom / denom;
}

// GGX Schlick geometry distribution
float geometrySchlick(float NdotV, float rough)
{
    float r =  rough + 1.0;
    float k = (r * r) / 8.0;
    float nom = NdotV;
    float denom = NdotV * (1.0 - k) + k;

    return nom / denom;
}

// Smith geometry distribution
float geometrySmith(vec3 N, vec3 V, vec3 L, float rough)
{
    float NdotV = max(dot(N, V), 0.0);
    float NdotL = max(dot(N, L), 0.0);
    float ggx2 = geometrySchlick(NdotV, rough);
    float ggx1 = geometrySchlick(NdotL, rough);

    return ggx1 * ggx2;
}

void main()
{
    vec3 camera_position = -(view_matrix * vec4(0.0, 0.0, 0.0, 1.0)).xyz;

    vec3 light_positions[4] = { vec3(3.0, 3.0, 0.0), vec3(3.0, -3.0, 0.0), vec3(-3.0, -3.0, 0.0), vec3(-3.0, 3.0, 0.0) };
    vec3 light_color[4] = { vec3(5.0, 5.0, 5.0), vec3(5.0, 5.0, 5.0), vec3(5.0, 5.0, 5.0), vec3(5.0, 5.0, 5.0) };

    vec3 N = normalize(texture(normal, uv_frag).xyz);
    vec3 V = normalize(camera_position - texture(position, uv_frag).xyz);

    vec3 Lo = vec3(0.0);

    for(int i = 0; i < 4; ++i)
    {
        vec3 L = normalize(light_positions[i] - texture(position, uv_frag).xyz);
        vec3 H = normalize(V + L);

        float distance = length(light_positions[i] - texture(position, uv_frag).xyz);
        float attenuation = 1.0 / (distance * distance);
        vec3 radiance = light_color[i] * attenuation;

        vec3 F0 = vec3(0.04);
        F0 = mix(F0, texture(albedo, uv_frag).rgb, texture(metalness, uv_frag).r);
        vec3 F = fresnel(max(dot(H, V), 0.0), F0);
        float NDF = distribution(N, H, texture(roughness, uv_frag).r);
        float G = geometrySmith(N, V, L, texture(roughness, uv_frag).r);

        vec3 nominator = NDF * G * F;
        float denominator = 4 * max(dot(N, V), 0.0) * max(dot(N, L), 0.0) + 0.001;
        vec3 specular = nominator / denominator;

        vec3 kS = F;
        vec3 kD = vec3(1.0) - kS;
        kD *= 1.0 - texture(metalness, uv_frag).r;

        float NdotL = max(dot(N, L), 0.0);
        Lo += (kD * texture(albedo, uv_frag).rgb / PI + specular) * radiance * NdotL;
    }

    // Hardcoded ambient until IBL is not implemented
    vec3 ambient = vec3(0.2) * texture(albedo, uv_frag).rgb * texture(ambient_occlusion, uv_frag).r;

    fragment = vec4(ambient + Lo, 1.0);
}