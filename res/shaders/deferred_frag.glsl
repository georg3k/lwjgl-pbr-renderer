#version 420 core

in VS_OUT
{
    vec3 position;
    vec3 normal;
    vec3 bitangent;
    vec2 uv;
} vs_in;

layout (binding = 4) uniform sampler2D albedo_map;
layout (binding = 5) uniform sampler2D metalness_map;
layout (binding = 6) uniform sampler2D roughness_map;
layout (binding = 7) uniform sampler2D normal_map;
layout (binding = 8) uniform sampler2D emission_map;
layout (binding = 9) uniform sampler2D ambient_occlusion_map;

// Order is significant in std140 layout. Don't break!
layout (std140, binding = 2) uniform material_block
{
    vec3  material_albedo;
    bool  material_has_albedo_map;
    bool  material_has_normal_map;
    float material_metalness;
    bool  material_has_metalness_map;
    float material_roughness;
    bool  material_has_roughness_map;
    vec3  material_emission;
    bool  material_has_emission_map;
    bool  material_has_ambient_occlusion_map;
};

layout (location = 0) out vec3 position;
layout (location = 1) out vec3 albedo;
layout (location = 2) out vec3 normal;
layout (location = 3) out float metalness;
layout (location = 4) out float roughness;
layout (location = 5) out vec3 emission;
layout (location = 6) out float ambient_occlusion;

void main()
{
    position = vs_in.position;

    albedo = material_albedo.rgb;
    if(material_has_albedo_map)
        albedo *= texture(albedo_map, vs_in.uv).rgb;

    if(material_has_normal_map)
    {
        mat3 texture_space_matrix = mat3(normalize(cross(vs_in.bitangent, vs_in.normal)), vs_in.bitangent, vs_in.normal);
        normal = texture_space_matrix * normalize(texture(normal_map, vs_in.uv).rgb * 2.0 - 1.0);
    }
    else
        normal = vs_in.normal.xyz;

    metalness = material_metalness;
    if(material_has_metalness_map)
        metalness *= texture(metalness_map, vs_in.uv).r;

    roughness = material_roughness;
    if(material_has_roughness_map)
        roughness *= texture(roughness_map, vs_in.uv).r;

    emission = material_emission;
    if(material_has_emission_map)
        emission *= texture(emission_map, vs_in.uv).rgb;

    ambient_occlusion = 1.0; // should be environment ambient
    if(material_has_ambient_occlusion_map)
        ambient_occlusion *= texture(ambient_occlusion_map, vs_in.uv).r;
}