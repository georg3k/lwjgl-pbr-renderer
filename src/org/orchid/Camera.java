package org.orchid;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL31.GL_UNIFORM_BUFFER;

public class Camera extends Node
{
    boolean matrixUpdated = false;
    private float near = 0.1f;
    private float far = 1000.0f;
    private float fov = 1.0f;
    private Matrix4f viewMatrix = new Matrix4f();
    private Matrix4f projectionMatrix = new Matrix4f();
    private int vbo;
    private FloatBuffer viewBuffer = BufferUtils.createFloatBuffer(16);
    private FloatBuffer projectionBuffer = BufferUtils.createFloatBuffer(16);

    /**
     * Constructor
     *
     * @param name unique identifier
     */
    public Camera(String name)
    {
        super(name);

        vbo = glGenBuffers();
        glBindBuffer(GL_UNIFORM_BUFFER, vbo);
        glBufferData(GL_UNIFORM_BUFFER, 128, GL_STATIC_DRAW);
        glBindBuffer(GL_UNIFORM_BUFFER, 0);
    }

    /**
     * Constructor
     *
     * @param name   node name
     * @param parent parent node
     */
    public Camera(String name, Node parent)
    {
        this(name);
        setParent(parent);
    }

    /**
     * Removes camera from scene
     */
    @Override
    public void remove()
    {
        glDeleteBuffers(vbo);
        super.remove();
    }

    /**
     * Near clipping plane getter
     *
     * @return near value
     */
    public float getNear()
    {
        return near;
    }

    /**
     * Near clipping plane setter
     *
     * @param near new near value
     */
    public void setNear(float near)
    {
        this.near = near;
        matrixUpdated = false;
    }

    /**
     * Far clipping plane getter
     *
     * @return far value
     */
    public float getFar()
    {
        return far;
    }

    /**
     * Far clipping plane setter
     *
     * @param far new far value
     */
    public void setFar(float far)
    {
        this.far = far;
        matrixUpdated = false;
    }

    /**
     * Field of view getter
     *
     * @return field of view value
     */
    public float getFOV()
    {
        return fov;
    }

    /**
     * Field of view setter
     *
     * @param fov new field of view value
     */
    public void setFOV(float fov)
    {
        this.fov = fov;
        recalculateProjectionMatrix();
    }

    /**
     * View matrix
     *
     * @return view matrix
     */
    public Matrix4f getViewMatrix()
    {
        if (!matrixUpdated)
            recalculateViewMatrix();

        return new Matrix4f(viewMatrix);
    }

    /**
     * Projection matrix
     *
     * @return projection matrix
     */
    public Matrix4f getProjectionMatrix()
    {
        return new Matrix4f(projectionMatrix);
    }

    /**
     * Bind view and projection matrices vbo to shader
     */
    public void use()
    {
        if (!matrixUpdated)
            recalculateViewMatrix();
        glBindBufferBase(GL_UNIFORM_BUFFER, Shader.CAMERA_BLOCK, vbo);
    }

    @Override
    protected void setOutdated()
    {
        matrixUpdated = false;
        super.setOutdated();
    }

    private void recalculateViewMatrix()
    {
        viewBuffer.clear();
        viewMatrix.set(getModelMatrix()).invert();
        viewMatrix.get(viewBuffer);
        glBindBuffer(GL_UNIFORM_BUFFER, vbo);
        glBufferSubData(GL_UNIFORM_BUFFER, 0, viewBuffer);
        glBindBuffer(GL_UNIFORM_BUFFER, 0);
        matrixUpdated = true;
    }

    private void recalculateProjectionMatrix()
    {
        projectionBuffer.clear();
        projectionMatrix.identity().perspective(fov,
                Float.parseFloat(Configuration.getProperty("window_width")) /
                        Float.parseFloat(Configuration.getProperty("window_height")), near, far);
        projectionMatrix.get(projectionBuffer);
        glBindBuffer(GL_UNIFORM_BUFFER, vbo);
        glBufferSubData(GL_UNIFORM_BUFFER, 64, projectionBuffer);
        glBindBuffer(GL_UNIFORM_BUFFER, 0);
    }
}
