package org.orchid;

import org.lwjgl.opengl.GL;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;

/**
 * Main class - loads configuration and scene files and manages game loop
 */
public class Orchid
{
    private static long window;
    private static Map<String, String> properties = new HashMap<>();
    private static Map<String, Method> callbacks = new HashMap<>();

    private static Node sceneTree;
    private static Camera mainCamera;

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
     * Entry point method
     *
     * @param args default argument list (not used)
     */
    public static void main(String[] args)
    {
        loadConfiguration("./res/config.xml");
        loadScene(getProperty("main_scene"));

        if (!glfwInit())
            throw new RuntimeException("GLFW initialization failed");

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);

        window = glfwCreateWindow(Integer.parseInt(getProperty("window_width")),
                Integer.parseInt(getProperty("window_height")), getProperty("window_title"), 0, 0);
        if (window == 0)
            throw new RuntimeException("Window creation failed");

        glfwMakeContextCurrent(window);
        GL.createCapabilities();

        while (!glfwWindowShouldClose(window))
        {
            glClear(GL_COLOR_BUFFER_BIT);

            glfwPollEvents();
            glfwSwapBuffers(window);
        }
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
                    }
                }

                @Override
                public void endElement(String namespace, String lName, String gName)
                {
                    switch (gName)
                    {
                        case "node":
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
                    }
                }
            });
        } catch (Exception e) {
            System.err.println("Scene file loading failed");
            e.printStackTrace();
        }
    }
}