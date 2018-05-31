#version 420 core

#define PI 3.1415926

in VS_OUT
{
    vec3 position;
    vec3 normal;
    vec3 bitangent;
    vec2 uv;
    vec3 camera_position;
} vs_in;

layout (binding = 4) uniform sampler2D albedo_map;
layout (binding = 5) uniform sampler2D metalness_map;
layout (binding = 6) uniform sampler2D roughness_map;
layout (binding = 7) uniform sampler2D normal_map;
layout (binding = 8) uniform sampler2D emission_map;
layout (binding = 9) uniform sampler2D ambient_occlusion_map;

layout (binding = 10) uniform samplerCube radiance;
layout (binding = 11) uniform samplerCube irradiance;
layout (binding = 12) uniform sampler2D BRDFlookUp;

layout (std140, binding = 2) uniform material_block
{
    vec4  material_albedo;
    bool  material_has_albedo_map;
    bool  material_has_normal_map;
    float material_metalness;
    bool  material_has_metalness_map;
    float material_roughness;
    bool  material_has_roughness_map;
    bool  material_has_ambient_occlusion_map;
    bool  material_has_emission_map;
    vec3  material_emission;
};

layout (location = 0) out vec3 position;
layout (location = 1) out vec4 albedo_metalness;
layout (location = 2) out vec4 normal_roughness;
layout (location = 3) out vec3 environment_emission;

float DistributionGGX(vec3 N, vec3 H, float rough)
 {
     float a      = rough*rough;
     float a2     = a*a;
     float NdotH  = max(dot(N, H), 0.0);
     float NdotH2 = NdotH * NdotH;

     float num   = a2;
     float denom = (NdotH2 * (a2 - 1.0) + 1.0);
     denom = PI * denom * denom;

     return num / denom;
 }

 float GeometrySchlickGGX(float NdotV, float rough)
 {
     float r = (rough + 1.0);
     float k = (r*r) / 8.0;

     float num   = NdotV;
     float denom = NdotV * (1.0 - k) + k;

     return num / denom;
 }

 vec3 FresnelSchlickRoughness(float cosTheta, vec3 F0, float rough)
 {
     return F0 + (max(vec3(1.0 - rough), F0) - F0) * pow(1.0 - cosTheta, 5.0);
 }

 float GeometrySmith(vec3 N, vec3 V, vec3 L, float rough)
 {
     float NdotV = max(dot(N, V), 0.0);
     float NdotL = max(dot(N, L), 0.0);
     float ggx2  = GeometrySchlickGGX(NdotV, rough);
     float ggx1  = GeometrySchlickGGX(NdotL, rough);

     return ggx1 * ggx2;
 }

void main()
{
    // "Hard" blending (like tree leaves) still can be used
    if(material_albedo.a == 0 || material_has_albedo_map && texture(albedo_map, vs_in.uv).a == 0)
        discard;

    position = vs_in.position;

    albedo_metalness.rgb = material_albedo.rgb;
    if(material_has_albedo_map)
        albedo_metalness.rgb *= texture(albedo_map, vs_in.uv).rgb;

    if(material_has_normal_map)
    {
        mat3 texture_space_matrix = mat3(normalize(cross(vs_in.bitangent, vs_in.normal)), vs_in.bitangent, vs_in.normal);
        normal_roughness.rgb = texture_space_matrix * normalize(texture(normal_map, vs_in.uv).rgb * 2.0 - 1.0);
    }
    else
        normal_roughness.rgb = vs_in.normal.xyz;

    albedo_metalness.a = material_metalness;
    if(material_has_metalness_map)
        albedo_metalness.a *= texture(metalness_map, vs_in.uv).r;

    normal_roughness.a = material_roughness;
    if(material_has_roughness_map)
        normal_roughness.a *= texture(roughness_map, vs_in.uv).r;

    environment_emission = material_emission;
    if(material_has_emission_map)
        environment_emission *= texture(emission_map, vs_in.uv).rgb;

    vec3 N = normalize(normal_roughness.rgb);
    vec3 V = normalize(vs_in.camera_position - position);

    vec3 F0 = vec3(0.04);
    F0 = mix(F0, albedo_metalness.rgb, albedo_metalness.a);
    vec3 F = FresnelSchlickRoughness(max(dot(N, V), 0.0), F0, normal_roughness.a);
    vec3 kD = 1.0 - F;
    kD *= 1.0 - albedo_metalness.a;
    vec3 diffuse = texture(irradiance, N).rgb * albedo_metalness.rgb;

    vec3 R = reflect(-V, N);

    const float MAX_REFLECTION_LOD = 4.0;
    vec3 prefiltered_radiance = textureCubeLod(radiance, R, normal_roughness.a * MAX_REFLECTION_LOD).rgb;
    vec2 radianceBRDF = texture(BRDFlookUp, vec2(max(dot(N, V), 0.0), normal_roughness.a)).rg;
    vec3 specular = prefiltered_radiance * (F * radianceBRDF.x + radianceBRDF.y);

    vec3 environment = kD * diffuse + specular;

    if(material_has_ambient_occlusion_map)
        environment *= texture(ambient_occlusion_map, vs_in.uv).r;

    environment_emission = environment;
    if(material_has_emission_map)
        environment_emission += texture(emission_map, vs_in.uv).rgb;
}