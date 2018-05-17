#version 420 core

layout (location = 0) in vec2 position;
layout (location = 3) in vec2 uv;

out vec2 uv_frag;

void main()
{
    uv_frag = uv;
    gl_Position = vec4(position, 0.0, 1.0);
}