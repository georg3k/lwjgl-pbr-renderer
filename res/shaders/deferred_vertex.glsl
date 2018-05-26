#version 420 core

layout (location = 0) in vec3 position;
layout (location = 1) in vec3 normal;
layout (location = 2) in vec3 bitangent;
layout (location = 3) in vec2 uv;

layout (std140, binding = 0) uniform model_block
{
    mat4 model_matrix;
};

layout (std140, binding = 1) uniform camera_block
{
    mat4 view_matrix;
    mat4 projection_matrix;
};

out VS_OUT
{
    vec3 position;
    vec3 normal;
    vec3 bitangent;
    vec2 uv;
} vs_out;

void main()
{
    vs_out.position = (model_matrix * vec4(position, 1.0)).xyz;
    vs_out.normal = (model_matrix * vec4(normal, 0.0)).xyz;
    vs_out.bitangent = (model_matrix * vec4(bitangent, 0.0)).xyz;
    vs_out.uv = uv;

    gl_Position = projection_matrix * view_matrix * model_matrix * vec4(position, 1.0);
}