package com.tj6200.autocraft.helpers.recipes.requirements;

import com.tj6200.autocraft.api.CraftSolution;
import com.tj6200.autocraft.api.Pair;
import com.tj6200.autocraft.helpers.Utils;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RequirementSolution implements CraftSolution {

    private final List<Pair<RecipeRequirement, ItemStack>> history = new ArrayList<>();
    private final List<ItemStack> containerItems = new ArrayList<>();
    private final ItemStack[] state;

    public RequirementSolution(Inventory base) {
        state = new ItemStack[base.getStorageContents().length];
        for (int j = 0; j < state.length; j++) {
            // Build clones so we don't affect the original inventory
            if (base.getStorageContents()[j] != null)
                state[j] = base.getStorageContents()[j].clone();
        }
    }

    private RequirementSolution(RequirementSolution old) {
        // Copy the history
        history.addAll(old.history);

        // Copy the state
        state = new ItemStack[old.state.length];
        for (int j = 0; j < state.length; j++) {
            // Build clones so we don't affect the original state
            if (old.state[j] != null)
                state[j] = old.state[j].clone();
        }

        // Copy the container items
        // We don't clone them to save on performance as we never modify them
        // anyways, we only use the ones from the final solution even.
        containerItems.addAll(old.containerItems);
    }

    /**
     * Get the 'container item' which is the item
     * left in the crafting area after an item is used
     * in a crafting recipe.
     * <p>
     * Returns null if nothing/air is the container item.
     */
    private static ItemStack getContainerItem(Material input, int amount) {
        var remainingItem = input.getCraftingRemainingItem();
        if (remainingItem == null) return null;
        return new ItemStack(remainingItem, amount);
    }

    @Override
    public List<ItemStack> getContainerItems() {
        return containerItems;
    }

    /**
     * Returns a new permutation of this solution with the given requirement's solution of item.
     */
    public RequirementSolution addRequirement(RecipeRequirement requirement, ItemStack item) {
        var newSol = new RequirementSolution(this);
        newSol.history.add(new Pair<>(requirement, item));

        // We need to take the amount of times we need this requirement times the amount in the requirement
        int amountToFind = requirement.amount * item.getAmount();

        // Go through the state of the new requirement and remove the items
        for (int j = 0; j < newSol.state.length; j++) {
            var it = newSol.state[j];

            // If this item is similar we start taking it away
            if (Utils.isSimilar(item, it)) {
                int cap = Math.min(it.getAmount(), amountToFind);
                if (it.getAmount() - cap <= 0) newSol.state[j] = null;
                else it.setAmount(it.getAmount() - cap);
                amountToFind -= cap;

                // If we're taking any items we see if that leaves a container item
                if (cap >= 0) {
                    var containerItem = getContainerItem(item.getType(), cap);
                    if (containerItem != null) {
                        newSol.containerItems.add(containerItem);
                    }
                }

                // When we've taken all the items that need taking we're done
                if (amountToFind <= 0) break;
            }
        }

        return newSol;
    }

    /**
     * Tests if this solution has the items required to fulfill the given variant of the requirement.
     */
    public boolean test(RecipeRequirement requirement, ItemStack item) {
        // We need to find the amount of times we need this requirement times the amount in the requirement
        int amountToFind = requirement.amount * item.getAmount();

        for (ItemStack it : state) {
            //If any item in our array of valid items is similar to this item we have found our match.
            if (Utils.isSimilar(item, it)) {
                amountToFind -= Math.min(it.getAmount(), amountToFind);

                //If we have at least the amount of any valid item in this inventory we call it good.
                if (amountToFind <= 0)
                    return true;
            }
        }

        return false;
    }

    /**
     * Returns the amount of items that were used for this solution.
     */
    public int getCost() {
        return history.stream().mapToInt(it -> it.getKey().amount * it.getValue().getAmount()).sum();
    }

    @Override
    public void applyTo(Inventory inv) {
        for (int i = 0; i < state.length; i++) {
            inv.setItem(i, state[i]);
        }
    }

    @Override
    public String toString() {
        return "RequirementSolution{" +
                "history=" + history +
                ", containerItems=" + containerItems +
                ", state=" + Arrays.toString(state) +
                '}';
    }
}
