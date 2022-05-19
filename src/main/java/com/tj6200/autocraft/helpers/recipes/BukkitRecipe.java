package com.tj6200.autocraft.helpers.recipes;

import com.tj6200.autocraft.AutoCraft;
import com.tj6200.autocraft.api.CraftSolution;
import com.tj6200.autocraft.api.CraftingRecipe;
import com.tj6200.autocraft.api.RecipeType;
import com.tj6200.autocraft.helpers.ReflectionHelper;
import com.tj6200.autocraft.helpers.Utils;
import com.tj6200.autocraft.helpers.recipes.requirements.RecipeRequirement;
import com.tj6200.autocraft.helpers.recipes.requirements.RequirementSolution;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Build a recipe from item stacks in code.
 * These recipes have a prebuilt list of items they search for that
 * can be re-used for both testing if the items are contained in the
 * inventory as for taking them.
 */
public class BukkitRecipe implements CraftingRecipe {
    private static final Class<?> craftMetaBlockState = ReflectionHelper.getCraftBukkitClass("inventory.CraftMetaBlockState").orElse(null);
    private static final Field blockEntityTag = ReflectionHelper.getField(craftMetaBlockState, "blockEntityTag").orElse(null);
    private final ItemStack result;
    private NamespacedKey namespacedKey;
    private RecipeType type = RecipeType.UNKNOWN;
    private List<RecipeRequirement> requirements;
    //Shaped Recipes
    private String[] pattern;
    private Map<Character, Collection<ItemStack>> key;
    //Shapeless Recipes
    private Collection<Collection<ItemStack>> ingredients;

    public BukkitRecipe(NamespacedKey namespacedKey, ItemStack result, String[] pattern, Map<Character, Collection<ItemStack>> key) {
        this.namespacedKey = namespacedKey;
        this.type = RecipeType.SHAPED;
        this.result = result;
        this.pattern = pattern;
        this.key = key;
    }

    public BukkitRecipe(NamespacedKey namespacedKey, ItemStack result, List<Collection<ItemStack>> ingredients) {
        this.namespacedKey = namespacedKey;
        type = RecipeType.SHAPELESS;
        this.result = result;
        this.ingredients = ingredients;
    }

    /**
     * Build a recipe from a bukkit recipe.
     */
    public BukkitRecipe(Recipe bukkitRecipe) {
        namespacedKey = ((Keyed) bukkitRecipe).getKey();
        result = bukkitRecipe.getResult();
        if (bukkitRecipe instanceof ShapedRecipe) {
            type = RecipeType.SHAPED;
            pattern = ((ShapedRecipe) bukkitRecipe).getShape();
            key = new HashMap<>();

            Map<Character, RecipeChoice> choiceMap = ((ShapedRecipe) bukkitRecipe).getChoiceMap();
            choiceMap.forEach((k, v) -> {
                List<ItemStack> values = new ArrayList<>();
                if (v != null) { //V can be null for some reason.
                    if (v instanceof RecipeChoice.ExactChoice) {
                        values.addAll(((RecipeChoice.ExactChoice) v).getChoices());
                    } else if (v instanceof RecipeChoice.MaterialChoice) {
                        for (Material m : ((RecipeChoice.MaterialChoice) v).getChoices()) {
                            values.add(new ItemStack(m));
                        }
                    } else {
                        ItemStack val = v.getItemStack();
                        if (val != null) values.add(val);
                    }
                }
                key.put(k, values);
            });
        } else if (bukkitRecipe instanceof ShapelessRecipe) {
            type = RecipeType.SHAPELESS;
            ingredients = new ArrayList<>();

            List<RecipeChoice> choiceList = ((ShapelessRecipe) bukkitRecipe).getChoiceList();
            for (var v : choiceList) {
                List<ItemStack> values = new ArrayList<>();
                if (v != null) { //V can be null for some reason.
                    if (v instanceof RecipeChoice.ExactChoice) {
                        values.addAll(((RecipeChoice.ExactChoice) v).getChoices());
                    } else if (v instanceof RecipeChoice.MaterialChoice) {
                        for (Material m : ((RecipeChoice.MaterialChoice) v).getChoices()) {
                            values.add(new ItemStack(m));
                        }
                    } else {
                        ItemStack val = v.getItemStack();
                        if (val != null) values.add(val);
                    }
                }
                ingredients.add(values);
            }
        }
    }

