package com.tj6200.autocraft.listeners;

import com.tj6200.autocraft.AutoCraft;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class EntitiesLoaderListener implements Listener {

    public EntitiesLoaderListener(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityLoad(EntitiesLoadEvent e) {
        if (AutoCraft.areAllAutoCraftersLoaded()) {
            AutoCraft.LOGGER.log("All AutoCrafters are loaded!");
            unregister();
            return;
        }
        Chunk chunk = e.getChunk();
        List<Entity> list = e.getEntities();

        AutoCraft.load(chunk, list);
    }

    public void unregister() {
        HandlerList.unregisterAll(this);
    }

}
