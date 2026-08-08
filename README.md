# City Racer

[![Java](https://img.shields.io/badge/Java-26-orange?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/)
[![LWJGL](https://img.shields.io/badge/LWJGL-3.3.6-blueviolet?logo=maven)](https://www.lwjgl.org/)
[![OpenGL](https://img.shields.io/badge/OpenGL-3.3%20Core-5586A4?logo=opengl&logoColor=white)](https://www.opengl.org/)
[![JOML](https://img.shields.io/badge/JOML-1.10.8-3766ab?logo=maven)](https://github.com/JOML-CI/JOML)
[![GLFW](https://img.shields.io/badge/GLFW-3.3-8C1D40)]()
[![Build](https://img.shields.io/badge/build-Maven-2C2255?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![Platform](https://img.shields.io/badge/platform-Linux-3A9E2C?logo=linux)]()
[![Assimp](https://img.shields.io/badge/Assimp-LWJGL-87A96B?logo=assimp)](https://assimp.org/)
[![STB](https://img.shields.io/badge/STB-image%20loader-4C9CBE?logo=maven)](https://github.com/nothings/stb)
[![glTF](https://img.shields.io/badge/format-glTF%2FGLB-8233C5)](https://www.khronos.org/gltf/)
[![Shaders](https://img.shields.io/badge/shaders-GLSL-7C5CFF)](https://www.khronos.org/opengl/wiki/OpenGL_Shading_Language)
[![3D](https://img.shields.io/badge/renderer-custom%20OpenGL-22C55E)](./src/main/java/game/engine/Renderer.java)

[![Gameplay](https://img.shields.io/badge/gameplay-passenger%20delivery-22C55E)]()
[![Timer](https://img.shields.io/badge/timer-5%20minutes-EF4444)]()
[![Lives](https://img.shields.io/badge/lives-3-D97706)]()
[![HUD](https://img.shields.io/badge/renders-HUD%20%26%20minimap-0EA5E9)]()
[![Scoring](https://img.shields.io/badge/scoring-top%205%20file--based-0D9488)]()
[![Robustness](https://img.shields.io/badge/robustness-model--and--font--fallbacks-64748B)]()

[![GitHub repo size](https://img.shields.io/github/repo-size/PSLSbb/gamegame?logo=github)](https://github.com/PSLSbb/gamegame)
[![GitHub code size](https://img.shields.io/github/languages/code-size/PSLSbb/gamegame?logo=github)](https://github.com/PSLSbb/gamegame)
[![Last commit](https://img.shields.io/github/last-commit/PSLSbb/gamegame?logo=github)](https://github.com/PSLSbb/gamegame)
[![Top language](https://img.shields.io/github/languages/top/PSLSbb/gamegame?logo=java&logoColor=white)](https://github.com/PSLSbb/gamegame/search?l=Java)
[![Language count](https://img.shields.io/github/languages/count/PSLSbb/gamegame?logo=github)](https://github.com/PSLSbb/gamegame/languages)
[![Open issues](https://img.shields.io/github/issues/PSLSbb/gamegame?logo=github)](https://github.com/PSLSbb/gamegame/issues)
[![Closed issues](https://img.shields.io/github/issues-closed/PSLSbb/gamegame?logo=github)](https://github.com/PSLSbb/gamegame/issues?q=is%3Aissue+is%3Aclosed)
[![Open PRs](https://img.shields.io/github/issues-pr/PSLSbb/gamegame?logo=github)](https://github.com/PSLSbb/gamegame/pulls)
[![File count](https://img.shields.io/github/dir-file-count/PSLSbb/gamegame?logo=github)](https://github.com/PSLSbb/gamegame)

[![Stars](https://img.shields.io/github/stars/PSLSbb/gamegame?logo=github)](https://github.com/PSLSbb/gamegame/stargazers)
[![Forks](https://img.shields.io/github/forks/PSLSbb/gamegame?logo=github)](https://github.com/PSLSbb/gamegame/fork)
[![Watchers](https://img.shields.io/github/watchers/PSLSbb/gamegame?logo=github)](https://github.com/PSLSbb/gamegame/watchers)
[![Contributors](https://img.shields.io/github/contributors/PSLSbb/gamegame?logo=github)](https://github.com/PSLSbb/gamegame/graphs/contributors)
[![Commit activity](https://img.shields.io/github/commit-activity/m/PSLSbb/gamegame?logo=github)](https://github.com/PSLSbb/gamegame)
[![Release](https://img.shields.io/github/v/release/PSLSbb/gamegame?logo=github)](https://github.com/PSLSbb/gamegame/releases)
[![Release date](https://img.shields.io/github/release-date/PSLSbb/gamegame?logo=github)](https://github.com/PSLSbb/gamegame/releases)

City Racer is a Java/LWJGL passenger-delivery driving game. The player drives a van through a loaded 3D city map, picks up passengers at green markers, drops them off at yellow markers, avoids traffic cars, and tries to score as many deliveries as possible before time or lives run out.

The project is intentionally compact: it has a small custom rendering/gameplay stack, GLB model loading through Assimp, arcade-style vehicle movement, generated traffic routes, passenger objectives, a HUD, and a minimap.

## Contents

- [Features](#features)
- [Badges](#badges)
- [Requirements](#requirements)
- [Project Structure](#project-structure)
- [Architecture Overview](#architecture-overview)
- [Runtime Flow](#runtime-flow)
- [Game State Machine](#game-state-machine)
- [Passenger State Machine](#passenger-state-machine)
- [Bundled Assets](#bundled-assets)
- [Quick Start](#quick-start)
- [How to Play](#how-to-play)
- [Build and Run Commands](#build-and-run-commands)
- [Core Systems](#core-systems)
- [Asset and Model Pipeline](#asset-and-model-pipeline)
- [Rendering and Shaders](#rendering-and-shaders)
- [Gameplay Tuning Guide](#gameplay-tuning-guide)
- [Adding or Changing Content](#adding-or-changing-content)
- [Troubleshooting](#troubleshooting)
- [Development Notes](#development-notes)
- [Suggested Next Improvements](#suggested-next-improvements)
- [Related Documents](#related-documents)

## Features

- 3D city driving using LWJGL 3, GLFW, OpenGL 3.3, JOML, STB, and Assimp.
- GLB model loading for the city map, player vehicle, traffic vehicles, and passengers.
- Passenger pickup and drop-off gameplay.
- Five-minute game timer.
- Three-life collision system.
- Traffic cars following generated routes.
- Third-person chase camera.
- HUD with score, timer, speed, lives, delivery count, and passenger hints.
- Minimap with road samples, traffic routes, player direction, passengers, traffic, and spawn location.
- Menu, instructions screen, scoreboard, and game-over screen.
- Fallback city/car generation if a model fails to load.

## Badges

**Tech stack**

| Badge | Meaning |
| --- | --- |
| `Java 26` | Compiler target (`maven.compiler.source/target` in `pom.xml`) |
| `LWJGL 3.3.6` | LWJGL version pinned in `pom.xml` |
| `OpenGL 3.3 Core` | GLFW/OpenGL context requested by the engine |
| `JOML 1.10.8` | JOML math library dependency |
| `GLFW 3.3` | Window/input library (part of LWJGL 3.3.6) |
| `Assimp` | Model import via the LWJGL-Assimp binding |
| `STB` | Image loading via the LWJGL-STB binding |
| `glTF/GLB` | 3D asset format loaded by Assimp |
| `GLSL` | Shader language for the vertex/fragment shaders |
| `custom OpenGL` | The project's own renderer (`game.engine.Renderer`) |
| `Maven` | Build tool |
| `Linux` | Current LWJGL natives classifier (`natives-linux` in `pom.xml`) |

| Badge | Meaning |
| --- | --- |
| `passenger delivery` | Core gameplay loop |
| `5 minutes` / `3 lives` | Game timer and lives in `GameState` |
| `HUD & minimap` | In-game overlays rendered by `HUD` and `MiniMap` |
| `top 5 file-based` | Score persistence via `scoreboard.txt` |
| `model & font fallbacks` | Placeholder geometry/blocks when assets cannot load |

**Living GitHub stats**

| Badge | Meaning |
| --- | --- |
| `repo size`, `code size` | Total / source-only size of the repository |
| `last commit`, `commit activity` | Freshness and cadence of pushes |
| `top language`, `language count` | Primary and total languages on GitHub |
| `open issues`, `closed issues` | Issue tracker state |
| `open PRs` | Open pull requests |
| `file count` | Files tracked in the repository |
| `stars`, `forks`, `watchers`, `contributors` | Community engagement/contributor graph |
| `release`, `release date` | Latest tagged release and its date |

## Requirements

The Maven project is configured for:

- JDK 21 or newer, based on `maven.compiler.source` and `maven.compiler.target` in `pom.xml`.
- Maven 3.8+.
- Linux native LWJGL libraries, because `pom.xml` sets:

```xml
<lwjgl.natives>natives-linux</lwjgl.natives>
```

- A graphics environment that supports OpenGL 3.3 Core Profile.
- A desktop display session for GLFW. Headless terminals usually cannot run the game window without extra display setup.

For Windows or macOS, update the `lwjgl.natives` property in `pom.xml` to the matching LWJGL classifier, such as `natives-windows` or `natives-macos`.

## Project Structure

```text
.
|-- pom.xml
|-- source/                          # City map GLB asset
|   `-- burnin_rubber_crash_n_burn_city.glb
|-- 1982_toyota_hiace_combi.glb      # Player vehicle GLB asset
|-- bmw_m4_competition_m_package.glb # Traffic vehicle GLB asset
|-- assets/
|   `-- passengers/
|       |-- ATTRIBUTION.txt
|       `-- passenger.glb            # Bundled CesiumMan character (CC-BY 4.0)
|-- textures/                        # Extracted embedded GLTF textures
|   |-- gltf_embedded_0.png ...
|   `-- gltf_embedded_8.png
|-- src/main/java/game
|   |-- Main.java
|   |-- core                         # Shared object contracts
|   |   |-- GameObject.java
|   |   |-- Renderable.java
|   |   `-- Updatable.java
|   |-- engine
|   |   |-- Camera.java
|   |   |-- Font.java
|   |   |-- Mesh.java
|   |   |-- Renderer.java
|   |   |-- ShaderProgram.java
|   |   |-- Texture.java
|   |   |-- Transform.java
|   |   `-- Window.java
|   |-- gameplay
|   |   |-- CityMap.java
|   |   |-- CollisionSystem.java
|   |   |-- GameState.java
|   |   |-- HUD.java
|   |   |-- MiniMap.java
|   |   |-- Passenger.java
|   |   |-- PlayerController.java
|   |   `-- TrafficCar.java
|   |-- scene
|   |   |-- Entity.java
|   |   `-- ModelLoader.java
|   `-- scoring
|       |-- ScoreEntry.java
|       |-- Scoreboard.java
|       `-- ScoreboardManager.java
|-- src/main/resources/shaders
|   |-- fragment.glsl
|   |-- menu_fragment.glsl
|   |-- menu_vertex.glsl
|   `-- vertex.glsl
`-- target/  (generated build output, do not edit)
```

## Architecture Overview

`game.Main` is the application coordinator. It creates the window, renderer, camera, game state, gameplay systems, models, entities, input callbacks, and main loop.

The code is layered so higher layers depend only on lower ones:

```mermaid
graph TD
    subgraph ui
        Menu
    end
    subgraph gameplay
        CityMap
        CollisionSystem
        GameState
        HUD
        MiniMap
        Passenger
        PlayerController
        TrafficCar
    end
    subgraph scene
        Entity
        ModelLoader
    end
    subgraph engine
        Camera
        Font
        Mesh
        Renderer
        ShaderProgram
        Texture
        Transform
        Window
    end
    U --> gameplay
    gameplay --> scene
    scene --> engine

    click U "src/main/java/game/ui/Menu.java"
    click HUD "src/main/java/game/gameplay/HUD.java"
    click Renderer "src/main/java/game/engine/Renderer.java"
```

## Runtime Flow

```mermaid
sequenceDiagram
    participant Main
    participant Window
    participant ModelLoader
    participant CityMap
    participant GameState
    participant HUD as Renderer/HUD

    Main->>Window: create 1280x720 GLFW window
    Main->>Model: load city & vehicle & passenger GLBs
    Model-->>Main: mesh lists
    Main->>Main: normalize scale/origin
    Main->>CityMap: extract road samples, calc bounds
    CityMap-->>Main: routes, passenger locations
    Main->>Main: build traffic cars & passengers
    loop main loop
        Main->>GameState: update current screen
        GameState-->>Main: tick timer/lives/score
        Main->>HUD: render 3D world, then 2D overlays
    end
    Main->>Window: swap buffers
```

## Game State Machine

The top-level screen flow is driven by `GameState.GameScreen`:

```mermaid
stateDiagram-v2
    [*] --> MENU
    MENU --> PLAYING : Enter / start
    MENU --> MENU : Quit / Esc
    PLAYING --> GAME_OVER : timer = 0 or lives = 0
    PLAYING --> MENU : Esc
```

Scores earned in `PLAYING` are persisted through the `scoring` package (`ScoreboardManager`).

## Passenger State Machine

Each passenger follows a pickup/drop-off lifecycle:

```mermaid
stateDiagram-v2
    [*] --> AVAILABLE
    AVAILABLE --> CARRIED : in pickupRadius
    CARRIED --> DELIVERED : in dropoffRadius
    DELIVERED --> [*]
```

- Pickup and drop-off radii are `3.0f` world units in `Passenger.java`.

## Bundled Assets

The project ships the following GLB models:

| Asset | Path | Size | Role | Attribution |
| --- | --- | --- | --- | --- |
| City map | `source/burnin_rubber_crash_n_burn_city.glb` | ~18 MB | City/map world geometry | Burnin' Rubber providers |
| Player vehicle | `1982_toyota_hiace_combi.glb` | ~34 MB | Player car | Original asset author |
| Traffic vehicle | `bmw_m4_competition_m_package.glb` | ~23 MB | Traffic car mesh clones | Original asset author |
| Passenger | `assets/passengers/passenger.glb` | ~0.5 MB | Pickup/drop-off character | Cesium (CC-BY 4.0) |

Embedded textures extracted from models at build time land in `textures/` (`gltf_embedded_0.png ... gltf_embedded_8.png`).

Also bundled: `assets/passengers/ATTRIBUTION.txt` documenting the Cesium passenger model license.

## Quick Start

From the repository root:

```bash
mvn package && java -jar target/city-racer-new-variation-ref-1.0.jar
```

During the first Maven build, dependencies may need to be downloaded from Maven Central. The shaded jar is configured with `game.Main` as the entry point.

## How to Play

Objective:

- Drive through the city.
- Find passengers at green pickup markers.
- Drive to the active passenger's yellow drop-off marker.
- Avoid traffic cars.
- Keep the car within the city bounds.
- Deliver as many passengers as possible before the timer expires or lives reach zero.

Controls:

| Action | Keys |
| --- | --- |
| Accelerate | `W` or `Up Arrow` |
| Brake / reverse | `S` or `Down Arrow` |
| Turn left | `A` or `Left Arrow` |
| Turn right | `D` or `Right Arrow` |
| Navigate menu | `Up Arrow`, `Down Arrow` |
| Select menu item | `Enter` |
| Return to menu / close instructions | `Esc` |

Rules:

- Each delivered passenger is worth 100 points.
- The timer starts at five minutes and the player has three lives.
- Hitting traffic costs one life and returns the player to the spawn point.
- The game ends when the timer reaches zero or lives reach zero.

HUD:

- Top left: score. Top center: remaining time. Top right: speed in km/h.
- Bottom left: lives. Left side: delivery count.
- Center notification: nearest passenger or current drop-off objective.
- Minimap: road samples, traffic, player direction, passengers, traffic, spawn point.
- Main menu exposes a scoreboard of the top five scores (via `ScoreboardManager`).

## Build and Run Commands

Compile:

```bash
mvn compile
```

Package a runnable shaded jar:

```bash
mvn package
```

Run the packaged game:

```bash
java -jar target/city-racer-1.0.jar
```

Run from compiled classes with Maven-managed dependencies:

```bash
mvn exec:java -Dexec.mainClass=game.Main
```

The project does not currently define tests. If tests are added later, run them with:

```bash
mvn test
```

## Core Systems

### Main Loop

The main loop in `Main.loop()` calculates `deltaTime`, polls window events, starts a new render frame, updates the active screen, and swaps buffers. `deltaTime` is capped to `0.05f` seconds to keep physics stable.

```java
float deltaTime = (now - lastTime) / 1_000_000_000.0f;
if (deltaTime > 0.05f) deltaTime = 0.05f;
```

### Screen State

`GameState.GameScreen` has three states: `MENU`, `PLAYING`, `GAME_OVER`. The menu handles start, instructions, scoreboard, and quit.

### Player Movement

`PlayerController` implements arcade driving (acceleration `12.0f`, max speed `30.0f`, reverse limit `40%`, braking `8.0f`, turn speed `2.5f`, friction `4.0f`). The camera follows behind and above the car with smoothing.

### Traffic

Traffic cars are clones of the traffic model (`bmw_m4_competition_m_package.glb`) assigned generated routes. Speed is randomized (`4.0f + Math.random() * 4.0f`) and count is capped at eight:

```java
int trafficCount = Math.min(8, carMeshes.size() * 8);
```

### Collisions

- City bounds: player position clamped to calculated bounds.
- Buildings: solid mesh edges push the player away.
- Traffic: contact within two car radii costs a life and respawns the player. A two-second crash cooldown prevents repeat collisions.

## Asset and Model Pipeline

The project uses five packaged GLB assets. `pom.xml` includes `*.glb` and `*.gltf` resources. `ModelLoader.resolveModelPath()` searches: the given path, the working directory, parent project directories, and packaged resources (copied to a temp file for Assimp).

### City Model Processing

`Main.createCityScene()` normalizes bounds, shifts the center, estimates ground height from road-ish meshes (`street`, `parking`, `crosses`, `bridge`, `tunnel`), and scales to gameplay space. Collision and invisible meshes are skipped for rendering.

### Car Model Processing

`Main.createPlayerCar()` centers the mesh origin, seats the bottom at ground level, rescales the length to about `4.5f` units, and applies a `PI` yaw offset so the nose faces forward.

## Rendering and Shaders

- 3D world shader: `src/main/resources/shaders/vertex.glsl` + `fragment.glsl`.
- 2D UI/menu shader: `src/main/resources/shaders/menu_vertex.glsl` + `menu_fragment.glsl`.

3D rendering uses depth testing, opaque-then-transparent ordering, and a simple directional light with ambient, diffuse, and specular components. 2D rendering uses an orthographic 1280x720 projection for text, rectangles, lines, and diamonds. `Renderer` falls back to placeholder blocks when no system font is found.

## Gameplay Tuning Guide

| Goal | File | Values to edit |
| --- | --- | --- |
| Change game time | `src/main/java/game/gameplay/GameState.java` | `timeLimit` |
| Change player lives | `src/main/java/game/gameplay/GameState.java` | `lives`, `startGame()` |
| Change score per delivery | `src/main/java/game/gameplay/GameState.java` | `deliverPassenger()` |
| Change car physics | `src/main/java/game/gameplay/PlayerController.java` | `acceleration`, `maxSpeed`, `braking`, `turnSpeed`, `friction` |
| Change pickup/drop-off radius | `src/main/java/game/gameplay/Passenger.java` | `pickupRadius`, `dropoffRadius` |
| Change traffic count/speed | `src/main/java/game/Main.java` | `trafficCount`, speed in `createTraffic()` |
| Change collision size | `src/main/java/game/gameplay/CollisionSystem.java` | `carCollisionRadius`, `buildingCollisionRadius` |
| Change crash cooldown | `src/main/java/game/Main.java` | `CRASH_COOLDOWN_SECONDS` |
| Change camera sampling | `src/main/java/game/engine/Camera.java` | `distance`, `height`, `lookHeight` |
| Change HUD/minimap layout | `src/main/java/game/gameplay/HUD.java`, `MiniMap.java` | draw coordinates and colors |
| Change menu text | `src/main/java/game/ui/Menu.java` | menu/instruction strings |

## Adding or Changing Content

### Replace a Vehicle

1. Drop a new `.glb`/`.gltf` in the repository root.
2. Update `CAR_MODEL_PATH` / `TRAFFIC_CAR_MODEL_PATH` in `Main.java`.
3. `mvn package`, then check appearance, orientation (`CAR_MODEL_YAW_OFFSET`), and scale/roof placement.

### Replace the City Map

1. Add the city `.glb`/`.gltf`.
2. Update `CITY_MODEL_PATH` in `Main.java`.
3. Name road meshes with road-like words (`street`, `parking`, `crosses`, `bridge`, `tunnel`) and buildings with `building`.
4. Watch startup logs for loaded mesh count, bounds, and road sample count.

### Add a Passenger

Edit `CityMap.generateRoadSamplePassengerLocations()` (or `generatePassengerLocations()` if no road samples). Each passenger needs pickup/drop-off `Vector2f` coordinates plus a name.

### Add Traffic Routes

Routes are lists of `Vector2f` waypoints produced by `CityMap.generateRoutes()` / `CityMap.generateRoadSampleRoutes()`. `TrafficCar` loops waypoints and wraps around.

### Add a New HUD Element

1. Add data to `GameState`.
2. Pass it to `HUD.render(...)`.
3. Draw via `renderer.drawText`, `renderer.drawRect`, `renderer.drawLine`, or `renderer.drawDiamond`.

### Add a New Shader Uniform

1. Declare it in the GLSL shader.
2. Add a setter call in `Renderer`, with a helper in `ShaderProgram` if needed.

## Troubleshooting

### `Unsupported class file major version` or compile failure

Install JDK 21+ or lower `maven.compiler.source/target` in `pom.xml`.

### LWJGL native library errors

This project targets `natives-linux`. Change the `lwjgl.natives` property for your OS.

### Window does not open

Check for a desktop display session, OpenGL 3.3 Core support, and that GLFW is well-initialized; headless or SSH sessions usually cannot open the window.

### Models do not load

The `mvn package` run must include the GLB/GLTF resources. Verify `CAR_MODEL_PATH`, `CITY_MODEL_PATH`, `TRAFFIC_CAR_MODEL_PATH`, `PASSENGER_MODEL_PATH` match file paths. Startup logs show whether Assimp reported an import error. The project falls back to placeholder geometry.

### Car spawns in a strange place

Spawn placement depends on detected road geometry. Ensure road meshes are named with road-like words and kept mostly horizontal.

### Building collisions do not work

`CollisionSystem.isSolidCityMesh()` only treats meshes whose name contains `building` as solid. Rename meshes or adjust the predicate.

### Road height is wrong

Road height sampling relies on road-like mesh names and horizontal triangles. Avoid marking road meshes `collision` or `invisible` for some sampling paths.

### Text appears as blocks

The system font loader could not find a font. Install a common font from `Renderer`'s path list or update the list.

## Development Notes

- No GUI console required at runtime (window renders for the game), but the run environment needs OpenGL + GLFW.
- The codebase currently has no automated test suite; `mvn test` is safe to run but exits with no tests.
- Window size is fixed at 1280x720; HUD/menu/minimap coordinates are hard-coded for it.
- This is a small custom engine, not a full framework.
- Build outputs go to `target/`; `dependency-reduced-pom.xml` is generated by the Shade plugin.
- Score persistence currently writes to `scoreboard.txt`.
- Scores also exist in `scoring/` (`Scoreboard.java` central manager).

## Suggested Next Improvements

- Add Maven profiles for Linux, Windows, and macOS LWJGL natives.
- Add a resize-aware renderer and HUD layout.
- Add sound effects for pickup, delivery, collision, and menu navigation.
- Add unit tests for `CityMap`, `Passenger`, `GameState`, routes, and scoring.
- Add debug toggles for road samples, city bounds, collision bounds, and route lines.
- Add a pause screen instead of returning straight to the menu on `Esc`.
- Credit the non-Cesium assets authors in `assets/` next to their files.

## Related Documents

- `MARIA_DESIGN_DIAGRAM.md` — proposed relational (MariaDB) schema mapping the game's runtime design.