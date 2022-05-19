/*  AutoCraft plugin
 *
 *  Copyright (C) 2021 Fliens
 *  Copyright (C) 2021 MrTransistor
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.tj6200.autocraft;

import com.google.gson.*;
import com.tj6200.autocraft.commands.DebugCraftersExecutor;
import com.tj6200.autocraft.commands.ListCraftersExecutor;
import com.tj6200.autocraft.commands.ReloadRecipesExecutor;
import com.tj6200.autocraft.commands.RestartCraftersExecutor;
import com.tj6200.autocraft.helpers.Utils;
import com.tj6200.autocraft.listeners.EntitiesLoaderListener;
import com.tj6200.autocraft.listeners.EventListener;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import com.tj6200.autocraft.api.*;
import org.bukkit.scheduler.BukkitRunnable;


public class AutoCraft extends JavaPlugin {

    public static boolean particles;
    public static String redstoneMode;
    public static long craftCooldown;
    public static long minutesPerSave;

    private static long saveCoolDown;

    public static ArrayList<AutoCrafter> autoCrafters = new ArrayList<>();

    public static LogHandler LOGGER;
    public static AutoCraft INSTANCE;

    private void registerCommand(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
        }
    }

    private void registerCommands() {
        registerCommand("reloadrecipes", new ReloadRecipesExecutor());
        registerCommand("listcrafters", new ListCraftersExecutor());
        registerCommand("restartcrafters", new RestartCraftersExecutor());
        registerCommand("debugcrafters", new DebugCraftersExecutor());
    }

    @Override
    public void onEnable() {
        super.onEnable();
        INSTANCE = this;

        LOGGER = new LogHandler(this);
        LOGGER.log("AutoCraft plugin started");

        checkConfigFile();
        updateConfig();

        Bukkit.getScheduler().runTask(this, ()-> {
            RecipeHandler.collectRecipes();
            getAutoCrafters();
        });

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, (Runnable) new BukkitRunnable() {
            @Override
            public void run() {
                saveAutoCrafters();
            }
        }, saveCoolDown, saveCoolDown);

        registerCommands();
        new EventListener(this);
        new EntitiesLoaderListener(this);
    }

    private static void getAutoCraftersFromJSON(JsonObject json) {
        JsonArray jsonArray = json.getAsJsonArray("autocrafter positions");
        Iterator<JsonElement> it = jsonArray.iterator();
        autoCrafters.clear();
        while(it.hasNext()) {
            JsonObject object = (JsonObject) it.next();
            try {
                autoCrafters.add(new AutoCrafter(object));
            }catch (IllegalArgumentException e) {
                LOGGER.log("AutoCrafter does not exist anymore.");
            }
        }
    }

    private static void getAutoCrafters() {
        File file = AutoCraft.INSTANCE.getFile("autocrafters.json");
        try {
            String json = Files.readString(Path.of(file.getPath()));
            JsonObject jsonObject = (JsonObject) JsonParser.parseString(json);
            getAutoCraftersFromJSON(jsonObject);
            LOGGER.log(autoCrafters.size() + " autocrafters initialized");
        }catch(Exception e) {
            LOGGER.log(e.getLocalizedMessage());
            LOGGER.log("Could not load json file");
        }
    }

    private static JsonObject autoCraftersJSON() {
        JsonArray array = new JsonArray();
        for(AutoCrafter autoCrafter: autoCrafters) {
            if (!autoCrafter.isBroken()) {
                array.add(autoCrafter.toJSON());
            }
        }
        JsonObject object = new JsonObject();
        object.add("autocrafter positions", array);
        return object;
    }

    private void saveAutoCrafters() {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        JsonObject object = autoCraftersJSON();
        File file = getFile("autocrafters.json");
        try {
            FileWriter fw = new FileWriter(file, false);
            fw.flush();
            gson.toJson(object, fw);
            fw.close();
            LOGGER.log("Saved AutoCrafters");
        } catch (IOException e) {
            LOGGER.log("Could not save autocrafters");
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        saveAutoCrafters();
        LOGGER.log("AutoCraft plugin stopped");
    }

    public static AutoCrafter getAutoCrafter(Block block) {
        for(AutoCrafter autoCrafter: autoCrafters) {
            if (autoCrafter.block.equals(block)) {
                return autoCrafter;
            }
        }
        return null;
    }

    public static AutoCrafter getAutoCrafterFromInventory(Block block) {
        for(AutoCrafter autoCrafter: autoCrafters) {
            if (autoCrafter.destinationBlock.equals(block)) {
                return autoCrafter;
            }
        }
        return null;
    }
    public static AutoCrafter getAutoCrafterFromInventorySpots(Block block1, Block block2) {
        for(AutoCrafter autoCrafter: autoCrafters) {
            if (autoCrafter.destinationBlock.equals(block1) || autoCrafter.destinationBlock.equals(block2)) {
                return autoCrafter;
            }
        }
        return null;
    }

    public static ArrayList<AutoCrafter> getAutoCraftersInChunk(Chunk chunk) {
        ArrayList<AutoCrafter> list = new ArrayList<>();
        for (AutoCrafter autoCrafter: autoCrafters) {
            if (autoCrafter.dispenserChunk.getChunkKey() == chunk.getChunkKey() &&
                autoCrafter.dispenserChunk.getWorld() == chunk.getWorld()) {
                list.add(autoCrafter);
            }
        }
        return list;
    }

    public static boolean isAutoCrafter(Block block) {
        for(AutoCrafter autoCrafter: autoCrafters) {
            if (autoCrafter.block.equals(block)) {
                return true;
            }
        }
        return false;
    }

    public static void updateAutoCrafter(Block block, ItemFrame itemFrame, Player player) {
        ItemStack item = itemFrame.getItem();
        for(AutoCrafter autoCrafter: autoCrafters) {
            if(autoCrafter.block.equals(block)) {
                Utils.sendActionBarMessageToPlayer(player, "AutoCrafter Item Updated");
                autoCrafter.setItem(item);
                return;
            }
        }
        Utils.sendActionBarMessageToPlayer(player, "AutoCrafter Created");
        Block itemFrameBlock = itemFrame.getLocation().getBlock();
        autoCrafters.add(new AutoCrafter(block, itemFrameBlock, item));
    }

    public static void breakAutoCrafter(Block block) {
        breakAutoCrafter(block, null);
    }

    public static void breakAutoCrafter(Block block, Player player) {
        for(AutoCrafter autoCrafter: autoCrafters) {
            if (autoCrafter.block.equals(block)) {
                if (autoCrafter.isBroken()) {
                    return;
                }
                if (player != null) {
                    Utils.sendActionBarMessageToPlayer(player, "AutoCrafter Broken");
                }
                autoCrafter.breakCrafter();
                return;
            }
        }
    }

    public static void destroyAutoCrafter(Block block, Player player) {
        for(AutoCrafter autoCrafter: autoCrafters) {
            if (autoCrafter.block.equals(block)) {
                if (player != null) {
                    Utils.sendActionBarMessageToPlayer(player, "AutoCrafter Destroyed");
                }
                autoCrafter.breakCrafter();
                autoCrafters.remove(autoCrafter);
                return;
            }
        }
    }

    public static void destroyAutoCrafter(Block block) {
        destroyAutoCrafter(block, null);
    }

    public static boolean areAllAutoCraftersLoaded() {
        boolean result = true;
        for(AutoCrafter autoCrafter: autoCrafters) {
            result = result && autoCrafter.isLoaded();
        }
        return result;
    }

    public static void load(Chunk chunk, List<Entity> entities) {
        for(AutoCrafter autoCrafter: autoCrafters) {
            if (chunk.getChunkKey() == autoCrafter.itemFrameChunk.getChunkKey()) {
                autoCrafter.load(entities);
            }
        }
    }

    /**
     * This method returns a list of coordinates for particles around a block
     *
     * @param loc              block position
     * @param particleDistance distance between particles
     * @return a list of particle positions
     **/

    public static List<Location> getHollowCube(Location loc, double particleDistance) {
        List<Location> result = new ArrayList<>();
        World world = loc.getWorld();
        double minX = loc.getBlockX();
        double minY = loc.getBlockY();
        double minZ = loc.getBlockZ();
        double maxX = loc.getBlockX() + 1;
        double maxY = loc.getBlockY() + 1;
        double maxZ = loc.getBlockZ() + 1;

        for (double x = minX; x <= maxX; x = Math.round((x + particleDistance) * 1e2) / 1e2) {
            for (double y = minY; y <= maxY; y = Math.round((y + particleDistance) * 1e2) / 1e2) {
                for (double z = minZ; z <= maxZ; z = Math.round((z + particleDistance) * 1e2) / 1e2) {
                    int components = 0;
                    if (x == minX || x == maxX)
                        components++;
                    if (y == minY || y == maxY)
                        components++;
                    if (z == minZ || z == maxZ)
                        components++;
                    if (components >= 2) {
                        result.add(new Location(world, x, y, z));
                    }
                }
            }
        }
        return result;
    }

    /**
     * This method checks if the configuration file exists and has right version
     * If config version doesn't match, backs it up to config_old.yml
     */

    private void checkConfigFile(){
        File configFile = getFile("config.yml");
        if(configFile.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(getResource("config.yml")));
            if (config.getInt("config-version") != defaultConfig.getInt("config-version")) {
                LOGGER.log(ChatColor.RED + "Config version does not match, saving to 'config_old.yml' and going back to defaults");

                if(!configFile.renameTo(getFile("config_old.yml"))) {
                    LOGGER.log("Could not rename old file.");
                }
            }
        }
        saveDefaultConfig();
    }

    /**
     * This method returns File object for specified file inside plugin data folder
     */

    private File getFile(String fileName){
        return new File(getDataFolder(), fileName.replace('/', File.separatorChar));
    }

    /**
     * This method reloads values from config file
     */

    private void setMinutesPerSave(long minutes) {
        minutesPerSave = minutes;
        saveCoolDown = 20 * 60 * minutes;
    }

    private void updateConfig(){
        craftCooldown = getConfig().getLong("craftCooldown");
        particles = getConfig().getBoolean("particles");
        redstoneMode = getConfig().getString("redstoneMode");
        setMinutesPerSave(getConfig().getLong("saveCooldown"));
    }


    /**
     * This method adds items to an inventory
     *
     * @param inventory inventory to add items to
     * @param items     an ArrayList of items to add
     * @return true if items were added, false if items weren't added because some of them didn't fit
     **/

    public static boolean addItemsIfCan(Inventory inventory, List<ItemStack> items) { // I think this needs explanation
        ItemStack[] mutableCont = inventory.getContents(); // We have to manually clone each ItemStack to prevent them from
        ItemStack[] contents = inventory.getContents(); // mutation when adding items. This is the backup inventory
        for (int i = 0; i < mutableCont.length; i++)
            contents[i] = mutableCont[i] == null ? null : mutableCont[i].clone();
        for (ItemStack item : items) {
            Map<Integer, ItemStack> left = inventory.addItem(item.clone()); // addItem() returns a map of items which didn`t fit
            if (!left.isEmpty()) {
                inventory.setStorageContents(contents);// if so, we rollback the inventory
                return false;
            }
        }
        return true;
    }

    /*
     * Warning! testIfCanFit() method should be only used if you don`t need to add items.
     * In cases when you have to add items please use addItemsIfCan()
     */

    /**
     * This method is similar to addItemsifCan() with only one difference - it doesn't add items
     *
     * @param inventory inventory to add items to
     * @param items     an ArrayList of items to add
     * @return true if items can fit into the inventory, false if not
     * @see AutoCraft#addItemsIfCan(Inventory inventory, List items)
     **/

    public static boolean testIfCanFit(Inventory inventory, ArrayList<ItemStack> items) {
        ItemStack[] mutableCont = inventory.getContents();
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < mutableCont.length; i++)
            contents[i] = mutableCont[i] == null ? null : mutableCont[i].clone();
        for (ItemStack item : items) {
            Map<Integer, ItemStack> left = inventory.addItem(item.clone());
            if (!left.isEmpty()) {
                inventory.setStorageContents(contents);
                return false;
            }
        }
        inventory.setStorageContents(contents);
        return true;
    }
}