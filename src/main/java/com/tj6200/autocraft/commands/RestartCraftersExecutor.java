package com.tj6200.autocraft.commands;

import com.tj6200.autocraft.AutoCraft;
import com.tj6200.autocraft.api.AutoCrafter;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class RestartCraftersExecutor implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return false;
        }
        Chunk chunk = null;
        if (args.length > 0 && args[0].equalsIgnoreCase("inChunk")) {
            chunk = player.getChunk();
        }
        for (AutoCrafter autoCrafter: AutoCraft.autoCrafters) {
            if (chunk != null &&
                    (chunk.getChunkKey() != autoCrafter.getChunkKey() ||
                            chunk.getWorld() != autoCrafter.getWorld())) {
                continue;
            }
            autoCrafter.restart();
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> list = new ArrayList<>();
        list.add("inChunk");
        return list;
    }
}
