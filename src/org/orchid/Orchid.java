package org.orchid;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AINode;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.Assimp;
import org.lwjgl.opengl.GL;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

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

    private static int frameBuffer;
    private static int colorBuffer;
    private static int depthBuffer;

    private static int renderQuadArray;
    private static int verticesBuffer;
    private static int uvsBuffer;

    private static Map<String, String> properties = new HashMap<>();
    private static Map<String, Method> callbacks = new HashMap<>();

    private static Node sceneTree;
    private static Camera mainCamera;

    private static Shader forwardShader;
    private static Shader combineShader;

    private static double deltaTime;

    /**
     * Property getter
     *
     * @param name name of the property
     * @return property value
     */
    public static String getProperty(String name)
    {
        return properties.get(name);
    }

    /**
     * Property setter - sets or updates existent property and invokes callback if it is defined
     *
     * @param name  name of the property
     * @param value value of the property
     */
    public static void setProperty(String name, String value)
    {
        properties.put(name, value);

        try {
            if (callbacks.get(name) != null)
                callbacks.get(name).invoke(null);
        } catch (Exception e) {
            System.err.println("Property \"" + name + "\" callback invoke failed");
        }
    }

    /**
     * Window properties change callback
     */
    public static void windowCallback()
    {
        glfwSetWindowSize(window, Integer.parseInt(getProperty("window_width")),
                Integer.parseInt(getProperty("window_height")));
        glfwSetWindowTitle(window, getProperty("window_title"));
    }

    /**
     * Sets camera to render from
     *
     * @param camera camera to set as main
     */
    public static void setMainCamera(Camera camera)
    {
        mainCamera = camera;
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
        loadConfiguration("./res/config.xml");

        if (!glfwInit())
            throw new RuntimeException("GLFW initialization failed");

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
        glfwWindowHint(GLFW_OPENGL_CORE_PROFILE, GLFW_TRUE);

        window = glfwCreateWindow(Integer.parseInt(getProperty("window_width")),
                Integer.parseInt(getProperty("window_height")), getProperty("window_title"), 0, 0);
        if (window == 0)
            throw new RuntimeException("Window creation failed");

        glfwMakeContextCurrent(window);
        GL.createCapabilities();

        // Scene loading invokes some of GL functions so it should be performed after context creation
        loadScene(getProperty("main_scene"));

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

            glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glEnable(GL_DEPTH_TEST);

            forwardShader.use();
            mainCamera.bindBuffer();
            sceneTree.update();

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

    private static void loadConfiguration(String path)
    {
        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            parser.parse(path, new DefaultHandler()
            {
                private String element, name, value, callback;

                @Override
                public void startElement(String namespace, String lName, String gName, Attributes attr)
                {
                    element = gName;

                    if (element.equals("property"))
                        name = value = callback = null;
                }

                @Override
                public void characters(char[] characters, int start, int length)
                {
                    // Ignoring whitespace padding
                    if (new String(characters, start, length).trim().length() == 0)
                        return;

                    switch (element)
                    {
                        case "name":
                            name = new String(characters, start, length);
                            break;
                        case "value":
                            value = new String(characters, start, length);
                            break;
                        case "callback":
                            callback = new String(characters, start, length);
                            break;
                    }
                }

                @Override
                public void endElement(String namespace, String lName, String gName)
                {
                    if (gName.equals("property") || name == null || value == null)
                        return;

                    properties.put(name, value);

                    if (callback != null)
                    {
                        try {
                            Method method = Orchid.class.getMethod(callback);
                            callbacks.put(name, method);
                        } catch (NoSuchMethodException e) {
                            System.err.println("Callback \"" + callback + "\"is not found");
                        }
                    }
                }
            });
        } catch (Exception e) {
            System.err.println("Configuration file loading failed");
            e.printStackTrace();
        }
    }

    private static void loadScene(String path)
    {
        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            parser.parse(path, new DefaultHandler()
            {
                String element;
                String chars;
                Node node = null;
                float[] values = new float[4];

                @Override
                public void startElement(String namespace, String lName, String gName, Attributes attr)
                {
                    element = gName;
                    switch (gName)
                    {
                        case "node":
                            String name = attr.getValue("name");
                            if(name == null)
                                throw new RuntimeException("Node name is not defined in scene file");

                            node = new Node(name, node);
                            if (sceneTree == null)
                                sceneTree = node;
                            break;
                        case "camera":
                            name = attr.getValue("name");
                            if (name == null)
                                throw new RuntimeException("Node name is not defined in scene file");

                            node = new Camera(name, node);

                            if (mainCamera == null)
                                mainCamera = (Camera) node;
                            break;
                    }
                }

                @Override
                public void characters(char[] characters, int start, int length)
                {
                    // Ignoring whitespace padding
                    if (new String(characters, start, length).trim().length() == 0)
                        return;

                    switch (element)
                    {
                        case "x":
                            values[0] = Float.parseFloat(new String(characters, start, length));
                            break;
                        case "y":
                            values[1] = Float.parseFloat(new String(characters, start, length));
                            break;
                        case "z":
                            values[2] = Float.parseFloat(new String(characters, start, length));
                            break;
                        case "mesh":
                            chars = new String(characters, start, length);
                            break;
                        case "near":
                            ((Camera) node).setNear(Float.parseFloat(new String(characters, start, length)));
                            break;
                        case "far":
                            ((Camera) node).setFar(Float.parseFloat(new String(characters, start, length)));
                            break;
                        case "fov":
                            ((Camera) node).setFOV(Float.parseFloat(new String(characters, start, length)));
                            break;
                    }
                }

                @Override
                public void endElement(String namespace, String lName, String gName)
                {
                    switch (gName)
                    {
                        case "node":
                        case "camera":
                            node = node.getParent();
                            break;
                        case "position":
                            node.setPosition(values[0], values[1], values[2]);
                            break;
                        case "rotation":
                            node.setRotation(values[0], values[1], values[2]);
                            break;
                        case "scale":
                            node.setScale(values[0], values[1], values[2]);
                            break;
                        case "mesh":
                            AIScene scene = Assimp.aiImportFile(chars,
                                    Assimp.aiProcess_Triangulate
                                            | Assimp.aiProcess_FlipUVs
                                            | Assimp.aiProcess_CalcTangentSpace);
                            node.addChild(loadModel(scene.mRootNode(), scene));
                            break;
                    }
                }

                private Node loadModel(AINode aiNode, AIScene aiScene)
                {
                    Node newNode = new Node(aiNode.mName().dataString());

                    Matrix4f matrix = new Matrix4f(
                            aiNode.mTransformation().a1(), aiNode.mTransformation().b1(), aiNode.mTransformation().c1(), aiNode.mTransformation().d1(),
                            aiNode.mTransformation().a2(), aiNode.mTransformation().b2(), aiNode.mTransformation().c2(), aiNode.mTransformation().d2(),
                            aiNode.mTransformation().a3(), aiNode.mTransformation().b3(), aiNode.mTransformation().c3(), aiNode.mTransformation().d3(),
                            aiNode.mTransformation().a4(), aiNode.mTransformation().b4(), aiNode.mTransformation().c4(), aiNode.mTransformation().d4()
                    );

                    Vector3f mediator = new Vector3f();

                    matrix.getTranslation(mediator);
                    newNode.setPosition(mediator);

                    matrix.getEulerAnglesZYX(mediator);
                    newNode.setRotation(mediator);

                    matrix.getScale(mediator);
                    newNode.setScale(mediator);

                    for (int i = 0; i < aiNode.mNumMeshes(); i++) {
                        AIMesh aiMesh = AIMesh.create(aiScene.mMeshes().get(aiNode.mMeshes().get(i)));
                        Mesh mesh = new Mesh(aiMesh.mName().dataString(), newNode);
                        mesh.loadMesh(aiMesh);
                    }

                    for (int i = 0; i < aiNode.mNumChildren(); i++)
                        newNode.addChild(loadModel(AINode.create(aiNode.mChildren().get(i)), aiScene));

                    return newNode;
                }
            });
        } catch (Exception e) {
            System.err.println("Scene file loading failed");
            e.printStackTrace();
        }
    }

    private static void genFramebuffer()
    {
        frameBuffer = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);

        colorBuffer = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, colorBuffer);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, Integer.parseInt(getProperty("window_width")),
                Integer.parseInt(getProperty("window_height")), 0, GL_RGB, GL_UNSIGNED_BYTE, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glBindTexture(GL_TEXTURE_2D, 0);

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorBuffer, 0);

        depthBuffer = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, depthBuffer);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, Integer.parseInt(getProperty("window_width")),
                Integer.parseInt(getProperty("window_height")));
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