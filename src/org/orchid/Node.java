package org.orchid;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Node class - represents 3d scene entity
 */
public class Node
{
    private String name;

    private Vector3f position = new Vector3f(0.0f);
    private Vector3f rotation = new Vector3f();
    private Vector3f scale = new Vector3f(1.0f);

    private Node parent;
    private List<Node> children = new ArrayList<>();
    private Map<String, Node> childrenMap = new HashMap<>();

    private Matrix4f positionMatrix = new Matrix4f();
    private Matrix4f rotationMatrix = new Matrix4f();
    private Matrix4f scaleMatrix = new Matrix4f();

    private Matrix4f modelMatrix = new Matrix4f();

    private boolean matrixUpdated = false;

    /**
     * Constructor
     *
     * @param name name of the node
     */
    public Node(String name)
    {
        this.name = name;
    }

    /**
     * Constructor
     *
     * @param name     name of the node
     * @param parent parent of the node
     */
    public Node(String name, Node parent)
    {
        this(name);
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
     * Child getter by index
     *
     * @param index index of the child
     * @return child node
     */
    public Node getChild(int index)
    {
        return children.get(index);
    }

    /**
     * Child getter by name
     *
     * @param name name of the child
     * @return child node
     */
    public Node getChild(String name)
    {
        return childrenMap.get(name);
    }

    /**
     * Adds new child node
     *
     * @param child new child
     */
    public void addChild(Node child)
    {
        if (childrenMap.containsKey(child.getName()))
            return;

        // Same name avoidance
        int index = 0;
        Pattern p = Pattern.compile(child.name + "\\s*(\\d*)$");
        for(Node n : children)
        {
            Matcher m = p.matcher(n.name);

            if(m.matches())
            {
                if(index == 0) index = 1;

                if(m.group(1).length() > 0) {
                    index = index < Integer.parseInt(m.group(1)) ? Integer.parseInt(m.group(1)) : index;
                }
            }
        }

        if(index != 0)
            child.name = child.name.replaceAll("\\s*\\d*$",  " " + (index + 1));

        children.add(child);
        childrenMap.put(child.getName(), child);

        child.setOutdated();

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
        childrenMap.remove(children.get(index).getName());
        children.get(index).setParent(null);
        children.remove(index);
    }

    /**
     * Removes node from children
     *
     * @param node child node
     */
    public void removeChild(Node node)
    {
        if (!children.contains(node))
            return;

        node.setParent(null);
        children.remove(node);
        childrenMap.remove(node.getName());
    }

    /**
     * Removes node from scene
     */
    public void remove()
    {
        parent.removeChild(this);
        for (Node n : children)
            n.remove();
    }

    /**
     * Unique identifier getter
     *
     * @return unique identifier
     */
    public String getName()
    {
        return name;
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
    public Matrix4f getModelMatrix()
    {
        if (!matrixUpdated)
            recalculateModelMatrix();

        return new Matrix4f(modelMatrix);

    }

    // Sets node (and all its' children) matrices outdated status and forces node to recalculate them for optimization
    protected void setOutdated()
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
        rotationMatrix.rotationXYZ(rotation.x(), rotation.y(), rotation.z());
        setOutdated();
    }

    private void recalculateScale()
    {
        scaleMatrix.identity().scale(scale);
        setOutdated();
    }

    private void recalculateModelMatrix()
    {
        modelMatrix.mul(positionMatrix).mul(rotationMatrix).mul(scaleMatrix);
        if(parent != null)
            modelMatrix.set(parent.getModelMatrix().mul(modelMatrix));

        matrixUpdated = true;
    }
}
