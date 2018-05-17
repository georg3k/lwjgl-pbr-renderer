#version 420 core

in vec2 uv_frag;

layout (binding = 0) uniform sampler2D position;
layout (binding = 1) uniform sampler2D albedo_metalness;
layout (binding = 2) uniform sampler2D normal_roughness;
layout (binding = 3) uniform sampler2D emission;
layout (binding = 4) uniform sampler2D ambient_occlusion;

layout (location = 0) out vec4 fragment;

void main()
{
    fragment = vec4(texture(albedo_metalness, uv_frag).rgb, 1.0);
}