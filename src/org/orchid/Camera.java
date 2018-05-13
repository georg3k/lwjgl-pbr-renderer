package org.orchid;

import org.joml.Matrix4f;

public class Camera extends Node
{
    boolean matrixUpdated = false;
    private float near = 0.1f;
    private float far = 1000.0f;
    private float fov = 1.0f;
    private Matrix4f viewMatrix = new Matrix4f();
    private Matrix4f projectionMatrix = new Matrix4f();

    /**
     * Constructor
     *
     * @param name unique identifier
     */
    public Camera(String name)
    {
        super(name);
    }

    /**
     * Constructor
     *
     * @param name   node name
     * @param parent parent node
     */
    public Camera(String name, Node parent)
    {
        super(name, parent);
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
        recalculateProjectionMatrix();
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

    @Override
    protected void setOutdated()
    {
        matrixUpdated = false;
        super.setOutdated();
    }

    private void recalculateViewMatrix()
    {
        viewMatrix.set(getModelMatrix()).invert();
        matrixUpdated = true;
    }

    private void recalculateProjectionMatrix()
    {
        projectionMatrix.identity().perspective(fov,
                Float.parseFloat(Orchid.getProperty("window_width")) /
                        Float.parseFloat(Orchid.getProperty("window_height")), near, far);
    }
}
