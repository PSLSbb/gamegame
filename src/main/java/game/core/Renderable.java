package game.core;

import org.joml.Vector3f;

import game.engine.Camera;
import game.engine.Renderer;

/**
 * ABSTRACTION PILLAR:
 * Anything that can be drawn on screen can promise to implement render().
 *
 * The real 3D game needs a Renderer, Camera, and camera position to draw a
 * mesh correctly, so those details are passed in through the interface.
 */
public interface Renderable {
    void render(Renderer renderer, Camera camera, Vector3f viewPosition);
}
