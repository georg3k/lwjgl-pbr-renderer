#version 420 core

in vec2 uv_frag;

layout (binding = 0) uniform sampler2D color;

layout (location = 0) out vec4 fragment;

void main()
{
    // Tweak gamma correction value as you wish
    float gamma = 2.2;
    // Exposure is something else rather then real exposure
    float exposure = 0.8;

    fragment = vec4(pow(texture(color, uv_frag).rgb * exposure, vec3(1.0 / gamma)), 1.0);
}