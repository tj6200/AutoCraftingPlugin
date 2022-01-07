package com.tj6200.autocraft.helpers.recipes.requirements;

import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class RecipeRequirement {
    private final Collection<ItemStack> item;
    public final int amount;

    public RecipeRequirement(Collection<ItemStack> items, int amount) {
        this.item = items;
        this.amount = amount;
    }

    /**
     * Returns whether this recipe requirement is invalid or not. Invalid requirements should not be used ever.
     */
    public boolean isInvalid() {
        return item == null || item.isEmpty() || amount < 1;
    }

    /**
     * Returns a list of all solutions that can validate this requirement.
     */
    public List<RequirementSolution> getSolutions(Collection<RequirementSolution> solutions) {
        return solutions.stream().flatMap(base -> item.stream().filter(it -> base.test(this, it)).map(it -> base.addRequirement(this, it))).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "RecipeRequirement{" +
                "item=" + item +
                ", amount=" + amount +
                '}';
    }
}
