package org.orchid;

import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.GL_RGB8_SNORM;

/**
 * Main class - loads configuration and scene files and manages game loop
 */
public class Orchid
{
    private static long window;
    private static int windowHeight, windowWidth;

    // Skybox data
    private static int skyboxCubeArray;
    private static int skyboxVerticesBuffer;

    // Output render quad data
    private static int renderQuadArray;
    private static int verticesBuffer;
    private static int uvsBuffer;

    // Deferred pass data
    private static int deferredframeBuffer;
    private static int deferredPositionBuffer;
    private static int deferredAlbedoBuffer;
    private static int deferredNormalBuffer;
    private static int deferredMetalnessBuffer;
    private static int deferredRoughnessBuffer;
    private static int deferredEmissionBuffer;
    private static int deferredAmbientOcclusionBuffer;

    // Forward/Postprocessing pass data
    private static int frameBuffer;
    private static int colorBuffer;

    // Depth buffer is used both by forward and deferred pass
    private static int sharedDepthbuffer;

    private static Shader deferredShader;
    private static Shader forwardShader;
    private static Shader combineShader;
    private static Shader postprocessingShader;
    private static Shader skyboxShader;

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
        cleanupDepthbuffer();
        genDepthbuffer();
        cleanupDeferredFramebuffer();
        genDeferredFramebuffer();
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

        glDepthFunc(GL_LEQUAL);

        // Scene loading invokes some of GL functions so it should be performed after context creation
        Scene.loadScene(Configuration.getProperty("main_scene"));

        // Deferred shader loading
        deferredShader = new Shader("./res/shaders/deferred_vertex.glsl",
                "./res/shaders/deferred_frag.glsl");

        // Forward shader loading
        forwardShader = new Shader("./res/shaders/forward_vertex.glsl",
                "./res/shaders/forward_frag.glsl");

        // Combining shader loading
        combineShader = new Shader("./res/shaders/combine_vertex.glsl",
                "./res/shaders/combine_frag.glsl");

        // Postprocessing shader loading
        postprocessingShader = new Shader("./res/shaders/postprocessing_vertex.glsl",
                "./res/shaders/postprocessing_frag.glsl");

        // Skybox shader loading
        skyboxShader = new Shader("./res/shaders/skybox_vertex.glsl",
                "./res/shaders/skybox_frag.glsl");


        genDepthbuffer();
        genDeferredFramebuffer();
        genFramebuffer();
        genRenderquad();
        genSkybox();

        double lastTime = 0.0;
        double currentTime;

        float rot = 0.0f;

        while (!glfwWindowShouldClose(window))
        {
            currentTime = glfwGetTime();
            deltaTime = currentTime - lastTime;
            lastTime = currentTime;

            Scene.update();

            deferredPass();
            if(Scene.getSkybox() != null)
                skyboxPass();
            forwardPass();
            postprocessingPass();

            glfwPollEvents();
            glfwSwapBuffers(window);
        }

