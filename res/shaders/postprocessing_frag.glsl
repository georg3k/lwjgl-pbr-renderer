#version 420 core

in vec2 uv_frag;

layout (binding = 0) uniform sampler2D color;

layout (location = 0) out vec4 fragment;

void main()
{
    fragment = texture(color, uv_frag);
}