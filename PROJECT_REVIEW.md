# City Racer — Project Review & Improvement Plan

*Reviewed: 2026-08-07*

---

## 1. What This Project Is (in Simple English)

**City Racer** is a small 3D driving game written in **Java** that you control with
a keyboard. You drive a delivery van around a city, pick up passengers waiting at
green markers, drop them off at yellow markers, earn points, and try to do as many
deliveries as you can before either the 5-minute timer runs out or you run out of
your 3 lives (which happens when you crash into traffic or other cars).

Under the hood it's built on **LWJGL 3** (Java bindings for OpenGL, GLFW for the
window, Assimp for loading 3D models, STB for images), **JOML** for math, and a
**small hand-written game engine** that the project authors wrote themselves —
there is no Unity/Unreal-style framework here. Everything from the render loop, the
camera, the HUD, and the minimap to the traffic simulation is custom code.

The city and vehicles are real 3D models loaded from `.glb` files (a Burnin' Rubber
city map, a Toyota Hiace van for the player, a BMW M4 for traffic, and a passenger
model). If a model fails to load, the game can fall back to simple generated box
geometry so it still runs.

The code is written as a **teaching project**: it demonstrates the classic OOP
pillars (abstraction, inheritance, polymorphism, encapsulation) through a
`GameObject` hierarchy, `Updatable`/`Renderable` interfaces, and a state machine on
the `Passenger` class. A separate `OOP_REFACTOR_GUIDE.md` explains all of that for
students/teachers.

### Key Numbers
- ~5,600 lines of Java across ~25 source files.
- `Main.java` is 1,770 lines (very large — see "Concerns").
- 4 `.glb` model files, ~80 MB of assets total.
- No test files, no CI for the game, no sound, no save/load besides `scoreboard.txt`.

---

## 2. Scorecard (out of 10)

| Metric | Score | Notes |
|---|---|---|
| **Visuals & Rendering** | 5.5 | Functional custom 3D renderer (lighting, textures, GLB models) but basic: one light, no shadows, no post-processing. |
| **Gameplay & Fun** | 6 | Solid arcade loop (pickup → deliver → avoid traffic) and decent tuning, but shallow: no difficulty curve, no variety. |
| **Code Organization / OOP** | 7 | Clean package layout, good OOP teaching structure, but `Main` is a god-class. |
| **Maintainability** | 5.5 | Readable and well-commented, but `Main.java` does everything and has zero tests. |
| **Performance** | 6 | Fine for a small scene (road-height index, entity limiting), but no culling/frustum and per-room traffic updates. |
| **Robustness / Error handling** | 7.5 | Strong: fallback geometry if models fail, delta-time capping, crash cooldown, defensive null checks. |
| **Documentation** | 9 | Excellent README with architecture overview, tuning guide, troubleshooting, and an OOP guide. |
| **Cross-platform & Build** | 3.5 | Targets Java 26 and hard-codes `natives-linux`; no Maven platform profiles; cannot currently build in this environment (JDK 17). |
| **Testing & CI** | 1.5 | No test suite at all; CI only drives chat automation, not the build. |
| **Polish / UX** | 5 | No sound, no pause menu, no resizing, no gamepad; minimap is fully written but **never rendered**. |
| **Asset licensing** | 4 | Assets are large, partly unlicensed, and mixed in the repo root. |
| **Overall** | **6.0 / 10** | A solid, well-documented teaching/arcade demo with a real codebase and clear room to grow. |

---

## What the Code Does Well

- **Excellent documentation.** The `README.md` is better than most real project
  READMEs: quick start, controls, architecture overview, a tuning table so you can
  tweak the game without hunting through code, and troubleshooting.
- **OOP done visibly.** `GameObject` → `Entity`, `Updatable`/`Renderable`
  interfaces, and Passenger's `SPAWNED → WAITING → HAILING → BOARDED → EXITED`
  state machine are textbook-quality examples.
- **Degrading gracefully.** If models, fonts, or textures are missing, the game
  falls back instead of crashing. That is genuinely good engineering for a demo.
- **Self-contained custom engine.** No frameworks; you can read and modify every
  layer (window → renderer → entity → gameplay).
- **Nice arcade feel decisions.** Third-person chase camera with smoothing, delta
  time capped at 50 ms, two-second crash invulnerability, road-height snapping so
  cars follow the terrain.

---

## Concerns & Findings

1. **`Main.java` is a god class.** 1,770 lines handle loading, normalization,
   collision setup, spawn-finding, model processing, fallbacks, input, the game
   loop, HUD markers, and score persistence. This is the single biggest
   maintainability problem.
2. **The minimap is dead code.** `MiniMap.java` (192 lines) is fully implemented
   but never instantiated or rendered from `Main`. The README even claims the game
   has a minimap, but it is not visible in game.
