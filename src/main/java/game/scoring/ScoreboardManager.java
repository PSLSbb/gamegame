package game.scoring;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ScoreboardManager {
    private static final int MAX_HIGH_SCORES = 5;
    private static final String FIELD_SEPARATOR = ",";

    private final Path scoreFile;

    public ScoreboardManager() {
        this(Path.of("scoreboard.txt"));
    }

    public ScoreboardManager(Path scoreFile) {
        this.scoreFile = scoreFile;
    }

    public List<ScoreEntry> loadTopScores() {
        List<ScoreEntry> scores = new ArrayList<>();

        if (!Files.exists(scoreFile)) {
            return scores;
        }

        try (BufferedReader reader = Files.newBufferedReader(scoreFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                ScoreEntry entry = parseLine(line);
                if (entry != null) {
                    scores.add(entry);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not load scoreboard: " + e.getMessage());
        }

        return topFive(scores);
    }

    public void addScore(ScoreEntry newEntry) {
        if (newEntry == null) {
            return;
        }

        List<ScoreEntry> scores = loadTopScores();
        scores.add(newEntry);
        saveTopScores(scores);
    }

    public void saveTopScores(List<ScoreEntry> scores) {
        List<ScoreEntry> topScores = topFive(scores);

        try {
            Path parent = scoreFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            System.err.println("Could not create scoreboard folder: " + e.getMessage());
            return;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(scoreFile)) {
            for (ScoreEntry entry : topScores) {
                writer.write(formatLine(entry));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Could not save scoreboard: " + e.getMessage());
        }
    }

    private List<ScoreEntry> topFive(List<ScoreEntry> scores) {
        List<ScoreEntry> sortedScores = new ArrayList<>();
        if (scores != null) {
            sortedScores.addAll(scores);
        }

        sortedScores.sort(
            Comparator.comparingInt(ScoreEntry::getScore).reversed()
                .thenComparing(ScoreEntry::getTimeElapsed)
        );

        if (sortedScores.size() > MAX_HIGH_SCORES) {
            return new ArrayList<>(sortedScores.subList(0, MAX_HIGH_SCORES));
        }
        return sortedScores;
    }

    private ScoreEntry parseLine(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }

        String[] parts = line.split(FIELD_SEPARATOR, -1);
        if (parts.length != 3) {
            return null;
        }

        try {
            String playerName = parts[0].trim();
            int score = Integer.parseInt(parts[1].trim());
            float timeElapsed = Float.parseFloat(parts[2].trim());
            return new ScoreEntry(playerName, score, timeElapsed);
        } catch (NumberFormatException e) {
            System.err.println("Skipping invalid scoreboard row: " + line);
            return null;
        }
    }

    private String formatLine(ScoreEntry entry) {
        String safeName = sanitizePlayerName(entry.getPlayerName());
        return safeName + FIELD_SEPARATOR + entry.getScore() + FIELD_SEPARATOR + entry.getTimeElapsed();
    }

    private String sanitizePlayerName(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return "Player";
        }
        return playerName.replace(FIELD_SEPARATOR, " ").trim();
    }
}
