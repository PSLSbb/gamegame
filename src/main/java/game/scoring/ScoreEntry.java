package game.scoring;

public class ScoreEntry {
    private String playerName;
    private int score;
    private float timeElapsed;

    public ScoreEntry(String playerName, int score, float timeElapsed) {
        this.playerName = playerName;
        this.score = score;
        this.timeElapsed = timeElapsed;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public float getTimeElapsed() {
        return timeElapsed;
    }

    public void setTimeElapsed(float timeElapsed) {
        this.timeElapsed = timeElapsed;
    }
}
