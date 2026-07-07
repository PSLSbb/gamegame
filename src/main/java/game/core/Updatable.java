package game.core;

/**
 * ABSTRACTION PILLAR:
 * Anything that changes over time can promise to implement update(dt).
 *
 * The GameManager does not need to know whether the object is a Taxi,
 * Passenger, or TrafficCar. It only needs to know that it is Updatable.
 */
public interface Updatable {
    void update(float dt);
}