3. **No tests, at all.** Math-heavy logic (route tracing, spawn finding, passenger
   state machine, collision resolution) has zero automated tests.
4. **Build portability.** `pom.xml` uses `maven.compiler.source/target = 26` and
   `natives-linux`. There's no Maven profile switching natives per OS. The repo
   can't compile on this machine (only JDK 17 available).
5. **Duplicated/legacy score classes.** `game.gameplay.ScoreEntry` and
   `game.gameplay.ScoreboardManager` are deprecated aliases that duplicate
   `game.scoring.*` — confusing for newcomers.
6. **Polish gaps.** No sound, no pause (ESC silently aborts the run and goes to
   menu, losing progress), no window resize (1280x720 fixed, all HUD positions
   hard-coded), text falls back to block rectangles if no font is found.
7. **Hard-coded magic numbers.** Tuning values are scattered constants in `Main`
   and gameplay classes rather than a config object — the README's tuning table
   documents them, but a `GameConfig` would be cleaner.
8. **Asset bloat & licensing.** ~68 MB of `/ *.glb*` and textures sit in the repo
   root; some have no attribution/ license files (the passenger asset is aborted
   but the others aren't attributed in-repo).

---

## 3. Improvement Plan

### Phase 0 — Hygiene & Documentation (few hours, high value)
- [ ] **Wire up the minimap** in `renderScene()` (or delete `MiniMap.java`). The
  code is ready; it just needs `new MiniMap(renderer)` + a `render(...)` call.
- [ ] **Remove deprecated duplicate classes** (`game.gameplay.ScoreEntry`,
  `game.gameplay.ScoreboardManager`) and update imports to `game.scoring.*`.
- [ ] **Add Maven profiles** for `natives-windows`, `natives-macos`,
  `natives-linux`, and make the JDK requirement explicit in the `README`.
- [ ] Rotate `README` minimap feature claims to match reality.

### Phase 1 — Make It Buildable & Testable (1–2 days)
- [ ] Add a **basic test suite** (JUnit 5): unit-test `Passenger` state machine,
  `CityMap` route generation, `CollisionSystem` (bounds + wall resolve),
  `GameState` scoring, and `ScoreboardManager` CSV round-trip.
- [ ] Add headless-safe "logic" unit tests that don't require OpenGL (all the
  above are pure math/IO — perfect to test).
- [ ] Add CI step (`mvn test`) to the `borevibe` workflow or a new build workflow.

### Phase 2 — Refactor the God Class (2–4 days)
- [ ] Split `Main.java` into a proper architecture:
  - `GameApp`/`Main` — window, loop, wiring only (~200 lines).
  - `AssetManager` — model loading/normalization/fallbacks.
  - `GameplayDirector` — screen state, passenger/traffic orchestration.
  - `SpawnFinder` — spawn sampling/preview logic (currently ~150 lines).
  - `RoadHeightMap` — the current triangle grid + sampling code.
- [ ] Extract tuning constants into a config object or `gameplay.Config`.

### Phase 3 — Game Feel & Polish (1–2 weeks)
- [ ] **Sound** (simple: pickup/drop-off/collision hits + engine hum) using LWJGL
  OpenAL; keep asset-free by generating tones or shipping small WAVs.
- [ ] **Pause menu** (`Esc` → "Resume / Restart / Quit") instead of abandoning.
- [ ] **Resizable window** + HUD/annhop layout that adapts (at minimum, computed
  from width/height).
- [ ] **UI text rendering** — bundle a font (e.g., fallback to a small TTF) so it
  never renders as blocks. Add font baking that pads the atlas.
- [ ] **Progressive difficulty** — increase traffic count/speed over time; add
  delivery combos for late-game.

### Phase 4 — Beyond the Demo (weeks)
- [ ] Shadows (simple shadow map), scope light/point lights, and a vignette/fog
  for atmosphere.
- [ ] Better collision: AABB broad-phase over the triangle loop per frame.
- [ ] Gamepad input + rebindable keymaps.
- [ ] `Options` screen (sensitivity, volume, difficulty).
- [ ] Level-loading from external city GLB, plus a "next city" progression.
- Key **asset tags**: add standardized `CREDITS`/licensing into the README and
  split asset subject files into `assets/*`.
- [ ] Store high scores in the user's home dir (not the working directory as
  `scoreboard.txt`).

### Priority Ranking (quick wins first)
1. Wire minimap (dead feature) — profit
2. Add unit tests — lowest cost, highest long-term value
3. Maven natives/JDK profiles — portability
4. Delete deprecated duplicates
5. Sound + pause
6. Refactor `Main.java`
7. Resizable window
8. Difficulty curve / graphics upgrades

---

*This file was produced from a code reading of the repository at
`gamegame` (src/main/java + resources on 2026-08-07).*