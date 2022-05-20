package com.tj6200.autocraft;

import com.tj6200.autocraft.helpers.Utils;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class LogHandler {

    private JavaPlugin plugin;

    public LogHandler (JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void debugLog(String message, List<Player> players) {
        for (Player player: players) {
            Utils.sendMessageToPlayer(player, message);
        }
    }

    public void log(String message) { plugin.getLogger().info(message); }

    public void warning(String message) { plugin.getLogger().warning(message); }

}
