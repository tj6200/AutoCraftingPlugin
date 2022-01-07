package com.tj6200.autocraft.api;

import com.google.gson.JsonObject;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.*;

import java.util.ArrayList;
import java.util.List;

import com.tj6200.autocraft.*;

public class AutoCrafter {

    private static final String worldJsonPropStr = "world";
    private static final String blockPosJsonPropStr = "position";
    private static final String itemFramePosJsonPropStr = "item frame position";

    public Block block;
    public Block itemFrame;
    public Chunk dispenserChunk;
    public Chunk itemFrameChunk;
    public ItemStack item;
    public List<CraftingRecipe> recipes = new ArrayList<>();

    private boolean isLoaded = false;
    private boolean isBroken = false;
    private Dispenser dispenser;
    public Block destinationBlock;
    private BlockState destination;
    public CraftingTask task;

    private void init(Block block, Block itemFrame, ItemStack item) {
        setBlock(block);
        setItemFrameBlock(itemFrame);
        this.task = new CraftingTask(this);
        isBroken = false;
        isLoaded = true;
        updateStates();
        setItem(item);
    }

    public AutoCrafter(Block block, Block itemFrame, ItemStack item){
        init(block, itemFrame, item);
    }

    public AutoCrafter(JsonObject json) {
        String worldKeyString = json.get(worldJsonPropStr).getAsString();
        World world = Bukkit.getWorld(worldKeyString);

        JsonObject position = json.getAsJsonObject(blockPosJsonPropStr);

        int x = position.get("x").getAsInt();
        int y = position.get("y").getAsInt();
        int z = position.get("z").getAsInt();

        Location blockLocation = new Location(world, x, y, z);
        Block block = world.getBlockAt(blockLocation);
        BlockState state = block.getState();
        if (!(state instanceof Dispenser)) {
            AutoCraft.LOGGER.log("Block is not a dispenser.");
            this.breakCrafter();
            throw new IllegalArgumentException();
        }
        setBlock(block);

        position = json.getAsJsonObject(itemFramePosJsonPropStr);
        x = position.get("x").getAsInt();
        y = position.get("y").getAsInt();
        z = position.get("z").getAsInt();

        blockLocation = new Location(world, x, y, z);
        block = world.getBlockAt(blockLocation);
        setItemFrameBlock(block);
    }

    public void setBlock(Block block) {
        this.block = block;
        BlockFace targetFace = ((org.bukkit.block.data.type.Dispenser) block.getBlockData()).getFacing();
        this.destinationBlock = new Location(block.getWorld(),
                block.getX() + targetFace.getModX(),
                block.getY() + targetFace.getModY(),
                block.getZ() + targetFace.getModZ()).getBlock();
        this.dispenserChunk = block.getChunk();
    }

    public void setItemFrameBlock(Block block) {
        this.itemFrame = block;
        this.itemFrameChunk = block.getChunk();
    }

    private void getRecipesFor(ItemStack item) {
        recipes = RecipeHandler.getRecipesFor(item);
    }

    public boolean setItem(ItemStack item) {
        isBroken = false;
        BlockState state = block.getState();
        ((Nameable) state).setCustomName("Autocrafter");
        state.update();
        getRecipesFor(item);
        if (recipes.size() == 0) {
            item = null;
            this.breakCrafter();
            return false;
        }
        this.run();
        return true;
    }

    public void breakCrafter() {
        isBroken = true;
        item = null;
        BlockState state = block.getState();
        ((Nameable) state).setCustomName(null);
        state.update();
    }

    public boolean isBroken() {
        return isBroken;
    }
    public boolean isLoaded() { return isLoaded; }

    public void load(List<Entity> entities) {
        if (isLoaded) {
            return;
        }

        for(Entity entity: entities) {
            if (!(entity instanceof ItemFrame)) {
                continue;
            }
            ItemFrame itemFrame = (ItemFrame) entity;
            Block itemFrameBlock = itemFrame.getLocation().getBlock();
            if (!itemFrameBlock.equals(this.itemFrame)) {
                continue;
            }

            ItemStack item = itemFrame.getItem();
            if (item.getType().equals(Material.AIR) || item == null) {
                continue;
            }
            this.setItem(item);
            this.isLoaded = true;
            AutoCraft.LOGGER.log("AutoCrafter was loaded");

            return;
        }

        this.breakCrafter();
    }

    private void updateStates() {
        dispenser = (Dispenser) this.block.getState();
        destination = destinationBlock.getState();
    }

    private boolean bukkitCraftItem() {
        Inventory inv = dispenser.getInventory();
        InventoryHolder destInv = (InventoryHolder) destination;

        for (CraftingRecipe recipe: recipes) {
            if (!recipe.containsRequirements(inv)) {
                continue;
            }
            CraftSolution solution = recipe.findSolution(inv);

            List<ItemStack> items = solution.getContainerItems();
            items.add(recipe.getResultDrop());

            if (!AutoCraft.addItemsIfCan(destInv.getInventory(), items)) {
                continue;
            }
            solution.applyTo(inv);

            if (AutoCraft.particles)
                for (Location loc : AutoCraft.getHollowCube(block.getLocation(), 0.05))
                    loc.getWorld().spawnParticle(Particle.REDSTONE, loc, 2, 0, 0, 0, 0,
                            new Particle.DustOptions(Color.LIME, 0.2F));

            return true;
        }
        return false;
    }

    /**
     * Main method that updates the autocrafter
     *
     **/

    public boolean handle() {
        if (isBroken || !isLoaded) {
            return false;
        }

        if (!dispenserChunk.isLoaded()) {
            return false;
        }

        updateStates();

        if (destination instanceof InventoryHolder) {
            return bukkitCraftItem();
        }

        return false;
    }

    public void run() {
        if (task != null) {
            return;
        }
        task = new CraftingTask(this);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    public JsonObject toJSON() {
        JsonObject json = new JsonObject();
        World world = block.getWorld();
        json.addProperty(worldJsonPropStr, world.getName());

        JsonObject position = new JsonObject();
        position.addProperty("x", block.getX());
        position.addProperty("y", block.getY());
        position.addProperty("z", block.getZ());

        json.add(blockPosJsonPropStr, position);

        position = new JsonObject();
        position.addProperty("x", itemFrame.getX());
        position.addProperty("y", itemFrame.getY());
        position.addProperty("z", itemFrame.getZ());

        json.add(itemFramePosJsonPropStr, position);

        return json;
    }
}
