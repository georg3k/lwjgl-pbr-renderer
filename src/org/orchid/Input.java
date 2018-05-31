package org.orchid;

import static org.lwjgl.glfw.GLFW.*;

public class Input
{
    /**
     * Keycodes
     */
    public static final int KEY_SPACE = 32;
    public static final int KEY_APOSTROPHE = 39;
    public static final int KEY_COMMA = 44;
    public static final int KEY_MINUS = 45;
    public static final int KEY_PERIOD = 46;
    public static final int KEY_SLASH = 47;
    public static final int KEY_0 = 48;
    public static final int KEY_1 = 49;
    public static final int KEY_2 = 50;
    public static final int KEY_3 = 51;
    public static final int KEY_4 = 52;
    public static final int KEY_5 = 53;
    public static final int KEY_6 = 54;
    public static final int KEY_7 = 55;
    public static final int KEY_8 = 56;
    public static final int KEY_9 = 57;
    public static final int KEY_SEMICOLON = 59;
    public static final int KEY_EQUAL = 61;
    public static final int KEY_A = 65;
    public static final int KEY_B = 66;
    public static final int KEY_C = 67;
    public static final int KEY_D = 68;
    public static final int KEY_E = 69;
    public static final int KEY_F = 70;
    public static final int KEY_G = 71;
    public static final int KEY_H = 72;
    public static final int KEY_I = 73;
    public static final int KEY_J = 74;
    public static final int KEY_K = 75;
    public static final int KEY_L = 76;
    public static final int KEY_M = 77;
    public static final int KEY_N = 78;
    public static final int KEY_O = 79;
    public static final int KEY_P = 80;
    public static final int KEY_Q = 81;
    public static final int KEY_R = 82;
    public static final int KEY_S = 83;
    public static final int KEY_T = 84;
    public static final int KEY_U = 85;
    public static final int KEY_V = 86;
    public static final int KEY_W = 87;
    public static final int KEY_X = 88;
    public static final int KEY_Y = 89;
    public static final int KEY_Z = 90;
    public static final int KEY_LEFT_BRACKET = 91;
    public static final int KEY_BACKSLASH = 92;
    public static final int KEY_RIGHT_BRACKET = 93;
    public static final int KEY_GRAVE_ACCENT = 96;
    public static final int KEY_WORLD_1 = 161;
    public static final int KEY_WORLD_2 = 162;
    public static final int KEY_ESCAPE = 256;
    public static final int KEY_ENTER = 257;
    public static final int KEY_TAB = 258;
    public static final int KEY_BACKSPACE = 259;
    public static final int KEY_INSERT = 260;
    public static final int KEY_DELETE = 261;
    public static final int KEY_RIGHT = 262;
    public static final int KEY_LEFT = 263;
    public static final int KEY_DOWN = 264;
    public static final int KEY_UP = 265;
    public static final int KEY_PAGE_UP = 266;
    public static final int KEY_PAGE_DOWN = 267;
    public static final int KEY_HOME = 268;
    public static final int KEY_END = 269;
    public static final int KEY_CAPS_LOCK = 280;
    public static final int KEY_SCROLL_LOCK = 281;
    public static final int KEY_NUM_LOCK = 282;
    public static final int KEY_PRINT_SCREEN = 283;
    public static final int KEY_PAUSE = 284;
    public static final int KEY_F1 = 290;
    public static final int KEY_F2 = 291;
    public static final int KEY_F3 = 292;
    public static final int KEY_F4 = 293;
    public static final int KEY_F5 = 294;
    public static final int KEY_F6 = 295;
    public static final int KEY_F7 = 296;
    public static final int KEY_F8 = 297;
    public static final int KEY_F9 = 298;
    public static final int KEY_F10 = 299;
    public static final int KEY_F11 = 300;
    public static final int KEY_F12 = 301;
    public static final int MOUSE_BUTTON_1 = 0;
    public static final int MOUSE_BUTTON_2 = 1;
    public static final int MOUSE_BUTTON_3 = 2;
    public static final int MOUSE_BUTTON_4 = 3;
    public static final int MOUSE_BUTTON_5 = 4;

    private static long window;

    private static boolean[] keys = new boolean[301];
    private static boolean[] keysDown = new boolean[301];
    private static boolean[] keysUp = new boolean[301];


    private static boolean[] mouseButtons = new boolean[5];
    private static boolean[] mouseButtonsDown = new boolean[5];
    private static boolean[] mouseButtonsUp = new boolean[5];

    private static double mousePosX, mousePosY;

    /**
     * Checks if key is pressed
     *
     * @param keycode keycode to check
     * @return keycode status
     */
    public static boolean getKey(int keycode)
    {
        return keys[keycode];
    }

    /**
     * Checks if key is down
     *
     * @param keycode keycode to check
     * @return keycode status
     */
    public static boolean getKeyDown(int keycode)
    {
        return keysDown[keycode];
    }

    /**
     * Checks if key is up
     *
     * @param keycode keycode to check
     * @return keycode status
     */
    public static boolean getKeyUp(int keycode)
    {
        return keysUp[keycode];
    }

    /**
     * Checks if mouse button is pressed
     *
     * @param button button to check
     * @return button status
     */
    public static boolean getMouseButton(int button)
    {
        return mouseButtons[button];
    }

    /**
     * Checks if mouse button is down
     *
     * @param button button to check
     * @return button status
     */
    public static boolean getMouseButtonDown(int button)
    {
        return mouseButtonsDown[button];
    }

    /**
     * Checks if mouse button is up
     *
     * @param button button to check
     * @return button status
     */
    public static boolean getMouseButtonUp(int button)
    {
        return mouseButtonsUp[button];
    }

    /**
     * Mouse cursor position X
     *
     * @return mouse x coordinate (in pixels)
     */
    public static int getMousePosX()
    {
        return (int) mousePosX;
    }

    /**
     * Mouse cursor position Y
     *
     * @return mouse y coordinate (in pixels)
     */
    public static int getMousePosY()
    {
        return (int) mousePosY;
    }

    static void init(long window)
    {
        Input.window = window;

        glfwSetCursorPosCallback(window, (window1, xpos, ypos) -> {
            mousePosX = xpos;
            mousePosY = ypos;
        });
    }

    static void update()
    {
        for (int i = 0; i < 301; i++) {
            boolean currentKey = glfwGetKey(window, i) == GLFW_PRESS;

            keysDown[i] = currentKey && !keys[i];
            keysUp[i] = !currentKey && keys[i];
            keys[i] = currentKey;
        }


        for (int i = 0; i < 5; i++) {
            boolean currentButton = glfwGetMouseButton(window, i) == GLFW_PRESS;

            mouseButtonsDown[i] = currentButton && !mouseButtons[i];
            mouseButtonsUp[i] = !currentButton && mouseButtons[i];
            mouseButtons[i] = currentButton;
        }
    }
}
