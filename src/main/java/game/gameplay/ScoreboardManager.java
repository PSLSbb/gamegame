package game.gameplay;

import java.nio.file.Path;

/**
 * Backward-compatible alias for older code/tabs.
 *
 * New code should use game.scoring.Scoreboard or game.scoring.ScoreboardManager.
 */
@Deprecated
public class ScoreboardManager extends game.scoring.ScoreboardManager {
    public ScoreboardManager() {
        super();
    }

    public ScoreboardManager(Path scoreFile) {
        super(scoreFile);
    }
}
