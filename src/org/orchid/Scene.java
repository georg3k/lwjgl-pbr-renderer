package org.orchid;

import org.joml.*;
import org.lwjgl.assimp.*;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.*;
import java.util.*;

public class Scene
{
    private static Node sceneTree;
    private static Camera mainCamera;
    private static ArrayList<Mesh> opaqueMeshes = new ArrayList<>();
    private static ArrayList<Mesh> transparentMeshes = new ArrayList<>();

    /**
     * Updates scene
     */
    public static void update()
    {
        sceneTree.update();
    }

    /**
     * Draws opaque meshes (used for deferred pass)
     */
    public static void drawOpaque()
    {
        mainCamera.use();
        for (Mesh m : opaqueMeshes)
            m.draw();
    }

    /**
     * Draws transparent meshes (used for forward pass)
     */
    public static void drawTransparent()
    {
        mainCamera.use();
        for (Mesh m : transparentMeshes)
            m.draw();
    }

    /**
     * Loads scene
     *
     * @param path path to scene file
     */
    static void loadScene(String path)
    {
        if(sceneTree != null)
            sceneTree.remove();
        sceneTree = null;
        mainCamera = null;
        opaqueMeshes.clear();
        transparentMeshes.clear();

        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            parser.parse(path, new DefaultHandler()
            {
                String element, value;
                boolean isTransparent = false;
                Node node = null;
                ArrayList<Float> values = new ArrayList<>();

                @Override
                public void startElement(String namespace, String lName, String gName, Attributes attr)
                {
                    element = gName;

                    switch (element) {
                        case "node":
                            node = new Node(attr.getValue("name"), node);
                            if (sceneTree == null)
                                sceneTree = node;
                            break;
                        case "camera":
                            node = new Camera(attr.getValue("name"), node);
                            if (mainCamera == null)
                                mainCamera = (Camera) node;
                            break;
                    }
                }

                @Override
                public void characters(char[] characters, int start, int length)
                {
                    // Ignoring whitespace padding
                    if (new String(characters, start, length).trim().length() == 0)
                        return;

                    value = new String(characters, start, length);

                    switch (element) {
                        case "x":
                        case "y":
                        case "z":
                            values.add(Float.parseFloat(value));
                            break;
                        case "near":
                            ((Camera) node).setNear(Float.parseFloat(value));
                            break;
                        case "far":
                            ((Camera) node).setFar(Float.parseFloat(value));
                            break;
                        case "fov":
                            ((Camera) node).setFOV(Float.parseFloat(value));
                            break;
                        case "transparent":
                            isTransparent = Boolean.parseBoolean(value);
                            break;
                    }
                }

                @Override
                public void endElement(String namespace, String lName, String gName)
                {
                    switch (gName) {
                        case "node":
                        case "camera":
                            node = node.getParent();
                            break;
                        case "position":
                            node.setPosition(values.get(0), values.get(1), values.get(2));
                            break;
                        case "rotation":
                            node.setRotation(values.get(0), values.get(1), values.get(2));
                            break;
                        case "scale":
                            node.setScale(values.get(0), values.get(1), values.get(2));
                            break;
                        case "mesh":
                            AIScene scene = Assimp.aiImportFile(value,
                                    Assimp.aiProcess_Triangulate
                                            | Assimp.aiProcess_FlipUVs
                                            | Assimp.aiProcess_CalcTangentSpace);

                            node.addChild(loadModel(scene.mRootNode(), scene));
                            break;
                    }
                }

                private Node loadModel(AINode aiNode, AIScene aiScene)
                {
                    Node newNode = new Node(aiNode.mName().dataString());

                    Matrix4f matrix = new Matrix4f(
                            aiNode.mTransformation().a1(), aiNode.mTransformation().b1(), aiNode.mTransformation().c1(), aiNode.mTransformation().d1(),
                            aiNode.mTransformation().a2(), aiNode.mTransformation().b2(), aiNode.mTransformation().c2(), aiNode.mTransformation().d2(),
                            aiNode.mTransformation().a3(), aiNode.mTransformation().b3(), aiNode.mTransformation().c3(), aiNode.mTransformation().d3(),
                            aiNode.mTransformation().a4(), aiNode.mTransformation().b4(), aiNode.mTransformation().c4(), aiNode.mTransformation().d4()
                    );

                    Vector3f mediator = new Vector3f();

                    matrix.getTranslation(mediator);
                    newNode.setPosition(mediator);

                    matrix.getEulerAnglesZYX(mediator);
                    newNode.setRotation(mediator);

                    matrix.getScale(mediator);
                    newNode.setScale(mediator);

                    for (int i = 0; i < aiNode.mNumMeshes(); i++) {
                        AIMesh aiMesh = AIMesh.create(aiScene.mMeshes().get(aiNode.mMeshes().get(i)));
                        Mesh mesh = new Mesh(aiMesh.mName().dataString(), newNode);
                        mesh.loadMesh(aiMesh);

                        if (isTransparent)
                            transparentMeshes.add(mesh);
                        else
                            opaqueMeshes.add(mesh);
                    }

                    for (int i = 0; i < aiNode.mNumChildren(); i++)
                        newNode.addChild(loadModel(AINode.create(aiNode.mChildren().get(i)), aiScene));

                    return newNode;
                }
            });
        } catch (Exception e) {
            System.err.println("Scene file loading failed");
            e.printStackTrace();
        }
    }
}
