package com.tj6200.autocraft.api;

import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.tj6200.autocraft.*;

public class AutoCrafter {

    private static final String worldJsonPropStr = "world";
    private static final String blockPosJsonPropStr = "position";
    private static final String itemFramePosJsonPropStr = "item frame position";

    public Block block;
    public Block itemFrame;
    private Chunk dispenserChunk;
    public Chunk itemFrameChunk;
    public ItemStack item;
    public List<CraftingRecipe> recipes = new ArrayList<>();
    public List<Player> debugPlayers = new ArrayList<>();

    private boolean isLoaded = false;
    private boolean isBroken = false;
    private Dispenser dispenser;
    public Block destinationBlock;
    public Block secondDestinationBlock;
    public BlockState destination;
    public CraftingTask task;

    private void init(Block block, Block itemFrame, ItemStack item) {
        this.setBlock(block);
        this.setItemFrameBlock(itemFrame);
        this.isBroken = false;
        this.isLoaded = true;
        this.updateStates();
        this.setItem(item);
    }

    public AutoCrafter(Block block, Block itemFrame, ItemStack item){
        this.init(block, itemFrame, item);
        this.run();
    }

    public AutoCrafter(JsonObject json) {
        String worldKeyString = json.get(worldJsonPropStr).getAsString();
        World world = Bukkit.getWorld(worldKeyString);
        if (world == null) {
            AutoCraft.LOGGER.log("World does not exist.");
            this.breakCrafter();
            return;
        }
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
            return;
        }
        this.setBlock(block);

        position = json.getAsJsonObject(itemFramePosJsonPropStr);
        x = position.get("x").getAsInt();
        y = position.get("y").getAsInt();
        z = position.get("z").getAsInt();

        blockLocation = new Location(world, x, y, z);
        block = world.getBlockAt(blockLocation);
        this.setItemFrameBlock(block);

