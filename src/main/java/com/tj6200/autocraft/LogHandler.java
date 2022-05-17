package com.tj6200.autocraft;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class LogHandler {

    public JavaPlugin plugin;

    public LogHandler (JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void log(String message) { plugin.getLogger().info(message); }

    public void warning(String message) { plugin.getLogger().warning(message); }
}
