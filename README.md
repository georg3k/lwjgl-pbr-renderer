# Orchid

Orchid is an **educational project in the field of real-time 3d computer graphics**. It is written in Java and using OpenGL API.

## Feature list

* Scene and configuration loading from xml files
* Loading variety of 3d meshes using Assimp library
* Loading regular textures and cubemaps (including HDR textures)
* Deferred rendering
* HDR (auto-adjustment is yet to come)
* Physically Based Rendering (metallic workflow)

## Currently in progress

* Image based lighting
* Light volumes

## Screenshots

![PBR test](https://i.imgur.com/ciPhHnP.png "PBR shader exmaple")

![Material sample](https://i.imgur.com/WfQBA7q.png "Rusty ball, bright interior")

![Material sample](https://i.imgur.com/nfNGXWJ.png "Shiny ball, bright interior")

![Material sample](https://i.imgur.com/v0ghakQ.jpg "Wooden ball, exterior")



## Build

To build this project you will need LWJGL library configuration with following modules: *Core, OpenGL, GLFW, Assimp, stb_image and JOML*