    @Override
    public String toString() {
        return "BukkitRecipe{" +
                "type=" + type +
                ", result=" + result +
                ", requirements=" + requirements +
                ", pattern=" + Arrays.toString(pattern) +
                ", key=" + key +
                ", ingredients=" + ingredients +
                '}';
    }

    @Override
    public RecipeType getType() {
        return type;
    }

    /**
     * The requirements map for this recipe, can be cached.
     */
    private List<RecipeRequirement> getRequirements() {
        if (requirements == null) {
            requirements = new ArrayList<>();
            switch (type) {
                case SHAPED -> {
                    //Count how many times each character in the pattern occurrences
                    Map<Character, Integer> occurrences = new HashMap<>();
                    for (String s : pattern) {
                        for (char c : s.toCharArray()) {
                            occurrences.put(c, occurrences.getOrDefault(c, 0) + 1);
                        }
                    }

                    // Test if all characters in the pattern show up in the recipe
                    occurrences.forEach((c, i) -> {
                        if (!key.containsKey(c)) {
                            AutoCraft.LOGGER.warning("Warning shaped recipe with pattern [[" + String.join("], [", pattern) + "]] had character " + c + " in pattern but not in key map.");
                        }
                    });

                    // Put the corresponding item for each part of the shape into the requirements list,
                    // we multiply the requirement for the amount of times the character occurs in the pattern.
                    key.forEach((c, items) -> {
                        if (!occurrences.containsKey(c)) {
                            AutoCraft.LOGGER.warning("Warning shaped recipe with pattern [[" + String.join("], [", pattern) + "]] had key " + c + " in key map but not in pattern.");
                        }

                        requirements.add(new RecipeRequirement(items, occurrences.getOrDefault(c, 0)));
                    });
                }
                case SHAPELESS -> ingredients.forEach(i -> requirements.add(new RecipeRequirement(i, 1)));
            }

            // Remove empty requirements
            requirements.removeIf(RecipeRequirement::isInvalid);
        }
        return requirements;
    }

    @Override
    public boolean containsRequirements(Inventory inv) {
        var solutions = Collections.singletonList(new RequirementSolution(inv));
        for (var requirement : getRequirements()) {
            // Get all new permutations of the solutions with this new requirement
            solutions = requirement.getSolutions(solutions);

            // If there are ever no new solutions that means the requirement
            // cannot be fulfilled so the recipe is not possible
            if (solutions.isEmpty()) return false;
        }

        // If we didn't already return due to the there not being any solutions
        // there are valid ingredients present.
        return true;
    }

    @Override
    public NamespacedKey getKey() {
        return namespacedKey;
    }

    @Override
    public CraftSolution findSolution(Inventory inv) {
        var solutions = Collections.singletonList(new RequirementSolution(inv));
        for (var requirement : getRequirements()) {
            // Get all new permutations of the solutions with this new requirement
            solutions = requirement.getSolutions(solutions);

            // If there are no solutions [containsRequirements] should have failed.
            if (solutions.isEmpty()) {
                throw new UnsupportedOperationException("Recipe is being crafted without necessary materials, how?");
            }
        }

        // Get the cheapest solution or the only solution if there is one (which is the case in most recipes)
        return (solutions.size() == 1 ? solutions.get(0) :
                        solutions.stream().min(Comparator.comparing(RequirementSolution::getCost)).orElseThrow(() -> new UnsupportedOperationException("No solutions found, how?")));
    }

    @Override
    public boolean creates(ItemStack stack) {
        var clone = stack.clone();

        // For all block state meta items we clear the block entity tag off the item we use for comparisons
        // so a full shulker box is accepted as craftable
        if (clone.hasItemMeta() && clone.getItemMeta() instanceof BlockStateMeta meta) {
            if (blockEntityTag != null) {
                try {
                    blockEntityTag.set(meta, null);
                } catch (Exception x) {
                    x.printStackTrace();
                }
            }
            clone.setItemMeta(meta);
        }

        return Utils.isSimilar(result, clone);
    }

    @Override
    public ItemStack getResultDrop() {
        return result.clone();
    }
}
