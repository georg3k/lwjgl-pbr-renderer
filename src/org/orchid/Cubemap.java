package org.orchid;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_WRAP_R;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X;
import static org.lwjgl.opengl.GL30.GL_RGB16F;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.stbi_image_free;
import static org.lwjgl.stb.STBImage.stbi_loadf;

public class Cubemap
{
    private static final String[] facePostifixes = {"_posx", "_negx", "_posy", "_negy", "_posz", "_negz"};

    private int texture;

    /**
     * Cubemap constructor
     *
     * @param path template path to cubemap facelist texture (without face indication)
     * @param extension file extension (with dot)
     * @param customMips whether or not to load custom mipmaps ("*_mip_1.hdr" ... "*_mip_4.hdr")
     */
    public Cubemap(String path, String extension, boolean customMips)
    {
        texture = glGenTextures();
        glBindTexture(GL_TEXTURE_CUBE_MAP, texture);

        IntBuffer width = BufferUtils.createIntBuffer(1);
        IntBuffer height = BufferUtils.createIntBuffer(1);
        IntBuffer channels = BufferUtils.createIntBuffer(1);
        FloatBuffer textureData = null;

        for (int i = 0; i < 6; i++) {
            width.clear();
            height.clear();
            channels.clear();

            textureData = stbi_loadf(path + facePostifixes[i] + extension,
                    width, height, channels, 3);
            if (textureData == null)
                System.err.println("Cubemap " + path + facePostifixes[i] + extension + " is not found");
            else {
                glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i,
                        0, GL_RGB16F, width.get(0), height.get(0), 0, GL_RGB, GL_FLOAT, textureData);
                stbi_image_free(textureData);
            }
        }

        glGenerateMipmap(GL_TEXTURE_CUBE_MAP);

        // Loading 1-5 lvl mipmaps if they are found
        if (customMips) {
            for (int i = 0; i < 6; i++) {
                for (int j = 1; j < 6; j++) {
                    width.clear();
                    height.clear();
                    channels.clear();

                    textureData = stbi_loadf(path + "_mip_" + j + facePostifixes[i] + extension,
                            width, height, channels, 3);
                    if (textureData == null) {
                        System.err.println("Cubemap mip " + path + "_mip_" + j + facePostifixes[i] + extension
                                + " is not found");
                        break;
                    }

                    glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i,
                            j, GL_RGB16F, width.get(0), height.get(0), 0, GL_RGB, GL_FLOAT, textureData);
                    stbi_image_free(textureData);
                }
            }
        }

        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);

        glBindTexture(GL_TEXTURE_CUBE_MAP, 0);
    }

    /**
     * Binds texture to current GL context
     */
    public void use()
    {
        glBindTexture(GL_TEXTURE_CUBE_MAP, texture);
    }

    /**
     * Deletes texture from VRAM
     */
    public void remove()
    {
        glDeleteTextures(texture);
    }
}
