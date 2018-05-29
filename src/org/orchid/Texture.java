package org.orchid;

import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBImage.stbi_image_free;
import static org.lwjgl.stb.STBImage.stbi_load;

public class Texture
{
    private static Map<String, Integer> loadedTextures = new HashMap<>();
    private static Map<String, Integer> loadedInstances = new HashMap<>();

    private int texture;
    private String path;

    /**
     * Constructor
     *
     * @param path path to texture image file
     */
    public Texture(String path, int channels)
    {
        this.path = path;

        if(loadedTextures.containsKey(path))
        {
            texture = loadedTextures.get(path);
            loadedInstances.put(path, loadedInstances.get(path) + 1);
            return;
        }

        IntBuffer textureWidth = BufferUtils.createIntBuffer(1);
        IntBuffer textureHeight = BufferUtils.createIntBuffer(1);
        IntBuffer textureChannels = BufferUtils.createIntBuffer(1);
        ByteBuffer textureData = null;

        try {
            textureData = stbi_load(path, textureWidth, textureHeight, textureChannels, channels);
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

        int format = 0;
        int internal = 0;
        switch (channels) {
            case 1:
                format = GL_R8;
                internal = GL_RED;
                break;
            case 2:
                format = GL_RG8;
                internal = GL_RG;
                break;
            case 3:
                format = GL_RGB8;
                internal = GL_RGB;
                break;
            case 4:
                format = GL_RGBA8;
                internal = GL_RGBA;
                break;
            default:
                throw new RuntimeException("Wrong texture channel count: " + channels);
        }

        glTexImage2D(GL_TEXTURE_2D, 0, format, textureWidth.get(0), textureHeight.get(0), 0,
                internal, GL_UNSIGNED_BYTE, textureData);
        glGenerateMipmap(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, 0);

        stbi_image_free(textureData);

        loadedTextures.put(path, texture);
        loadedInstances.put(path, 1);
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
        loadedInstances.put(path, loadedInstances.get(path) - 1);
        if (loadedInstances.get(path) != 0)
            return;

        glDeleteTextures(texture);
    }
}
