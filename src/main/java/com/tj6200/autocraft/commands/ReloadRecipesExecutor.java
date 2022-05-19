package com.tj6200.autocraft.commands;

import com.tj6200.autocraft.RecipeHandler;
import com.tj6200.autocraft.helpers.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ReloadRecipesExecutor implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        RecipeHandler.collectRecipes();
        if (sender instanceof Player player) {
            Utils.sendActionBarMessageToPlayer(player, "Reloaded Recipes");
        }
        return true;
    }
}