        cleanupFramebuffer();
        cleanupRenderquad();
        cleanupSkybox();
        Scene.sceneCleanup();
    }

    private static void skyboxPass()
    {
        glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);
        glEnable(GL_DEPTH_TEST);

        skyboxShader.use();
        glActiveTexture(GL_TEXTURE10);
        Scene.getSkybox().use();
        drawSkybox();

    }

    private static void deferredPass()
    {
        glBindFramebuffer(GL_FRAMEBUFFER, deferredframeBuffer);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        deferredShader.use();
        glEnable(GL_DEPTH_TEST);
        Scene.drawOpaque();

        glDisable(GL_DEPTH_TEST);
        glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        glClear(GL_COLOR_BUFFER_BIT);

        combineShader.use();

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, deferredPositionBuffer);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, deferredAlbedoBuffer);
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, deferredNormalBuffer);
        glActiveTexture(GL_TEXTURE3);
        glBindTexture(GL_TEXTURE_2D, deferredMetalnessBuffer);
        glActiveTexture(GL_TEXTURE4);
        glBindTexture(GL_TEXTURE_2D, deferredRoughnessBuffer);
        glActiveTexture(GL_TEXTURE5);
        glBindTexture(GL_TEXTURE_2D, deferredEmissionBuffer);
        glActiveTexture(GL_TEXTURE6);
        glBindTexture(GL_TEXTURE_2D, deferredAmbientOcclusionBuffer);
        glActiveTexture(GL_TEXTURE7);
        Scene.getSkybox().use();
        glActiveTexture(GL_TEXTURE8);
        Scene.getSkyboxIrradiance().use();

        drawRenderquad();
    }

    private static void forwardPass()
    {
        glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);

        forwardShader.use();
        glEnable(GL_DEPTH_TEST);
        Scene.drawTransparent();
    }

    private static void postprocessingPass()
    {
        glDisable(GL_DEPTH_TEST);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glClear(GL_COLOR_BUFFER_BIT);

        postprocessingShader.use();

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, colorBuffer);

        drawRenderquad();
    }

    private static void genDepthbuffer()
    {
        sharedDepthbuffer = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, sharedDepthbuffer);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, windowWidth, windowHeight);
        glBindRenderbuffer(GL_RENDERBUFFER, 0);
    }

    private static void cleanupDepthbuffer()
    {
        glDeleteRenderbuffers(sharedDepthbuffer);
    }

    private static void genDeferredFramebuffer()
    {
        deferredframeBuffer = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, deferredframeBuffer);

        deferredPositionBuffer = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, deferredPositionBuffer);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB16F, windowWidth, windowHeight,
                0, GL_RGB, GL_FLOAT, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glBindTexture(GL_TEXTURE_2D, 0);

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, deferredPositionBuffer, 0);

        deferredAlbedoBuffer = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, deferredAlbedoBuffer);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, windowWidth, windowHeight,
                0, GL_RGBA, GL_FLOAT, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glBindTexture(GL_TEXTURE_2D, 0);

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, GL_TEXTURE_2D, deferredAlbedoBuffer, 0);

        deferredNormalBuffer = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, deferredNormalBuffer);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB8_SNORM, windowWidth, windowHeight,
                0, GL_RGB, GL_SHORT, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glBindTexture(GL_TEXTURE_2D, 0);

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT2, GL_TEXTURE_2D, deferredNormalBuffer, 0);

        deferredMetalnessBuffer = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, deferredMetalnessBuffer);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, windowWidth, windowHeight,
                0, GL_RED, GL_UNSIGNED_BYTE, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glBindTexture(GL_TEXTURE_2D, 0);

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT3, GL_TEXTURE_2D, deferredMetalnessBuffer, 0);

        deferredRoughnessBuffer = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, deferredRoughnessBuffer);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, windowWidth, windowHeight,
                0, GL_RED, GL_UNSIGNED_BYTE, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glBindTexture(GL_TEXTURE_2D, 0);

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT4, GL_TEXTURE_2D, deferredRoughnessBuffer, 0);

        deferredEmissionBuffer = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, deferredEmissionBuffer);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB16F, windowWidth, windowHeight,
                0, GL_RGB, GL_FLOAT, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glBindTexture(GL_TEXTURE_2D, 0);

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT5, GL_TEXTURE_2D, deferredEmissionBuffer, 0);

        deferredAmbientOcclusionBuffer = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, deferredAmbientOcclusionBuffer);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, windowWidth, windowHeight,
                0, GL_RED, GL_UNSIGNED_BYTE, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glBindTexture(GL_TEXTURE_2D, 0);

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT6, GL_TEXTURE_2D, deferredAmbientOcclusionBuffer, 0);

        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, sharedDepthbuffer);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            System.err.println("Deferred framebuffer is not ready: " + glCheckFramebufferStatus(GL_FRAMEBUFFER));


        int attachments[] = {GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1, GL_COLOR_ATTACHMENT2,
                GL_COLOR_ATTACHMENT3, GL_COLOR_ATTACHMENT4, GL_COLOR_ATTACHMENT5, GL_COLOR_ATTACHMENT6};

        glDrawBuffers(attachments);

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    private static void cleanupDeferredFramebuffer()
    {
        glDeleteFramebuffers(deferredframeBuffer);
        glDeleteTextures(deferredPositionBuffer);
        glDeleteTextures(deferredAlbedoBuffer);
        glDeleteTextures(deferredNormalBuffer);
        glDeleteTextures(deferredRoughnessBuffer);
        glDeleteTextures(deferredEmissionBuffer);
        glDeleteTextures(deferredAmbientOcclusionBuffer);
    }

    private static void genFramebuffer()
    {
        frameBuffer = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);

        colorBuffer = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, colorBuffer);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, windowWidth, windowHeight,
                0, GL_RGBA, GL_FLOAT, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glBindTexture(GL_TEXTURE_2D, 0);

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorBuffer, 0);

        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, sharedDepthbuffer);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            System.err.println("Forward framebuffer is not ready");

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    private static void cleanupFramebuffer()
    {
        glDeleteFramebuffers(frameBuffer);
        glDeleteTextures(colorBuffer);
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

    private static void drawRenderquad()
    {
        glBindVertexArray(renderQuadArray);
        glDrawArrays(GL_TRIANGLES, 0, 6);

    }

    private static void cleanupRenderquad()
    {
        glDeleteVertexArrays(renderQuadArray);
        glDeleteBuffers(verticesBuffer);
        glDeleteBuffers(uvsBuffer);
    }

    private static void genSkybox()
    {
        float skyboxVertices[] = {
                -1.0f, 1.0f, -1.0f,
                -1.0f, -1.0f, -1.0f,
                1.0f, -1.0f, -1.0f,
                1.0f, -1.0f, -1.0f,
                1.0f, 1.0f, -1.0f,
                -1.0f, 1.0f, -1.0f,

                -1.0f, -1.0f, 1.0f,
                -1.0f, -1.0f, -1.0f,
                -1.0f, 1.0f, -1.0f,
                -1.0f, 1.0f, -1.0f,
                -1.0f, 1.0f, 1.0f,
                -1.0f, -1.0f, 1.0f,

                1.0f, -1.0f, -1.0f,
                1.0f, -1.0f, 1.0f,
                1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, -1.0f,
                1.0f, -1.0f, -1.0f,

                -1.0f, -1.0f, 1.0f,
                -1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, 1.0f,
                1.0f, -1.0f, 1.0f,
                -1.0f, -1.0f, 1.0f,

                -1.0f, 1.0f, -1.0f,
                1.0f, 1.0f, -1.0f,
                1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, 1.0f,
                -1.0f, 1.0f, 1.0f,
                -1.0f, 1.0f, -1.0f,

                -1.0f, -1.0f, -1.0f,
                -1.0f, -1.0f, 1.0f,
                1.0f, -1.0f, -1.0f,
                1.0f, -1.0f, -1.0f,
                -1.0f, -1.0f, 1.0f,
                1.0f, -1.0f, 1.0f
        };

        skyboxCubeArray = glGenVertexArrays();
        glBindVertexArray(skyboxCubeArray);

        skyboxVerticesBuffer = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, skyboxVerticesBuffer);
        glBufferData(GL_ARRAY_BUFFER, skyboxVertices, GL_STATIC_DRAW);

        glEnableVertexAttribArray(Shader.POSITION_LOCATION);
        glVertexAttribPointer(Shader.POSITION_LOCATION, 3, GL_FLOAT, false, 0, 0);

        glBindVertexArray(0);
    }

    private static void drawSkybox()
    {
        glBindVertexArray(skyboxCubeArray);
        glDrawArrays(GL_TRIANGLES, 0, 36);

    }

    private static void cleanupSkybox()
    {
        glDeleteVertexArrays(skyboxCubeArray);
        glDeleteBuffers(verticesBuffer);
    }
}