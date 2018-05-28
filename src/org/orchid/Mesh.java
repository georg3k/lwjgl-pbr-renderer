package org.orchid;

import org.lwjgl.BufferUtils;
import org.lwjgl.assimp.AIMesh;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.GL_UNIFORM_BUFFER;

public class Mesh extends Node
{
    private int ubo;
    private int vao;
    private int verticesBuffer;
    private int normalsBuffer;
    private int bitangentsBuffer;
    private int uvsBuffer;
    private int ebo;
    private int numFaces;

    private Material material;

    private boolean matrixUpdated = false;
    private FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

    /**
     * Constructor
     *
     * @param name name of the node
     */
    public Mesh(String name)
    {
        super(name);

        ubo = glGenBuffers();
        glBindBuffer(GL_UNIFORM_BUFFER, ubo);
        glBufferData(GL_UNIFORM_BUFFER, 64, GL_STATIC_DRAW);
        glBindBuffer(GL_UNIFORM_BUFFER, 0);
    }

    /**
     * Constructor
     *
     * @param name   name of the node
     * @param parent parent node
     */
    public Mesh(String name, Node parent)
    {
        this(name);
        setParent(parent);
    }

    /**
     * Removes mesh from scene
     */
    @Override
    public void remove()
    {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(ubo);
        glDeleteBuffers(verticesBuffer);
        glDeleteBuffers(normalsBuffer);
        glDeleteBuffers(bitangentsBuffer);
        glDeleteBuffers(uvsBuffer);

        super.remove();
    }

    /**
     * Material getter
     *
     * @return material
     */
    public Material getMaterial()
    {
        return material;
    }

    /**
     * Material setter
     *
     * @param material material
     */
    public void setMaterial(Material material)
    {
        this.material = material;
    }

    /**
     * Loads mesh from Assimp mesh struct
     *
     * @param aiMesh Assimp mesh
     */
    public void loadMesh(AIMesh aiMesh)
    {
        // TODO: Optimization needed

        FloatBuffer vertices = BufferUtils.createFloatBuffer(aiMesh.mNumVertices() * 3);
        FloatBuffer normals = BufferUtils.createFloatBuffer(aiMesh.mNumVertices() * 3);
        FloatBuffer bitangents = BufferUtils.createFloatBuffer(aiMesh.mNumVertices() * 3);
        FloatBuffer uvs = BufferUtils.createFloatBuffer(aiMesh.mNumVertices() * 2);

        boolean hasNormals = aiMesh.mNormals() != null;
        boolean hasBitangents = aiMesh.mBitangents() != null;
        boolean hasUVs = aiMesh.mTextureCoords(0) != null;

        for (int i = 0; i < aiMesh.mNumVertices(); i++) {
            vertices.put(aiMesh.mVertices().get(i).x());
            vertices.put(aiMesh.mVertices().get(i).y());
            vertices.put(aiMesh.mVertices().get(i).z());

            if (hasNormals) {
                normals.put(aiMesh.mNormals().get(i).x());
                normals.put(aiMesh.mNormals().get(i).y());
                normals.put(aiMesh.mNormals().get(i).z());
            }

            if (hasBitangents) {
                bitangents.put(aiMesh.mBitangents().get(i).x());
                bitangents.put(aiMesh.mBitangents().get(i).y());
                bitangents.put(aiMesh.mBitangents().get(i).z());
            }

            if (hasUVs) {
                uvs.put(aiMesh.mTextureCoords(0).get(i).x());
                uvs.put(aiMesh.mTextureCoords(0).get(i).y());
            }
        }

        IntBuffer indices = BufferUtils.createIntBuffer(aiMesh.mNumFaces() * 3);

        for (int i = 0; i < aiMesh.mNumFaces(); i++) {
            indices.put(aiMesh.mFaces().get(i).mIndices().get(0));
            indices.put(aiMesh.mFaces().get(i).mIndices().get(1));
            indices.put(aiMesh.mFaces().get(i).mIndices().get(2));
        }

        numFaces = indices.capacity();

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vertices.rewind();
        verticesBuffer = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, verticesBuffer);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        glEnableVertexAttribArray(Shader.POSITION_LOCATION);
        glVertexAttribPointer(Shader.POSITION_LOCATION, 3, GL_FLOAT, false, 0, 0);

        if (hasNormals) {
            normals.rewind();
            normalsBuffer = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, normalsBuffer);
            glBufferData(GL_ARRAY_BUFFER, normals, GL_STATIC_DRAW);

            glEnableVertexAttribArray(Shader.NORMAL_LOCATION);
            glVertexAttribPointer(Shader.NORMAL_LOCATION, 3, GL_FLOAT, false, 0, 0);
        }

        if (hasBitangents) {
            bitangents.rewind();
            bitangentsBuffer = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, bitangentsBuffer);
            glBufferData(GL_ARRAY_BUFFER, bitangents, GL_STATIC_DRAW);

            glEnableVertexAttribArray(Shader.BITANGENT_LOCATION);
            glVertexAttribPointer(Shader.BITANGENT_LOCATION, 3, GL_FLOAT, false, 0, 0);
        }

        if (hasUVs) {
            uvs.rewind();
            uvsBuffer = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, uvsBuffer);
            glBufferData(GL_ARRAY_BUFFER, uvs, GL_STATIC_DRAW);

            glEnableVertexAttribArray(Shader.UVS_LOCATION);
            glVertexAttribPointer(Shader.UVS_LOCATION, 2, GL_FLOAT, false, 0, 0);
        }

        indices.rewind();
        int elementsBuffer = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, elementsBuffer);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        glBindVertexArray(0);
    }

    /**
     * Draws mesh
     */
    public void draw()
    {
        if (!matrixUpdated) {
            matrixBuffer.clear();
            getModelMatrix().get(matrixBuffer);
            glBindBuffer(GL_UNIFORM_BUFFER, ubo);
            glBufferData(GL_UNIFORM_BUFFER, matrixBuffer, GL_STATIC_DRAW);
            glBindBuffer(GL_UNIFORM_BUFFER, 0);

            matrixUpdated = true;
        }

        material.use();

        glBindBufferBase(GL_UNIFORM_BUFFER, Shader.MODEL_BLOCK, ubo);
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, numFaces, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    @Override
    protected void setOutdated()
    {
        matrixUpdated = false;
        super.setOutdated();
    }
}
