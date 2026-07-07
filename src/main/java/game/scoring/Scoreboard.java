package game.scoring;

import java.nio.file.Path;

/**
 * Small class-diagram friendly name for the scoreboard system.
 *
 * ScoreboardManager keeps the file parsing/saving implementation, while this
 * class gives the game a simple "Scoreboard" type to point at in presentations.
 */
public class Scoreboard extends ScoreboardManager {
    public Scoreboard() {
        super();
    }

    public Scoreboard(Path scoreFile) {
        super(scoreFile);
    }
}
