# Taxi Game OOP Reference Architecture

`new variation ref` is still the real LWJGL taxi game. The OOP teaching
architecture is integrated into the game source instead of replacing the game
with a console-only demo.

## 5-Minute Class Diagram

- `game.Main` owns the high-level game loop.
- `game.core.Updatable` defines `update(float dt)`.
- `game.core.Renderable` defines `render(Renderer, Camera, Vector3f)`.
- `game.core.GameObject` is the abstract parent for world objects.
- `game.scene.Entity` extends `GameObject` and represents a renderable mesh.
- `game.gameplay.PlayerController`, `TrafficCar`, `GameState`, and `game.ui.Menu`
  implement `Updatable`.
- `game.gameplay.Passenger` owns a clear passenger state machine.
- `game.scoring.Scoreboard`, `ScoreboardManager`, and `ScoreEntry` own score
  persistence. The old `game.gameplay` score files remain only as tiny
  backward-compatible aliases.

## The 4 OOP Pillars In This Game

1. Abstraction
   - `Updatable` lets the game loop update different systems through one method.
   - `Renderable` lets renderable world objects expose one rendering contract.
   - `Main.updateObject(...)` accepts any `Updatable`.

2. Inheritance
   - `GameObject` stores shared object identity: id, display name, and active state.
   - `Entity` extends `GameObject`, so city meshes, car parts, traffic parts, and
     passenger models all share one parent type.

3. Polymorphism
   - `Menu`, `GameState`, `PlayerController`, and `TrafficCar` all implement
     `update(float dt)` differently.
   - `Main` can call `updateObject(something, dt)` without caring which concrete
     class it received.
   - `Entity.render(...)` overrides the `Renderable` contract with mesh rendering.

4. Encapsulation
   - `Entity` no longer exposes fields like `transform`, `color`, or `type`
     directly.
   - Other files now use methods such as `getTransform()`, `getColor()`,
     `getType()`, `setPosition(...)`, and `setColor(...)`.
   - `Passenger` keeps passenger state private and exposes behavior methods like
     `pickUp()`, `deliver()`, `isAvailable()`, and `getDirectionToDestination(...)`.

## Important Files

- `src/main/java/game/core/Updatable.java`
- `src/main/java/game/core/Renderable.java`
- `src/main/java/game/core/GameObject.java`
- `src/main/java/game/scene/Entity.java`
- `src/main/java/game/gameplay/PlayerController.java`
- `src/main/java/game/gameplay/TrafficCar.java`
- `src/main/java/game/gameplay/Passenger.java`
- `src/main/java/game/gameplay/GameState.java`
- `src/main/java/game/scoring/Scoreboard.java`
- `src/main/java/game/scoring/ScoreboardManager.java`
- `src/main/java/game/scoring/ScoreEntry.java`
- `src/main/java/game/ui/Menu.java`
- `src/main/java/game/Main.java`

## How To Explain It To A Teacher

The project starts in `Main`, which runs the game loop. The loop updates objects
through the `Updatable` interface, showing abstraction and polymorphism.
Renderable world meshes are `Entity` objects, and `Entity` inherits shared id,
name, and active-state behavior from `GameObject`, showing inheritance. Entity
state is private, so the rest of the game must communicate through methods,
showing encapsulation.

The result is still the original 3D taxi game, but its key classes now have a
clear textbook OOP structure that is easier to explain.
