# Nuclear Bomb — Forge 1.20.1

A fictional cinematic visual-effects mod for Minecraft Forge 1.20.1.

## Current visual implementation
- A custom multi-part 3D item assembled from model cuboids and metallic/industrial Minecraft atlas textures.
- A procedural world-space mushroom-cloud mesh rendered locally with position-colour triangles (not vanilla smoke/explosion particle sprites).
- A rising stem, scalloped volumetric crown, warm inner billows, drifting wisps and an expanding ground ring.
- Animated growth, subtle billow motion, screen flash and camera shake.
- The scene is cosmetic: it does not break blocks or damage entities.

## Performance
The cloud is generated procedurally on the client and is visible while the local cinematic timer is active. It uses a bounded mesh (rather than spawning hundreds of persistent particles).

## Build
Requirements: Java 17 and Gradle 8.x with internet access for dependency resolution.

```sh
gradle build
```

The mod JAR will be in `build/libs/`. Use Minecraft Forge 1.20.1 (47.x).
