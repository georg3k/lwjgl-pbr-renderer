package org.orchid;

import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL31.GL_UNIFORM_BUFFER;

public class Material
{
    private Vector4f albedo = new Vector4f(1.0f);
    private Texture albedoMap = null;
    ByteBuffer materialBuffer = BufferUtils.createByteBuffer(64);
    private Texture metalnessMap = null;
    private float roughness = 1.0f;
    private Texture roughnessMap = null;

    private Texture normalMap = null;
    private Vector3f emission = new Vector3f(0.0f);
    private Texture emissionMap = null;
    private Texture ambientOcclusionMap = null;

    private boolean bufferUpdated = false;
    private int buffer;
    private float metalness = 1.0f;

    /**
     * Constructor - allocates material UBO
     */
    public Material()
    {
        buffer = glGenBuffers();
        glBindBuffer(GL_UNIFORM_BUFFER, buffer);
        glBufferData(GL_UNIFORM_BUFFER, 80, GL_STATIC_DRAW);
        glBindBuffer(GL_UNIFORM_BUFFER, 0);
    }

    /**
     * Removes material from GRAM
     */
    public void remove()
    {
        glDeleteBuffers(buffer);
    }

    /**
     * Albedo value getter
     *
     * @return albedo value
     */
    public Vector4f getAlbedo()
    {
        return new Vector4f(albedo);
    }

    /**
     * Albedo value setter
     *
     * @param albedo albedo value
     */
    public void setAlbedo(Vector4f albedo)
    {
        this.albedo.set(albedo);
        bufferUpdated = false;
    }

    /**
     * Alternative albedo settre
     *
     * @param r red component
     * @param g green component
     * @param b blue component
     */
    public void setAlbedo(float r, float g, float b, float a)
    {
        albedo.set(r, g, b, a);
        bufferUpdated = false;
    }

    /**
     * Albedo map getter
     *
     * @return albedo map
     */
    public Texture getAlbedoMap()
    {
        return albedoMap;
    }

    /**
     * Albedo map setter
     *
     * @param albedoMap albedo map
     */
    public void setAlbedoMap(Texture albedoMap)
    {
        this.albedoMap = albedoMap;
        bufferUpdated = false;
    }

    /**
     * Metalness value getter
     *
     * @return metalness value
     */
    public float getMetalness()
    {
        return metalness;
    }

    /**
     * Metalness setter
     *
     * @param metalness metalness value
     */
    public void setMetalness(float metalness)
    {
        this.metalness = metalness;
        bufferUpdated = false;
    }

    /**
     * Metalness map getter
     *
     * @return metalness map
     */
    public Texture getMetalnessMap()
    {
        return metalnessMap;
    }

    /**
     * Metalness map setter
     *
     * @param metalnessMap metalness map
     */
    public void setMetalnessMap(Texture metalnessMap)
    {
        this.metalnessMap = metalnessMap;
        bufferUpdated = false;
    }

    /**
     * Roughness value getter
     *
     * @return roughness value
     */
    public float getRoughness()
    {
        return roughness;
    }

    /**
     * Roughness value setter
     *
     * @param roughness roughness value
     */
    public void setRoughness(float roughness)
    {
        this.roughness = roughness;
        bufferUpdated = false;
    }

    /**
     * Roughness map getter
     *
     * @return toughness map
     */
    public Texture getRoughnessMap()
    {
        return roughnessMap;
    }

    /**
     * Roughness map setter
     *
     * @param roughnessMap roughness map
     */
    public void setRoughnessMap(Texture roughnessMap)
    {
        this.roughnessMap = roughnessMap;
        bufferUpdated = false;
    }

    /**
     * Normal map getter
     *
     * @return roughness map
     */
    public Texture getNormalMap()
    {
        return normalMap;
    }

    /**
     * Roughness map setter
     *
     * @param normalMap roughness map
     */
    public void setNormalMap(Texture normalMap)
    {
        this.normalMap = normalMap;
        bufferUpdated = false;
    }

