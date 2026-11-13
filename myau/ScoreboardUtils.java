package myau;

import java.util.ArrayList;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;

public final class ScoreboardUtils {

    private static final Minecraft minecraft = Minecraft.func_71410_x();

    private ScoreboardUtils() {
    }

    public static ArrayList<String> sidebarLines() {
        if (minecraft.field_71441_e == null) {
            return new ArrayList<String>();
        }
        Scoreboard scoreboard = minecraft.field_71441_e.func_96441_U();
        ScoreObjective objective = scoreboard.func_96539_a(1);
        if (objective == null) {
            return new ArrayList<String>();
        }
        return scoreboard.func_96534_i(objective).stream().map(score -> formatScoreboardLine(scoreboard, score)).collect(Collectors.toCollection(ArrayList::new));
    }

    public static boolean isHypixelScoreboard() {
        ArrayList<String> lines = sidebarLines();
        if (lines.isEmpty()) {
            return false;
        }
        String firstLine = lines.get(0);
        return firstLine.equals("\u00a7ewww.hypixel.ne\ud83c\udf82\u00a7et")
                || firstLine.equals("\u00a7ewww.hypixel.ne\u00a7g\u00a7et");
    }

    public static boolean hasLobbyPlayerCount() {
        return sidebarLines().stream().anyMatch(line -> line.matches(".*Players: \u00a7a\\d+/\\d+.*"));
    }

    private static String formatScoreboardLine(Scoreboard scoreboard, Score score) {
        return ScorePlayerTeam.func_96667_a((Team) scoreboard.func_96509_i(score.func_96653_e()), (String) score.func_96653_e());
    }
}
