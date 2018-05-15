#version 420 core

in vec2 uv_frag;

out vec4 fragment;

uniform sampler2D color;

void main()
{
    fragment = texture(color, uv_frag);
}