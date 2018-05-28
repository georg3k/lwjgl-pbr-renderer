#version 420 core

#define     PI 3.1415926

in vec2 uv_frag;
in vec3 camera_position;

layout (binding = 0) uniform sampler2D position;
layout (binding = 1) uniform sampler2D albedo;
layout (binding = 2) uniform sampler2D normal;
layout (binding = 3) uniform sampler2D metalness;
layout (binding = 4) uniform sampler2D roughness;
layout (binding = 5) uniform sampler2D emission;
layout (binding = 6) uniform sampler2D ambient_occlusion;

layout (binding = 7) uniform samplerCube radiance;
layout (binding = 8) uniform samplerCube irradiance;
layout (binding = 9) uniform sampler2D BRDFlookUp;

layout (location = 0) out vec4 fragment;

// Schlick fresnel
vec3 fresnelSchlick(float cosTheta, vec3 F0)
{
    return F0 + (1.0 - F0) * pow(1.0 - cosTheta, 5.0);
}

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
    // Real scene lights are not implemented yet so I am using these "built-it" for testing
    vec3 light_positions[] = { vec3( -5, -5, -5), vec3( 5, -5, -5), vec3( 5, 5, -5), vec3( -5, 5, -5),
                               vec3( -5, -5,  5), vec3( 5, -5,  5), vec3( 5, 5, -5), vec3( -5, 5,  5)};
    vec3 light_color = vec3(50.0, 50.0, 50.0);

    float metalness_value = texture(metalness, uv_frag).r;
    float roughness_value = 1.0;//texture(roughness, uv_frag).r;
    float ambient_occlusion_value = texture(ambient_occlusion, uv_frag).r;
    vec3 position_value = texture(position, uv_frag).xyz;
    vec3 albedo_value = texture(albedo, uv_frag).rgb;
    vec3 emission_value = texture(emission, uv_frag).rgb;

    vec3 N = normalize(texture(normal, uv_frag).xyz);
    vec3 V = normalize(camera_position - position_value);

    vec3 F0 = vec3(0.04);
    F0 = mix(F0, albedo_value, metalness_value);
    vec3 F = FresnelSchlickRoughness(max(dot(N, V), 0.0), F0, roughness_value);
    vec3 kS = F;
    vec3 kD = 1.0 - kS;
    kD *= 1.0 - metalness_value;
    vec3 diffuse = texture(irradiance, N).rgb * albedo_value;

    vec3 R = reflect(-V, N);

    const float MAX_REFLECTION_LOD = 4.0;
    vec3 prefiltered_radiance = textureCubeLod(radiance, R, roughness_value * MAX_REFLECTION_LOD).rgb;
    vec2 radianceBRDF = texture(BRDFlookUp, vec2(max(dot(N, V), 0.0), roughness_value)).rg;
    vec3 radianceSpecular = prefiltered_radiance * (F * radianceBRDF.x + radianceBRDF.y);
    vec3 ambient = (kD * diffuse + radianceSpecular) * ambient_occlusion_value;

    vec3 Lo = vec3(0.0);

    for(int i = 0; i < 8; i++)
    {
        vec3 L = normalize(light_positions[i] - position_value);
        vec3 H = normalize(V + L);

        float distance = length(light_positions[i] - position_value);
        float attenuation = 1.0 / (distance * distance);
        vec3 radiance = light_color * attenuation;

        vec3 F = fresnelSchlick(max(dot(H, V), 0.0), F0);
        float NDF = DistributionGGX(N, H, roughness_value);
        float G = GeometrySmith(N, V, L, roughness_value);

        vec3 nominator = NDF * G * F;
        float denominator = 4 * max(dot(N, V), 0.0) * max(dot(N, L), 0.0) + 0.001;
        vec3 specular = nominator / denominator;

        vec3 kS = F;
        vec3 kD = vec3(1.0) - kS;
        kD *= 1.0 - metalness_value;

        float NdotL = max(dot(N, L), 0.0);
        Lo += (kD * albedo_value / PI + specular) * radiance * NdotL;
    }

    fragment = vec4(Lo + ambient, 1.0);
}