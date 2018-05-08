package org.orchid;

import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class Orchid
{
    private static long window;

    public static void main(String[] args)
    {
        if(!glfwInit())
            throw new RuntimeException("GLFW initialization failed");

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);

        window = glfwCreateWindow(800, 480, "Orchid", 0, 0);
        if(window == 0)
            throw new RuntimeException("Window creation failed");

        glfwMakeContextCurrent(window);
        GL.createCapabilities();

        while(!glfwWindowShouldClose(window))
        {
            glClear(GL_COLOR_BUFFER_BIT);

            glfwPollEvents();
            glfwSwapBuffers(window);
        }
    }
}