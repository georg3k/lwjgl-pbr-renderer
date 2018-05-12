package org.orchid;


import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Node class - represents 3d scene entity
 */
public class Node
{
    private String id;

    private Vector3f position = new Vector3f(0.0f);
    private Vector3f rotation = new Vector3f();
    private Vector3f scale = new Vector3f(1.0f);

    private Node parent;
    private List<Node> children = new ArrayList<>();

    private Matrix4f positionMatrix = new Matrix4f();
    private Matrix4f rotationMatrix = new Matrix4f();
    private Matrix4f scaleMatrix = new Matrix4f();

    private Matrix4f modelMatrix = new Matrix4f();

    private boolean matrixUpdated = false;

    /**
     * Constructor
     *
     * @param id unique identifier of the node
     */
    public Node(String id)
    {
        this.id = id;
    }

    /**
     * Constructor
     *
     * @param id     unique identifier of the node
     * @param parent parent of the node
     */
    public Node(String id, Node parent)
    {
        this(id);
        setParent(parent);
    }

    /**
     * Parent node getter
     *
     * @return parent node
     */
    public Node getParent()
    {
        return parent;
    }

    /**
     * Parent node setter
     *
     * @param parent new parent
     */
    public void setParent(Node parent)
    {
        this.parent = parent;
        if (parent != null)
            parent.addChild(this);
    }

    /**
     * Number of children
     *
     * @return number of children
     */
    public int getNumChild()
    {
        return children.size();
    }

    /**
     * Child getter
     *
     * @param index index of the child
     * @return child node
     */
    public Node getChild(int index)
    {
        return children.get(index);
    }

    /**
     * Adds new child node
     *
     * @param child new child
     */
    public void addChild(Node child)
    {
        children.add(child);
        if (child.getParent() != this)
            child.setParent(this);
    }

    /**
     * Removes node from children
     *
     * @param index index of the child to remove
     */
    public void removeChild(int index)
    {
        children.get(index).setParent(null);
        children.remove(index);
    }

    /**
     * Unique identifier getter
     *
     * @return unique identifier
     */
    public String getId()
    {
        return id;
    }

    /**
     * Position getter
     *
     * @return local position
     */
    public Vector3f getPosition()
    {
        return new Vector3f(position);
    }

    /**
     * Position setter
     *
     * @param position new local position
     */
    public void setPosition(Vector3f position)
    {
        this.position.set(position);
        recalculatePosition();
    }

    /**
     * Position setter
     *
     * @param x x coord of the new position
     * @param y y coord of the new position
     * @param z z coord of the new position
     */
    public void setPosition(float x, float y, float z)
    {
        position.set(x, y, z);
        recalculatePosition();
    }

    /**
     * Translates node position
     *
     * @param translation local translation
     */
    public void translate(Vector3f translation)
    {
        position.add(translation);
        recalculatePosition();
    }

    /**
     * Rotation getter
     *
     * @return local rotation
     */
    public Vector3f getRotation()
    {
        return new Vector3f(rotation);
    }

    /**
     * Rotation setter
     *
     * @param rotation new local rotation
     */
    public void setRotation(Vector3f rotation)
    {
        this.rotation.set(rotation);
        recalculateRotation();
    }

    /**
     * Rotation setter
     *
     * @param x x angle of the new rotation
     * @param y y angle of the new rotation
     * @param z z angle of the new rotation
     */
    public void setRotation(float x, float y, float z)
    {
        rotation.set(x, y, z);
        recalculateRotation();
    }

    /**
     * Scale getter
     *
     * @return local scale
     */
    public Vector3f getScale()
    {
        return new Vector3f(scale);
    }

    /**
     * Scale setter
     *
     * @param scale new local scale
     */
    public void setScale(Vector3f scale)
    {
        this.scale.set(scale);
        recalculateScale();
    }

    /**
     * Scale setter
     *
     * @param x x factor of the new scale
     * @param y y factor of the new scale
     * @param z z factor of the new scale
     */
    public void setScale(float x, float y, float z)
    {
        scale.set(x, y, z);
        recalculateScale();
    }

    /**
     * Scales node
     *
     * @param factor scale factor
     */
    public void scale(Vector3f factor)
    {
        scale.mul(factor);
        recalculateScale();
    }

    /**
     * Updated node and all its' children
     */
    public void update()
    {
        for (Node n : children)
            n.update();
    }

    /**
     * Model matrix
     *
     * @return model matrix in the global space
     */
    private Matrix4f getModelMatrix()
    {
        if (!matrixUpdated)
            // TODO: Check for math correctness
            return modelMatrix.identity().mul(positionMatrix).mul(rotationMatrix).mul(scaleMatrix);

        Matrix4f resultMatrix = new Matrix4f();
        if (parent != null)
            resultMatrix.set(parent.getModelMatrix());

        // TODO: Check for math correctness
        return resultMatrix.mul(modelMatrix);

    }

    // Sets node (and all its' children) matrices outdated status and forces node to recalculate them for optimization
    private void setOutdated()
    {
        matrixUpdated = false;
        for (Node n : children)
            n.setOutdated();
    }

    private void recalculatePosition()
    {
        positionMatrix.translation(position);
        setOutdated();
    }

    private void recalculateRotation()
    {
        // TODO: Check for math correctness
        rotationMatrix.rotation(1.0f, rotation);
        setOutdated();
    }

    private void recalculateScale()
    {
        scaleMatrix.identity().scale(scale);
        setOutdated();
    }
}
