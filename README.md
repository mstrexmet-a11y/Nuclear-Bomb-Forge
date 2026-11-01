# Nuclear Bomb — Forge 1.20.1

A fictional, cinematic Minecraft effect mod for Forge 1.20.1.

## Features
- Nuclear-device item in the Tools & Utilities creative tab.
- Bright flash, explosion particles and sound.
- Expanding ground dust ring.
- Layered rising mushroom-cloud silhouette.
- Central smoke column.

**Gameplay note:** this initial version is a visual-only effect. It does not destroy blocks or simulate real-world weapon behavior.

## Build
Requirements: Java 17 and internet access for Gradle dependency resolution.

On Windows:
```bat
gradlew.bat build
```

On Linux/macOS:
```sh
./gradlew build
```

The mod JAR will be in `build/libs/`.
Use the Forge 1.20.1 (47.x) profile.
