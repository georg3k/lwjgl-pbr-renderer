package org.orchid;

import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBImage.*;

public class Texture
{
    private static Map<String, Integer> loadedTextures = new HashMap<>();

    int texture;

    /**
     * Constructor
     *
     * @param path path to texture image file
     */
    public Texture(String path)
    {
        if(loadedTextures.containsKey(path))
        {
            texture = loadedTextures.get(path);
            return;
        }

        IntBuffer textureWidth = BufferUtils.createIntBuffer(1);
        IntBuffer textureHeight = BufferUtils.createIntBuffer(1);
        IntBuffer textureChannels = BufferUtils.createIntBuffer(1);
        ByteBuffer textureData = null;

        try {
            textureData = stbi_load(path, textureWidth, textureHeight, textureChannels, 4);
        } catch (Exception e) {
            System.err.println("Texture \"" + path + "\" loading failed");
            e.printStackTrace();
        }

        texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR_MIPMAP_LINEAR);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, textureWidth.get(0), textureHeight.get(0), 0,
                GL_RGBA, GL_UNSIGNED_BYTE, textureData);
        glGenerateMipmap(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, 0);

        loadedTextures.put(path, texture);
    }

    /**
     * Bind this texture to currently active texture slot
     */
    public void use()
    {
        glBindTexture(GL_TEXTURE_2D, texture);
    }

    /**
     * Deletes texture from GRAM
     */
    public void remove()
    {
        glDeleteTextures(texture);
    }
}