    /**
     * Emission value getter
     *
     * @return emission map
     */
    public Vector3f getEmission()
    {
        return new Vector3f(emission);
    }

    /**
     * Emission value setter
     *
     * @param emission emission value
     */
    public void setEmission(Vector3f emission)
    {
        this.emission.set(emission);
    }

    /**
     * Alternative emission value setter
     *
     * @param r red component
     * @param g green component
     * @param b blue component
     */
    public void setEmission(float r, float g, float b)
    {
        emission.set(r, g, b);
    }

    /**
     * Emission map getter
     *
     * @return emission map
     */
    public Texture getEmissionMap()
    {
        return emissionMap;
    }

    /**
     * Emission map setter
     *
     * @param emissionMap emission map setter
     */
    public void setEmissionMap(Texture emissionMap)
    {
        this.emissionMap = emissionMap;
        bufferUpdated = false;
    }

    /**
     * Ambient occlusion map getter
     *
     * @return ambient occlusion
     */
    public Texture getAmbientOcclusionMap()
    {
        return ambientOcclusionMap;
    }

    /**
     * Ambient occlusion map setter
     *
     * @param ambientOcclusionMap ambient occlusion map
     */
    public void setAmbientOcclusionMap(Texture ambientOcclusionMap)
    {
        this.ambientOcclusionMap = ambientOcclusionMap;
        bufferUpdated = false;
    }

    /**
     * Binds material UBO
     */
    public void use()
    {
        if(!bufferUpdated)
            updateBuffer();

        if(albedoMap != null) {
            glActiveTexture(GL_TEXTURE0 + Shader.ALBEDO_MAP_BINDING);
            albedoMap.use();
        }
        if(metalnessMap != null) {
            glActiveTexture(GL_TEXTURE0 + Shader.METALNESS_MAP_BINDING);
            metalnessMap.use();
        }
        if(roughnessMap != null) {
            glActiveTexture(GL_TEXTURE0 + Shader.ROUGHNESS_MAP_BINDING);
            roughnessMap.use();
        }
        if(normalMap != null) {
            glActiveTexture(GL_TEXTURE0 + Shader.NORMAL_MAP_BINDING);
            normalMap.use();
        }
        if(emissionMap != null) {
            glActiveTexture(GL_TEXTURE0 + Shader.EMISSION_MAP_BINDING);
            emissionMap.use();
        }
        if(ambientOcclusionMap != null) {
            glActiveTexture(GL_TEXTURE0 + Shader.AMBIENT_OCCLUSION_MAP_BINDING);
            ambientOcclusionMap.use();
        }
        glBindBufferBase(GL_UNIFORM_BUFFER, Shader.MATERIAL_BLOCK, buffer);
    }

    private void updateBuffer()
    {
        materialBuffer.clear();

        materialBuffer.putFloat(0, albedo.x());
        materialBuffer.putFloat(4, albedo.y());
        materialBuffer.putFloat(8, albedo.z());
        materialBuffer.putInt(12, albedoMap == null ? 0 : 1);
        materialBuffer.putInt(16, normalMap == null ? 0 : 1);
        materialBuffer.putFloat(20, metalness);
        materialBuffer.putInt(24, metalnessMap == null ? 0 : 1);
        materialBuffer.putFloat(28, roughness);
        materialBuffer.putInt(32, roughnessMap == null ? 0 : 1);
        materialBuffer.putFloat(36, emission.x());
        materialBuffer.putFloat(40, emission.y());
        materialBuffer.putFloat(44, emission.z());
        materialBuffer.putInt(48, emissionMap == null ? 0 : 1);
        materialBuffer.putInt(54, ambientOcclusionMap == null ? 0 : 1);

        glBindBuffer(GL_UNIFORM_BUFFER, buffer);
        glBufferData(GL_UNIFORM_BUFFER, materialBuffer, GL_STATIC_DRAW);
        glBindBuffer(GL_UNIFORM_BUFFER, 0);

        bufferUpdated = true;
    }
}
