package com.tj6200.autocraft;

import com.tj6200.autocraft.helpers.Utils;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;

public class LogHandler {

    private JavaPlugin plugin;
    private ArrayList<Player> debugPlayers;

    public LogHandler (JavaPlugin plugin) {
        this.plugin = plugin;
        this.debugPlayers = new ArrayList<>();
    }

    public void addDebugPlayer(Player player) {
        debugPlayers.add(player);
    }

    public void removeDebugPlayer(Player player) {
        if (debugPlayers.contains(player)) {
            debugPlayers.remove(player);
        }
    }

    public void debugLog(String message) {
        for (Player player: debugPlayers) {
            Utils.sendMessageToPlayer(player, message);
        }
    }

    public void log(String message) { plugin.getLogger().info(message); }

    public void warning(String message) { plugin.getLogger().warning(message); }

}
