package game.ui;

import java.util.List;

import game.engine.Renderer;
import game.core.Updatable;
import game.gameplay.GameState;
import game.scoring.ScoreEntry;

public class Menu implements Updatable {
    private final Renderer renderer;
    private final GameState state;
    private float pulseTimer = 0;
    private static final float SCREEN_CENTER_X = 640.0f;
    private static final float MENU_BUTTON_WIDTH = 360.0f;
    private static final float MENU_BUTTON_HEIGHT = 54.0f;
    private static final float MENU_BUTTON_GAP = 18.0f;
    private static final float MENU_BUTTON_X = SCREEN_CENTER_X - MENU_BUTTON_WIDTH * 0.5f;
    private static final float MENU_TEXT_SCALE = 0.58f;

    public Menu(Renderer renderer, GameState state) {
        this.renderer = renderer;
        this.state = state;
    }

    public void update(float deltaTime) {
        pulseTimer += deltaTime;
    }

    public void render() {
        if (state.isShowInstructions()) {
            renderInstructions();
        } else {
            renderMainMenu();
        }
    }

    private void renderMainMenu() {
        // Clean full-screen backdrop for the menu.
        renderer.drawRect(0, 0, 1280, 720, 0.05f, 0.05f, 0.15f, 1.0f);

        drawCenteredText("CITY RACER", 110.0f, 1.15f, 0.0f, 0.0f, 0.0f, 0.55f);
        drawCenteredText("CITY RACER", 106.0f, 1.15f, 1.0f, 0.8f, 0.2f, 1.0f);

        float pulse = (float) (Math.sin(pulseTimer * 3.0f) * 0.3f + 0.7f);
        String[] items = state.getMenuItems();
        int selection = state.getMenuSelection();
        float startY = 246.0f;

        for (int i = 0; i < items.length; i++) {
            float y = startY + i * (MENU_BUTTON_HEIGHT + MENU_BUTTON_GAP);
            drawMenuButton(items[i], y, i == selection, pulse);
        }
    }

    private void drawMenuButton(String label, float y, boolean selected, float pulse) {
        float alpha = selected ? 0.55f + pulse * 0.18f : 0.28f;
        float r = selected ? 0.23f : 0.12f;
        float g = selected ? 0.42f : 0.16f;
        float b = selected ? 0.72f : 0.24f;
        renderer.drawRect(MENU_BUTTON_X, y, MENU_BUTTON_WIDTH, MENU_BUTTON_HEIGHT, r, g, b, alpha);

        if (selected) {
            renderer.drawRect(MENU_BUTTON_X, y, 5.0f, MENU_BUTTON_HEIGHT, 1.0f, 0.8f, 0.2f, 0.9f);
            renderer.drawRect(
                MENU_BUTTON_X + MENU_BUTTON_WIDTH - 5.0f,
                y,
                5.0f,
                MENU_BUTTON_HEIGHT,
                1.0f,
                0.8f,
                0.2f,
                0.9f
            );
        }

        float textWidth = renderer.measureTextWidth(label, MENU_TEXT_SCALE);
        float textX = SCREEN_CENTER_X - textWidth * 0.5f;
        float textY = y + 12.0f;
        float textColor = selected ? 1.0f : 0.74f;
        renderer.drawText(textX, textY, MENU_TEXT_SCALE, label, textColor, textColor, textColor, 1.0f);
    }

    private void drawCenteredText(String text, float y, float scale, float r, float g, float b, float a) {
        float textX = SCREEN_CENTER_X - renderer.measureTextWidth(text, scale) * 0.5f;
        renderer.drawText(textX, y, scale, text, r, g, b, a);
    }

