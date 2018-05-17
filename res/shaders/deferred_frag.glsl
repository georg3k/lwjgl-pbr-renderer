#version 420 core

in VS_OUT
{
    vec3 position;
    vec3 normal;
    vec3 bitangent;
    vec2 uv;
} vs_in;

layout (location = 0) out vec4 position;
layout (location = 1) out vec4 albedo_metalness;
layout (location = 2) out vec4 normal_roughness;
layout (location = 3) out vec4 emission;
layout (location = 4) out vec4 ambient_occlusion;

void main()
{
    position = vec4(vs_in.position, 1.0);
    // using debugging values since materials are not supported yet
    albedo_metalness = vec4(1.0, 0.0, 0.0, 1.0);
    normal_roughness = vec4(vs_in.normal.xyz, 1.0);
    emission = vec4(0.0, 1.0, 0.0, 1.0);
    ambient_occlusion = vec4(0.0, 0.0, 1.0, 1.0);
}