        this.itemFrameChunk.addPluginChunkTicket(AutoCraft.INSTANCE);
        this.tryToLoad();
    }

    public void setBlock(Block block) {
        this.block = block;
        BlockFace targetFace = ((org.bukkit.block.data.type.Dispenser) block.getBlockData()).getFacing();
        this.destinationBlock = new Location(block.getWorld(),
                block.getX() + targetFace.getModX(),
                block.getY() + targetFace.getModY(),
                block.getZ() + targetFace.getModZ()).getBlock();
        this.secondDestinationBlock = new Location(block.getWorld(),
                block.getX() + targetFace.getModX() * 2,
                block.getY() + targetFace.getModY() * 2,
                block.getZ() + targetFace.getModZ() * 2).getBlock();
        this.dispenserChunk = block.getChunk();
    }

    public void setItemFrameBlock(Block block) {
        this.itemFrame = block;
        this.itemFrameChunk = block.getChunk();
    }

    private void getRecipesFor(ItemStack item) {
        this.recipes = RecipeHandler.getRecipesFor(item);
    }

    public void setItem(ItemStack item) {
        this.isBroken = false;
        this.isLoaded = true;
        BlockState state = block.getState();
        ((Nameable) state).customName(Component.text("Autocrafter"));
        state.update();
        this.getRecipesFor(item);
        if (this.recipes.size() == 0) {
            this.item = null;
            this.breakCrafter();
            return;
        }
        this.item = item;
        this.run();
    }

    public long getChunkKey() {
        return this.dispenserChunk.getChunkKey();
    }

    public World getWorld() {
        return this.dispenserChunk.getWorld();
    }
    public void addDebugPlayer(Player player) {
        if (!this.debugPlayers.contains(player)) {
            this.debugPlayers.add(player);
        }
    }

    public void removeDebugPlayer(Player player) {
        this.debugPlayers.remove(player);
    }

    public void breakCrafter() {
        this.isBroken = true;
        this.item = null;
        this.stop();
        BlockState state = block.getState();
        ((Nameable) state).customName(null);
        state.update();
    }

    public boolean isBroken() {
        return this.isBroken;
    }
    public boolean isLoaded() { return this.isLoaded; }

    public void load(List<Entity> entities) {
        if (this.isLoaded) {
            return;
        }
        for(Entity entity: entities) {
            if (!(entity instanceof ItemFrame itemFrame)) {
                continue;
            }
            Block itemFrameBlock = itemFrame.getLocation().getBlock();
            if (!itemFrameBlock.equals(this.itemFrame)) {
                continue;
            }

            ItemStack item = itemFrame.getItem();
            if (item.getType().equals(Material.AIR)) {
                continue;
            }
            this.setItem(item);
            this.itemFrameChunk.removePluginChunkTicket(AutoCraft.INSTANCE);
            AutoCraft.LOGGER.log(this + " was loaded.");
            AutoCraft.LOGGER.debugLog(this + " was loaded.", this.debugPlayers);
            this.run();
            return;
        }
        AutoCraft.LOGGER.log(this + " failed to load.");
        AutoCraft.LOGGER.debugLog(this + " failed to load.", this.debugPlayers);
    }

    public void unload() {
        this.item = null;
        this.recipes.clear();
        this.isLoaded = false;
        this.isBroken = false;
    }

    public void tryToLoad() {
        List<Entity> entities = Arrays.asList(this.itemFrameChunk.getEntities());
        this.load(entities);
    }

    private void updateStates() {
        if (!this.block.getType().equals(Material.DISPENSER)) {
            this.breakCrafter();
            return;
        }
        this.dispenser = (Dispenser) this.block.getState();
        this.destination = this.destinationBlock.getState();
    }

    private boolean bukkitCraftItem() {
        Inventory inv = this.dispenser.getInventory();
        InventoryHolder destInv = (InventoryHolder) destination;

        for (CraftingRecipe recipe: recipes) {
            if (!recipe.containsRequirements(inv)) {
                continue;
            }
            CraftSolution solution = recipe.findSolution(inv);

            List<ItemStack> items = solution.getContainerItems();
            items.add(recipe.getResultDrop());

            if (!AutoCraft.addItemsIfCan(destInv.getInventory(), items)) {
                AutoCraft.LOGGER.debugLog(this + " ? Destination inventory cannot hold items.", this.debugPlayers);
                continue;
            }
            solution.applyTo(inv);

            if (AutoCraft.particles) {
                for (Location loc : AutoCraft.getHollowCube(block.getLocation(), 0.05)) {
                    loc.getWorld().spawnParticle(Particle.REDSTONE, loc, 2, 0, 0, 0, 0,
                            new Particle.DustOptions(Color.LIME, 0.2F));
                }
            }

            return true;
        }
        return false;
    }

    /**
     * Main method that updates the autocrafter
     *
     **/

    public boolean handle() {
        if (this.isBroken) {
            AutoCraft.LOGGER.debugLog(this + " ? Broken.", this.debugPlayers);
            return false;
        }

        if (!this.isLoaded) {
            this.tryToLoad();
            if (!this.isLoaded) {
                AutoCraft.LOGGER.debugLog(this + " ? Failed to load.", this.debugPlayers);
                return false;
            }
        }

        if (!this.dispenserChunk.isLoaded()) {
            AutoCraft.LOGGER.debugLog(this + " ? Chunk was not loaded.", this.debugPlayers);
            return false;
        }
        updateStates();
        if (isBroken) {
            AutoCraft.LOGGER.debugLog(this + " ? Could not update states.", this.debugPlayers);
            return false;
        }
        if (destination instanceof InventoryHolder) {
            return bukkitCraftItem();
        }

        AutoCraft.LOGGER.debugLog(this + " ? Didn't find a suitable container to put items in.", this.debugPlayers);
        return false;
    }

    public boolean isRunning() {
        return (task != null);
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

    public void restart() {
        this.stop();
        this.run();
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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AutoCrafter[");
        builder.append(block.getWorld().getName());
        builder.append(":");
        builder.append(block.getX());
        builder.append(", ");
        builder.append(block.getY());
        builder.append(", ");
        builder.append(block.getZ());
        builder.append(";");
        if (this.item == null) {
            builder.append("null");
        }else {
            builder.append(this.item.getType().name());
        }
        builder.append(": B?");
        if (this.isBroken) {
            builder.append("Y");
        }else {
            builder.append("N");
        }
        builder.append(": L?");
        if (this.isLoaded()) {
            builder.append("Y");
        }else {
            builder.append("N");
        }
        builder.append(": R?");
        if (this.isRunning()) {
            builder.append("Y");
        }else {
            builder.append("N");
        }
        builder.append("]");
        return builder.toString();
    }
}