    private void renderInstructions() {
        renderer.drawRect(0, 0, 1280, 720, 0.05f, 0.05f, 0.15f, 1.0f);

        renderer.drawText(310, 50, 1.0f, "HOW TO PLAY", 1.0f, 0.8f, 0.2f, 1.0f);

        String[] instructions = {
            "OBJECTIVE:",
            "Drive around the city, find passengers,",
            "and deliver them to their destinations!",
            "",
            "CONTROLS:",
            "W / Up Arrow    - Accelerate forward",
            "S / Down Arrow  - Brake / Reverse",
            "A / Left Arrow  - Turn Left",
            "D / Right Arrow - Turn Right",
            "ENTER           - Select menu items",
            "ESC             - Back to menu",
            "",
            "GAMEPLAY:",
            "Green markers show passenger pickup locations",
            "Yellow markers show destination drop-off points",
            "Avoid traffic cars and stay within the city!",
            "Deliver passengers to earn points.",
            "",
            "Press ESC to return"
        };

        float y = 120;
        for (String line : instructions) {
            if (line.isEmpty()) {
                y += 10;
                continue;
            }
            boolean isHeader = line.endsWith(":") && !line.startsWith(" ");
            float r = isHeader ? 1.0f : 0.8f;
            float g = isHeader ? 0.8f : 0.8f;
            float b = isHeader ? 0.2f : 0.9f;
            float size = isHeader ? 0.45f : 0.38f;
            float x = 640 - (line.length() * 4);
            renderer.drawText(x, y, size, line, r, g, b, 1.0f);
            y += isHeader ? 30 : 22;
        }
    }

    public void renderGameOver(List<ScoreEntry> highScores, String playerNameInput, boolean scoreSaved) {
        renderer.drawRect(0, 0, 1280, 720, 0.0f, 0.0f, 0.0f, 0.8f);

        renderer.drawText(480, 120, 1.3f, "GAME OVER", 1.0f, 0.2f, 0.2f, 1.0f);

        String scoreStr = "FINAL SCORE: " + state.getScore();
        renderer.drawText(495, 220, 0.7f, scoreStr, 1.0f, 1.0f, 0.3f, 1.0f);

        String deliveriesStr = "PASSENGERS DELIVERED: " + state.getPassengersDelivered();
        renderer.drawText(470, 270, 0.5f, deliveriesStr, 0.8f, 1.0f, 0.8f, 1.0f);

        renderPreviousHighScore(highScores);
        renderNamePrompt(playerNameInput, scoreSaved);
        renderHighScores(highScores);

        if (state.getMenuSelection() == 0) {
            renderer.drawRect(403, 590, 474, 38, 0.3f, 0.5f, 0.8f, 0.5f);
        }
        renderer.drawText(420, 595, 0.6f, "ENTER - SAVE SCORE", 1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void renderPreviousHighScore(List<ScoreEntry> highScores) {
        if (highScores == null || highScores.isEmpty()) {
            renderer.drawText(470, 325, 0.45f, "PREVIOUS HIGH SCORE: none", 0.8f, 0.8f, 0.9f, 1.0f);
            return;
        }

        ScoreEntry best = highScores.get(0);
        String line = "PREVIOUS HIGH SCORE: " + best.getPlayerName() + " - " + best.getScore();
        renderer.drawText(420, 325, 0.45f, line, 1.0f, 0.8f, 0.2f, 1.0f);
    }

    private void renderNamePrompt(String playerNameInput, boolean scoreSaved) {
        if (scoreSaved) {
            renderer.drawText(515, 365, 0.45f, "Score saved.", 0.8f, 1.0f, 0.8f, 1.0f);
            return;
        }

        String typedName = playerNameInput == null || playerNameInput.isBlank() ? "_" : playerNameInput + "_";
        renderer.drawText(430, 365, 0.45f, "TYPE YOUR NAME: " + typedName, 0.85f, 1.0f, 0.85f, 1.0f);
    }

    private void renderHighScores(List<ScoreEntry> highScores) {
        renderer.drawText(530, 420, 0.55f, "TOP SCORES", 1.0f, 0.8f, 0.2f, 1.0f);

        if (highScores == null || highScores.isEmpty()) {
            renderer.drawText(500, 460, 0.4f, "No scores saved yet", 0.8f, 0.8f, 0.9f, 1.0f);
            return;
        }

        int limit = Math.min(5, highScores.size());
        for (int i = 0; i < limit; i++) {
            ScoreEntry entry = highScores.get(i);
            String line = String.format("%d. %s  %d pts  %s",
                i + 1,
                entry.getPlayerName(),
                entry.getScore(),
                formatTime(entry.getTimeElapsed())
            );
            renderer.drawText(435, 460 + i * 24, 0.36f, line, 0.85f, 0.9f, 1.0f, 1.0f);
        }
    }

    private String formatTime(float secondsElapsed) {
        int totalSeconds = Math.max(0, (int) secondsElapsed);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
