package org.orchid;

import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Main class - loads configuration and scene files and manages game loop
 */
public class Orchid
{
    private static long window;
    private static int windowHeight, windowWidth;

    private static int frameBuffer;
    private static int colorBuffer;
    private static int depthBuffer;

    private static int renderQuadArray;
    private static int verticesBuffer;
    private static int uvsBuffer;

    private static Shader forwardShader;
    private static Shader combineShader;

    private static double deltaTime;

    /**
     * Window properties change callback
     */
    public static void windowCallback()
    {
        glfwSetWindowSize(window, windowWidth, windowHeight);
        glfwSetWindowTitle(window, Configuration.getProperty("window_title"));

        windowWidth = Integer.parseInt(Configuration.getProperty("window_width"));
        windowHeight = Integer.parseInt(Configuration.getProperty("window_height"));

        // Resizing buffers by recreating them
        cleanupFramebuffer();
        genFramebuffer();
    }

    /**
     * Delta time of last rendered frame
     *
     * @return delta time
     */
    public static double getDeltaTime()
    {
        return deltaTime;
    }

    /**
     * Entry point method
     *
     * @param args default argument list (not used)
     */
    public static void main(String[] args)
    {
        Configuration.loadConfiguration("./res/config.xml");

        if (!glfwInit())
            throw new RuntimeException("GLFW initialization failed");

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
        glfwWindowHint(GLFW_OPENGL_CORE_PROFILE, GLFW_TRUE);

        windowWidth = Integer.parseInt(Configuration.getProperty("window_width"));
        windowHeight = Integer.parseInt(Configuration.getProperty("window_height"));

        window = glfwCreateWindow(windowWidth, windowHeight,
                Configuration.getProperty("window_title"), 0, 0);
        if (window == 0)
            throw new RuntimeException("Window creation failed");

        glfwMakeContextCurrent(window);
        GL.createCapabilities();

        // Scene loading invokes some of GL functions so it should be performed after context creation
        Scene.loadScene(Configuration.getProperty("main_scene"));

        // Forward shader loading
        forwardShader = new Shader("./res/shaders/forward_vertex.glsl",
                "./res/shaders/forward_frag.glsl");

        // Combining shader loading
        combineShader = new Shader("./res/shaders/combine_vertex.glsl",
                "./res/shaders/combine_frag.glsl");

        genFramebuffer();
        genRenderquad();

        double lastTime = 0.0;
        double currentTime;

        float rot = 0.0f;

        while (!glfwWindowShouldClose(window))
        {
            currentTime = glfwGetTime();
            deltaTime = currentTime - lastTime;
            lastTime = currentTime;

            Scene.update();

            glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glEnable(GL_DEPTH_TEST);

            forwardShader.use();
            Scene.drawOpaque();
            Scene.drawTransparent();

            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glClear(GL_COLOR_BUFFER_BIT);
            glDisable(GL_DEPTH_TEST);

            combineShader.use();
            glBindVertexArray(renderQuadArray);
            glBindTexture(GL_TEXTURE_2D, colorBuffer);
            glDrawArrays(GL_TRIANGLES, 0, 6);

            glfwPollEvents();
            glfwSwapBuffers(window);
        }

        cleanupFramebuffer();
        cleanupRenderquad();
    }

    private static void genFramebuffer()
    {
        frameBuffer = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);

        colorBuffer = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, colorBuffer);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, windowWidth, windowHeight,
                0, GL_RGB, GL_UNSIGNED_BYTE, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glBindTexture(GL_TEXTURE_2D, 0);

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorBuffer, 0);

        depthBuffer = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, depthBuffer);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, windowWidth, windowHeight);
        glBindRenderbuffer(GL_RENDERBUFFER, 0);

        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, depthBuffer);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            System.err.println("Framebuffer is not ready");

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    private static void cleanupFramebuffer()
    {
        glDeleteFramebuffers(frameBuffer);
        glDeleteTextures(colorBuffer);
        glDeleteRenderbuffers(depthBuffer);
    }

    private static void genRenderquad()
    {
        float[] vertices = {
                -1.0f, 1.0f,
                -1.0f, -1.0f,
                1.0f, -1.0f,
                -1.0f, 1.0f,
                1.0f, -1.0f,
                1.0f, 1.0f,
        };

        float[] uvs = {
                0.0f, 1.0f,
                0.0f, 0.0f,
                1.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 0.0f,
                1.0f, 1.0f,
        };

        renderQuadArray = glGenVertexArrays();
        glBindVertexArray(renderQuadArray);

        verticesBuffer = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, verticesBuffer);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        glEnableVertexAttribArray(Shader.POSITION_LOCATION);
        glVertexAttribPointer(Shader.POSITION_LOCATION, 2, GL_FLOAT, false, 0, 0);

        uvsBuffer = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, uvsBuffer);
        glBufferData(GL_ARRAY_BUFFER, uvs, GL_STATIC_DRAW);

        glEnableVertexAttribArray(Shader.UVS_LOCATION);
        glVertexAttribPointer(Shader.UVS_LOCATION, 2, GL_FLOAT, false, 0, 0);

        glBindVertexArray(0);
    }

    private static void cleanupRenderquad()
    {
        glDeleteVertexArrays(renderQuadArray);
        glDeleteBuffers(verticesBuffer);
        glDeleteBuffers(uvsBuffer);
    }
}