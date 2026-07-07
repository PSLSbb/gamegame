package game.gameplay;

/**
 * Backward-compatible alias for older code/tabs.
 *
 * New code should import game.scoring.ScoreEntry.
 */
@Deprecated
public class ScoreEntry extends game.scoring.ScoreEntry {
    public ScoreEntry(String playerName, int score, float timeElapsed) {
        super(playerName, score, timeElapsed);
    }
}
