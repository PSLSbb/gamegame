package game.gameplay;

import java.util.Collections;
import java.util.List;

import org.joml.Vector2f;
import org.joml.Vector3f;

import game.scene.Entity;

/**
 * A self-contained passenger model for the taxi gameplay loop.
 *
 * The passenger owns its pickup position, destination position, current state,
 * and the small amount of navigation math needed to point the taxi toward the
 * destination. The rest of the game can simply call update(...) once per frame.
 */
public class Passenger {
    /**
     * The passenger's simple life cycle:
     * SPAWNED  - Created by the game, but not yet active in the world.
     * WAITING  - Standing at the pickup point.
     * HAILING  - Taxi is close enough that the passenger is trying to board.
     * BOARDED  - Passenger is inside the taxi, so their position follows it.
     * EXITED   - Passenger reached the destination and is done.
     */
    public enum PassengerState {
        SPAWNED,
        WAITING,
        HAILING,
        BOARDED,
        EXITED
    }

    // The passenger starts at this position and waits here until pickup.
    private final Vector3f pickupPosition;

    // Required gameplay field: current 3D world position of the passenger.
    private final Vector3f currentPosition;

    // Required gameplay field: final 3D world position where the taxi should go.
    private final Vector3f destinationPosition;

    // Required gameplay field: the current state in the passenger state machine.
    private PassengerState state = PassengerState.SPAWNED;

    private final String name;
    private final String destinationName;

    // Tuning values. These are deliberately simple and readable for beginners.
    private float hailDistance = 8.0f;
    private float boardingDistance = 2.0f;
    private float destinationArrivalDistance = 6.0f;
    private float stoppedSpeedThreshold = 0.35f;

    // Legacy 2D radii used by older game code. They map onto the new state logic.
    private float pickupRadius = 5.0f;
    private float dropoffRadius = 6.0f;

    // Previous taxi position lets this class estimate whether the taxi has stopped.
    // Speed = distance moved / time. If that value is tiny, the taxi is stopped.
    private final Vector3f previousTaxiPosition = new Vector3f();
    private boolean hasPreviousTaxiPosition = false;

    // Optional visible models/markers owned by Main's rendering code.
    private List<Entity> pickupEntities = Collections.emptyList();
    private List<Entity> dropoffEntities = Collections.emptyList();

    public Passenger(Vector3f pickupPosition, Vector3f destinationPosition) {
        this(pickupPosition, destinationPosition, "Passenger", "Destination");
    }

    public Passenger(Vector3f pickupPosition, Vector3f destinationPosition, String name) {
        this(pickupPosition, destinationPosition, name, "Destination");
    }

    public Passenger(Vector3f pickupPosition, Vector3f destinationPosition, String name, String destinationName) {
        this.pickupPosition = copyOrZero(pickupPosition);
        this.currentPosition = new Vector3f(this.pickupPosition);
        this.destinationPosition = copyOrZero(destinationPosition);
        this.name = name != null ? name : "Passenger";
        this.destinationName = destinationName != null ? destinationName : "Destination";
    }

    public Passenger(Vector2f pickupLocation, Vector2f dropoffLocation, String name) {
        this(pickupLocation, dropoffLocation, name, "Destination");
    }

    public Passenger(Vector2f pickupLocation, Vector2f dropoffLocation, String name, String destinationName) {
        this(toVector3f(pickupLocation), toVector3f(dropoffLocation), name, destinationName);
    }

    /**
     * Advances this passenger's state machine by one frame.
     *
     * The math here is basic vector math:
     * 1. Subtract one position from another to get the "offset" between them.
     * 2. Measure that offset's length to get distance.
     * 3. Compare the distance to a threshold to decide if something is close.
     */
    public void update(float deltaTime, Vector3f taxiPosition) {
        if (taxiPosition == null) {
            return;
        }

        boolean taxiStopped = isTaxiStopped(deltaTime, taxiPosition);

        switch (state) {
            case SPAWNED:
                // A spawned passenger becomes available automatically on the
                // first update. No player action is required for this transition.
                state = PassengerState.WAITING;
                currentPosition.set(pickupPosition);
                break;

            case WAITING:
                // Distance is the straight-line gap from taxi to passenger.
                // When the taxi enters the hail radius, the passenger starts hailing.
                if (distanceBetween(taxiPosition, pickupPosition) <= hailDistance) {
                    state = PassengerState.HAILING;
                }
                break;

            case HAILING:
                // Boarding is stricter than hailing: the taxi must be very close
                // and moving slowly enough that the passenger can safely get in.
                if (distanceBetween(taxiPosition, pickupPosition) <= boardingDistance && taxiStopped) {
                    boardTaxi(taxiPosition);
                } else if (distanceBetween(taxiPosition, pickupPosition) > hailDistance * 1.4f) {
                    // If the taxi drives away, return to waiting.
                    state = PassengerState.WAITING;
                }
                break;

            case BOARDED:
                // While boarded, the passenger's world position is locked to the
                // taxi position. This makes them move exactly with the taxi.
                currentPosition.set(taxiPosition);

                if (distanceBetween(taxiPosition, destinationPosition) <= destinationArrivalDistance) {
                    exitTaxi();
                }
                break;

            case EXITED:
                // Finished passengers no longer transition.
                break;
        }

        previousTaxiPosition.set(taxiPosition);
        hasPreviousTaxiPosition = true;
    }

