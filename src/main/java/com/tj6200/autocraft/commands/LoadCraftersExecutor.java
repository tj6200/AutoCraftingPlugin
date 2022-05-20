package com.tj6200.autocraft.commands;

import com.tj6200.autocraft.AutoCraft;
import com.tj6200.autocraft.api.AutoCrafter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LoadCraftersExecutor implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return false;
        }
        for(AutoCrafter autoCrafter: AutoCraft.autoCrafters) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("inChunk")) {
                    if (autoCrafter.getChunkKey() != player.getChunk().getChunkKey() ||
                            autoCrafter.getWorld() != player.getWorld()) {
                        continue;
                    }
                }
            }
            autoCrafter.tryToLoad();
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
