#version 420 core

in VS_OUT
{
    vec3 position;
    vec3 normal;
    vec3 bitangent;
    vec2 uv;
} vs_in;

// Order is significant in std140 layout. Don't break!
layout (std140, binding = 2) uniform material_block
{
    vec4  material_albedo;
    bool  material_has_albedo_map;
    float material_metalness;
    bool  material_has_metalness_map;
    float material_roughness;
    vec3  material_emission;
    bool  material_has_roughness_map;
    vec3  material_ambient;
    bool  material_has_normal_map;
    bool  material_has_emission_map;
    bool  material_has_ambient_occlusion_map;
};

out vec4 fragment;

void main()
{
    fragment = vec4(1.0);
}