    /**
     * Returns a normalized direction vector from the taxi to the destination.
     *
     * Example:
     * - taxi at (2, 0, 2)
     * - destination at (5, 0, 6)
     * - raw direction = destination - taxi = (3, 0, 4)
     * - length = 5
     * - normalized direction = (3/5, 0, 4/5) = (0.6, 0, 0.8)
     *
     * Normalizing keeps only the direction and makes the vector length equal 1.
     * That is useful for steering, arrows, and minimap indicators because the
     * direction does not get larger just because the destination is far away.
     */
    public Vector3f getDirectionToDestination(Vector3f currentTaxiPos) {
        if (currentTaxiPos == null || state == PassengerState.EXITED) {
            return new Vector3f();
        }

        // Subtraction answers: "How far must I move on X, Y, and Z to get there?"
        Vector3f direction = new Vector3f(destinationPosition).sub(currentTaxiPos);

        // lengthSquared avoids a square root. We only need to know if the vector
        // is almost zero before normalizing, because a zero vector has no direction.
        if (direction.lengthSquared() < 0.0001f) {
            return new Vector3f();
        }

        return direction.normalize();
    }

    public Vector3f getCurrentPosition() {
        return new Vector3f(currentPosition);
    }

    public Vector3f getDestinationPosition() {
        return new Vector3f(destinationPosition);
    }

    public PassengerState getState() {
        return state;
    }

    public String getName() {
        return name;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public boolean isPickedUp() {
        return state == PassengerState.BOARDED;
    }

    public boolean isDelivered() {
        return state == PassengerState.EXITED;
    }

    public boolean isAvailable() {
        return state == PassengerState.SPAWNED ||
               state == PassengerState.WAITING ||
               state == PassengerState.HAILING;
    }

    public void pickUp() {
        if (isAvailable()) {
            boardTaxi(currentPosition);
        }
    }

    public void deliver() {
        if (state == PassengerState.BOARDED) {
            exitTaxi();
        }
    }

    public Vector2f getPickupLocation() {
        return new Vector2f(pickupPosition.x, pickupPosition.z);
    }

    public Vector2f getDropoffLocation() {
        return new Vector2f(destinationPosition.x, destinationPosition.z);
    }

    public Vector3f getPickupPosition3D() {
        return new Vector3f(pickupPosition);
    }

    public Vector3f getDropoffPosition3D() {
        return new Vector3f(destinationPosition);
    }

    public void setVisualEntities(List<Entity> pickupEntities, List<Entity> dropoffEntities) {
        this.pickupEntities = pickupEntities != null ? pickupEntities : Collections.emptyList();
        this.dropoffEntities = dropoffEntities != null ? dropoffEntities : Collections.emptyList();
    }

    public List<Entity> getVisibleEntities() {
        if (state == PassengerState.EXITED) {
            return Collections.emptyList();
        }
        return state == PassengerState.BOARDED ? dropoffEntities : pickupEntities;
    }

    public boolean isInPickupRange(Vector2f playerPos) {
        if (playerPos == null || !isAvailable()) {
            return false;
        }
        return playerPos.distance(getPickupLocation()) < pickupRadius;
    }

    public boolean isInDropoffRange(Vector2f playerPos) {
        if (playerPos == null || state != PassengerState.BOARDED) {
            return false;
        }
        return playerPos.distance(getDropoffLocation()) < dropoffRadius;
    }

    public void setHailDistance(float hailDistance) {
        this.hailDistance = Math.max(0.0f, hailDistance);
    }

    public void setBoardingDistance(float boardingDistance) {
        this.boardingDistance = Math.max(0.0f, boardingDistance);
    }

    public void setDestinationArrivalDistance(float destinationArrivalDistance) {
        this.destinationArrivalDistance = Math.max(0.0f, destinationArrivalDistance);
        this.dropoffRadius = this.destinationArrivalDistance;
    }

    public void setStoppedSpeedThreshold(float stoppedSpeedThreshold) {
        this.stoppedSpeedThreshold = Math.max(0.0f, stoppedSpeedThreshold);
    }

    private void boardTaxi(Vector3f taxiPosition) {
        state = PassengerState.BOARDED;
        currentPosition.set(taxiPosition);
    }

    private void exitTaxi() {
        state = PassengerState.EXITED;
        currentPosition.set(destinationPosition);
    }

    private boolean isTaxiStopped(float deltaTime, Vector3f taxiPosition) {
        if (!hasPreviousTaxiPosition) {
            return false;
        }

        // distance = how far the taxi moved since last frame.
        float distanceMoved = distanceBetween(taxiPosition, previousTaxiPosition);

        // speed = distance / time. A small speed means the taxi is basically stopped.
        float safeDeltaTime = Math.max(deltaTime, 0.0001f);
        float estimatedSpeed = distanceMoved / safeDeltaTime;
        return estimatedSpeed <= stoppedSpeedThreshold;
    }

    private static float distanceBetween(Vector3f a, Vector3f b) {
        // Subtracting positions creates an offset vector:
        // (b.x - a.x, b.y - a.y, b.z - a.z).
        // The length of that offset is the straight-line distance between points.
        return new Vector3f(b).sub(a).length();
    }

    private static Vector3f copyOrZero(Vector3f value) {
        return value != null ? new Vector3f(value) : new Vector3f();
    }

    private static Vector3f toVector3f(Vector2f value) {
        if (value == null) {
            return new Vector3f();
        }
        return new Vector3f(value.x, 0.5f, value.y);
    }
}
