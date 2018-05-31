package org.orchid;

import static org.lwjgl.glfw.GLFW.glfwGetTime;

public class Time
{
    private static double lastFrameTime;
    private static double deltaTime;

    /**
     * Time since app was launched
     *
     * @return time in seconds
     */
    public static double getCurrentTime()
    {
        return glfwGetTime();
    }

    /**
     * Delta time of the last drawn frame
     *
     * @return time in seconds
     */
    public static double getDeltaTime()
    {
        return deltaTime;
    }

    /**
     * Current rendering framerate
     *
     * @return frames per second
     */
    public static int getFramerate()
    {
        return (int) (1.0 / deltaTime);
    }

    static void updateDelta()
    {
        double currentTime = glfwGetTime();
        deltaTime = currentTime - lastFrameTime;
        lastFrameTime = currentTime;
    }
}
