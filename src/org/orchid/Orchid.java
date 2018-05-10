package org.orchid;

import org.lwjgl.opengl.GL;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
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
     * Entry point method
     *
     * @param args default argument list (not used)
     */
    public static void main(String[] args)
    {
        // Configuration loading
        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            parser.parse(new File("./res/config.xml"), new DefaultHandler()
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

                    switch (element) {
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

                    if (callback != null) {
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
            e.printStackTrace();
        }

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
}