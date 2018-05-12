package org.orchid;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.lwjgl.opengl.GL20.*;

/**
 * Shader class - represents GLSL shader program
 */
public class Shader
{
    private int program;

    /**
     * Constructor - builds shader from vertex and fragment shader files
     *
     * @param vertexPath   path to vertex shader file
     * @param fragmentPath path to fragment shader file
     */
    public Shader(String vertexPath, String fragmentPath)
    {
        String vertexSource = "";
        String fragmentSource = "";

        try {
            vertexSource = new String(Files.readAllBytes(Paths.get(vertexPath)));
            fragmentSource = new String(Files.readAllBytes(Paths.get(fragmentPath)));
        } catch (IOException e) {
            System.out.println("Shader reading failed");
            e.printStackTrace();
        }

        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, vertexSource);
        glCompileShader(vertexShader);
        if (glGetShaderInfoLog(vertexShader).equals(""))
            System.err.println(glGetShaderInfoLog(vertexShader));

        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, fragmentSource);
        glCompileShader(fragmentShader);
        if (glGetShaderInfoLog(fragmentShader).equals(""))
            System.err.println(glGetShaderInfoLog(fragmentShader));

        program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);

        glLinkProgram(program);

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
    }

    /**
     * Binds this shader as current in OpenGL context
     */
    public void use()
    {
        glUseProgram(program);
    }
}
