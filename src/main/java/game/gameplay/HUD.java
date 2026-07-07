package game.gameplay;

import game.engine.Renderer;

public class HUD {
    private final Renderer renderer;
    private final GameState state;

    public HUD(Renderer renderer, GameState state) {
        this.renderer = renderer;
        this.state = state;
    }

    public void render(float playerSpeed, String passengerInfo) {
        // Top bar background
        renderer.drawRect(0, 0, 1280, 50, 0.1f, 0.1f, 0.2f, 0.8f);

        // Score
        renderer.drawText(20, 12, 0.5f,
            "SCORE: " + state.getScore(), 1.0f, 1.0f, 0.3f, 1.0f);

        // Timer
        int minutes = (int)(state.getTimeRemaining() / 60);
        int seconds = (int)(state.getTimeRemaining() % 60);
        String timeStr = String.format("TIME: %02d:%02d", minutes, seconds);
        float timeX = 640 - (timeStr.length() * 7);
        renderer.drawText(timeX, 12, 0.5f, timeStr, 1.0f, 1.0f, 1.0f, 1.0f);

        // Speed
        String speedStr = String.format("SPEED: %d km/h", (int)(playerSpeed * 3.6f));
        renderer.drawText(1100, 12, 0.5f, speedStr, 0.3f, 0.8f, 1.0f, 1.0f);

        // Lives
        renderer.drawText(20, 655, 0.5f,
            "LIVES: " + state.getLives(), 1.0f, 0.3f, 0.3f, 1.0f);

        // Passenger info
        if (passengerInfo != null && !passengerInfo.isEmpty()) {
            renderer.drawRect(400, 70, 480, 35, 0.1f, 0.3f, 0.1f, 0.8f);
            renderer.drawText(415, 76, 0.45f, passengerInfo, 0.8f, 1.0f, 0.8f, 1.0f);
        }

        // Passengers delivered counter
        renderer.drawText(20, 258, 0.4f,
            "DELIVERIES: " + state.getPassengersDelivered(), 0.8f, 1.0f, 0.8f, 1.0f);
    }
}
