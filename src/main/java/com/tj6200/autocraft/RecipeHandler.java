package com.tj6200.autocraft;

import com.destroystokyo.paper.Namespaced;
import com.google.common.collect.Multimap;
import com.tj6200.autocraft.api.CraftingRecipe;
import com.tj6200.autocraft.api.RecipeType;
import com.tj6200.autocraft.helpers.Utils;
import com.tj6200.autocraft.helpers.recipes.BukkitRecipe;
import com.tj6200.autocraft.helpers.recipes.FireworksRecipe;
import com.tj6200.autocraft.helpers.recipes.SuspicousStewRecipe;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.tags.CustomItemTagContainer;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class RecipeHandler {

    public static ArrayList<CraftingRecipe> recipes = new ArrayList<>();

    public RecipeHandler() { }

    public static List<CraftingRecipe> getRecipesFor(final ItemStack item) {
        return recipes.stream().filter(r -> r.creates(item)).collect(Collectors.toList());
    }

    public static boolean addToRecipes(CraftingRecipe newRecipe) {
        for(CraftingRecipe recipe: recipes) {
            if (recipe.getKey() == newRecipe.getKey()) {
                return false;
            }
        }
        recipes.add(newRecipe);
        return true;
    }

    public static boolean addToRecipes(Recipe recipe) {
        BukkitRecipe bukkitRecipe = new BukkitRecipe(recipe);
        if (bukkitRecipe.getType() == RecipeType.UNKNOWN) {
            return false;
        }
        return addToRecipes(bukkitRecipe);
    }

    /**
     * This method returns an ArrayList of all available recipes (including extra)
     *
     **/

    public static void collectRecipes() {

        recipes.clear();

        Iterator<Recipe> it = Bukkit.recipeIterator();
        while (it.hasNext()) {
            Recipe recipe = it.next();
            if (recipe instanceof Keyed) {
                addToRecipes(recipe);
            }
        }

        // Custom recipes
        addToRecipes(new FireworksRecipe(NamespacedKey.minecraft("fireworks_duration_1"), 1));
        addToRecipes(new FireworksRecipe(NamespacedKey.minecraft("fireworks_duration_2"), 2));
        addToRecipes(new FireworksRecipe(NamespacedKey.minecraft("fireworks_duration_3"), 3));

        addCustomRecipes();

        for (var ingredient : SuspicousStewRecipe.INGREDIENTS.keySet()) {
            addToRecipes(new SuspicousStewRecipe(NamespacedKey.minecraft("suspicious_stew_" + ingredient.name().toLowerCase()), ingredient));
        }

        AutoCraft.LOGGER.log("Loaded recipes: " + recipes.size() + " recipes..");
    }

    // Will use in a future version
    private static void addCustomRecipes() {

    }

    public static CraftingRecipe getRecipeFor(NamespacedKey key) {
        for(CraftingRecipe recipe: recipes) {
            if (recipe.getKey().equals(key)) {
                return recipe;
            }
        }
        return null;
    }
}
