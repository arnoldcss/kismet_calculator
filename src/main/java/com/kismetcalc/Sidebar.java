package com.kismetcalc;

import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

//hopefully works
public final class Sidebar {

    private Sidebar() {
    }

    public static List<String> lines() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return List.of();

        Scoreboard scoreboard = mc.level.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) return List.of();

        List<PlayerScoreEntry> entries = new ArrayList<>(scoreboard.listPlayerScores(objective));
        
        entries.sort(Comparator.comparingInt(PlayerScoreEntry::value).reversed());

        List<String> lines = new ArrayList<>(entries.size());
        for (PlayerScoreEntry entry : entries) {
            if (entry.isHidden()) continue;


            String owner = entry.ownerName().getString();
            PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
            String text = team == null
                ? owner
                : team.getPlayerPrefix().getString()
                    + (strip(owner).isEmpty() ? "" : owner)
                    + team.getPlayerSuffix().getString();
            lines.add(strip(text).trim());
        }
        return lines;
    }

    
    public static String strip(String text) {
        if (text == null) return "";
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                i++;
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }
}
