package org.orchid;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AINode;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.Assimp;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.ArrayList;
import java.util.List;

public class Scene
{
    private static Node sceneTree;
    private static Camera mainCamera;
    private static ArrayList<Mesh> opaqueMeshes = new ArrayList<>();
    private static ArrayList<Mesh> transparentMeshes = new ArrayList<>();
    private static Material defaultMaterial = new Material();
    private static Cubemap skyboxCubemap = null;

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
     * Scenes skybox cubemap getter
     *
     * @return skybox
     */
    public static Cubemap getSkybox()
    {
        return skyboxCubemap;
    }

    /**
     * Scenes skybox cubemap setter
     *
     * @param skybox skybox
     */
    public static void setSkybox(Cubemap skybox)
    {
        skyboxCubemap = skybox;
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
                String name;
                List<String> characters = new ArrayList<>();

                Node node;
                Material material;
                boolean isMeshOpaque = true;

                String[] valuebleString =
                        {
                                "positive_x", "negative_x", "positive_y", "negative_y", "positive_z", "negative_z",
                                "x", "y", "z", "r", "g", "b", "a",
                                "metalness", "roughness",
                                "albedo_map", "metalness_map", "roughness_map", "normal_map", "emission_map", "ambient_occlusion_map",
                                "mesh_path", "transparent",
                                "near", "far", "fov",
                        };

                @Override
                public void startElement(String namespace, String localName, String globalName, Attributes attr)
                {
                    name = globalName;

                    switch (name) {
                        case "node":
                            node = new Node(attr.getValue("name"), node);
                            if (sceneTree == null) sceneTree = node;
                            break;
                        case "camera":
                            node = new Camera(attr.getValue("name"), node);
                            if (mainCamera == null) mainCamera = (Camera) node;
                            break;
                        case "material":
                            material = new Material();
                            break;
                    }
                }

                @Override
                public void characters(char[] characters, int begin, int length)
                {
                    if (new String(characters, begin, length).trim().length() == 0)
                        return;

                    for (String s : valuebleString)
                        if (name.equals(s)) {
                            this.characters.add(new String(characters, begin, length));
                            break;
                        }
                }

                @Override
                public void endElement(String namespace, String localName, String globalName)
                {
                    switch (globalName) {
                        case "skybox":
                            String[] strings = new String[characters.size()];
                            characters.toArray(strings);
                            skyboxCubemap = new Cubemap(strings);
                            characters.clear();
                            break;
                        case "node":
                        case "camera":
                            node = node.getParent();
                            break;
                        case "near":
                            ((Camera) node).setNear(Float.parseFloat(characters.get(0)));
                            characters.clear();
                            break;
                        case "far":
                            ((Camera) node).setFar(Float.parseFloat(characters.get(0)));
                            characters.clear();
                            break;
                        case "fov":
                            ((Camera) node).setFOV(Float.parseFloat(characters.get(0)));
                            characters.clear();
                            break;
                        case "position":
                            node.setPosition(Float.parseFloat(characters.get(0)), Float.parseFloat(characters.get(1)),
                                    Float.parseFloat(characters.get(2)));
                            characters.clear();
                            break;
                        case "rotation":
                            node.setRotation(Float.parseFloat(characters.get(0)), Float.parseFloat(characters.get(1)),
                                    Float.parseFloat(characters.get(2)));
                            characters.clear();
                            break;
                        case "scale":
                            node.setScale(Float.parseFloat(characters.get(0)), Float.parseFloat(characters.get(1)),
                                    Float.parseFloat(characters.get(2)));
                            characters.clear();
                            break;
                        case "albedo":
                            material.setAlbedo(Float.parseFloat(characters.get(0)), Float.parseFloat(characters.get(1)),
                                    Float.parseFloat(characters.get(2)), Float.parseFloat(characters.get(3)));
                            characters.clear();
                            break;
                        case "metalness":
                            material.setMetalness(Float.parseFloat(characters.get(0)));
                            characters.clear();
                            break;
                        case "roughness":
                            material.setRoughness(Float.parseFloat(characters.get(0)));
                            characters.clear();
                            break;
                        case "emission":
                            material.setEmission(Float.parseFloat(characters.get(0)), Float.parseFloat(characters.get(1)),
                                    Float.parseFloat(characters.get(2)));
                            characters.clear();
                            break;
                        case "ambient":
                            material.setAmbient(Float.parseFloat(characters.get(0)), Float.parseFloat(characters.get(1)),
                                    Float.parseFloat(characters.get(2)));
                            characters.clear();
                            break;
                        case "albedo_map":
                            material.setAlbedoMap(new Texture(characters.get(0)));
                            characters.clear();
                            break;
                        case "metalness_map":
                            material.setMetalnessMap(new Texture(characters.get(0)));
                            characters.clear();
                            break;
                        case "roughness_map":
                            material.setRoughnessMap(new Texture(characters.get(0)));
                            characters.clear();
                            break;
                        case "normal_map":
                            material.setNormalMap(new Texture(characters.get(0)));
                            characters.clear();
                            break;
                        case "emission_map":
                            material.setEmissionMap(new Texture(characters.get(0)));
                            characters.clear();
                            break;
                        case "ambient_occlusion_map":
                            material.setAmbientOcclusionMap(new Texture(characters.get(0)));
                            characters.clear();
                            break;
                        case "transparent":
                            isMeshOpaque = false;
                            break;
                        case "mesh_path":
                            AIScene aiScene = Assimp.aiImportFile(characters.get(0), Assimp.aiProcess_Triangulate
                                    | Assimp.aiProcess_FlipUVs | Assimp.aiProcess_CalcTangentSpace);

                            node.addChild(loadModel(aiScene.mRootNode(), aiScene));
                            characters.clear();
                            isMeshOpaque = true;
                            material = null;
                            break;
                    }
                }

                private Node loadModel(AINode aiNode, AIScene aiScene)
                {
                    Node node = new Node(aiNode.mName().dataString());

                    Matrix4f matrix = new Matrix4f(
                            aiNode.mTransformation().a1(), aiNode.mTransformation().b1(), aiNode.mTransformation().c1(), aiNode.mTransformation().d1(),
                            aiNode.mTransformation().a2(), aiNode.mTransformation().b2(), aiNode.mTransformation().c2(), aiNode.mTransformation().d2(),
                            aiNode.mTransformation().a3(), aiNode.mTransformation().b3(), aiNode.mTransformation().c3(), aiNode.mTransformation().d3(),
                            aiNode.mTransformation().a4(), aiNode.mTransformation().b4(), aiNode.mTransformation().c4(), aiNode.mTransformation().d4()
                    );

                    Vector3f mediator = new Vector3f();

                    matrix.getTranslation(mediator);
                    node.setPosition(mediator);

                    matrix.getEulerAnglesZYX(mediator);
                    node.setRotation(mediator);

                    matrix.getScale(mediator);
                    node.setScale(mediator);

                    for (int i = 0; i < aiNode.mNumMeshes(); i++) {
                        AIMesh aiMesh = AIMesh.create(aiScene.mMeshes().get(aiNode.mMeshes().get(i)));
                        Mesh mesh = new Mesh(aiMesh.mName().dataString(), node);
                        mesh.loadMesh(aiMesh);

                        if (isMeshOpaque)
                            opaqueMeshes.add(mesh);
                        else
                            transparentMeshes.add(mesh);

                        if (material != null)
                            mesh.setMaterial(material);
                        else
                            mesh.setMaterial(defaultMaterial);
                    }

                    for (int i = 0; i < aiNode.mNumChildren(); i++)
                        node.addChild(loadModel(AINode.create(aiNode.mChildren().get(i)), aiScene));

                    return node;
                }
            });
        } catch (Exception e) {
            System.err.println("Scene file loading failed");
            e.printStackTrace();
        }
    }
}
