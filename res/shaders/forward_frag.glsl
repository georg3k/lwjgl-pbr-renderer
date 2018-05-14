#version 420 core

in VS_OUT
{
    vec3 position;
    vec3 normal;
    vec3 bitangent;
    vec2 uv;
} vs_in;

out vec4 fragment;

void main()
{
    fragment = vec4(1.0);
}