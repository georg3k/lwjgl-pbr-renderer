#version 420 core

layout (location = 0) in vec2 position;
layout (location = 3) in vec2 uv;

layout (std140, binding = 1) uniform camera_block
{
    mat4 view_matrix;
    mat4 projection_matrix;
};

out vec2 uv_frag;
out vec3 camera_position;

void main()
{
    uv_frag = uv;
    // Simple but ineffective way to extract camera position
    mat4 camera_direction = inverse(view_matrix);
    camera_position = vec3(camera_direction[3][0], camera_direction[3][1], camera_direction[3][2]);

    gl_Position = vec4(position, 0.0, 1.0);
}