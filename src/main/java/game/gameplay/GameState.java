package game.gameplay;

import game.core.Updatable;

public class GameState implements Updatable {
    public enum GameScreen {
        MENU,
        PLAYING,
        GAME_OVER
    }

    private GameScreen screen = GameScreen.MENU;
    private int score = 0;
    private int passengersDelivered = 0;
    private int passengersTotal = 0;
    private float gameTime = 0;
    private float timeLimit = 300.0f; // 5 minutes
    private boolean hasPassenger = false;
    private String passengerDestination = "";
    private int lives = 3;

    // Menu state
    private int menuSelection = 0;
    private static final String[] MENU_ITEMS = {
        "START GAME",
        "HOW TO PLAY",
        "QUIT"
    };
    private boolean showInstructions = false;

    public GameScreen getScreen() { return screen; }
    public void setScreen(GameScreen screen) { this.screen = screen; }

    public int getScore() { return score; }
    public void addScore(int points) {
        this.score += points;
    }

    public int getPassengersDelivered() { return passengersDelivered; }
    public int getPassengersTotal() { return passengersTotal; }

    public void deliverPassenger() {
        passengersDelivered++;
        passengersTotal++;
        hasPassenger = false;
        addScore(100);
    }

    public void pickupPassenger(String destination) {
        hasPassenger = true;
        passengerDestination = destination;
    }

    public boolean hasPassenger() { return hasPassenger; }
    public String getPassengerDestination() { return passengerDestination; }

    public float getGameTime() { return gameTime; }
    public float getTimeLimit() { return timeLimit; }
    public float getTimeRemaining() { return Math.max(0, timeLimit - gameTime); }

    public void update(float deltaTime) {
        if (screen == GameScreen.PLAYING) {
            gameTime += deltaTime;
            if (gameTime >= timeLimit) {
                screen = GameScreen.GAME_OVER;
            }
        }
    }

    public int getLives() { return lives; }
    public void loseLife() {
        lives--;
        if (lives <= 0) {
            screen = GameScreen.GAME_OVER;
        }
    }

    public int getMenuSelection() { return menuSelection; }
    public void menuUp() { menuSelection = (menuSelection - 1 + MENU_ITEMS.length) % MENU_ITEMS.length; }
    public void menuDown() { menuSelection = (menuSelection + 1) % MENU_ITEMS.length; }
    public String getSelectedMenuItem() { return MENU_ITEMS[menuSelection]; }
    public String[] getMenuItems() { return MENU_ITEMS; }

    public boolean isShowInstructions() { return showInstructions; }
    public void setShowInstructions(boolean show) { showInstructions = show; }

    public void startGame() {
        screen = GameScreen.PLAYING;
        score = 0;
        passengersDelivered = 0;
        gameTime = 0;
        hasPassenger = false;
        lives = 3;
        menuSelection = 0;
    }

    public boolean isGameOver() {
        return screen == GameScreen.GAME_OVER;
    }
}
