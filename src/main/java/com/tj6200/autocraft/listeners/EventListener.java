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

package com.tj6200.autocraft.listeners;

import com.tj6200.autocraft.AutoCraft;
import com.tj6200.autocraft.api.AutoCrafter;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class EventListener implements Listener {

    public EventListener(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCloseInventory(InventoryCloseEvent e) {
        InventoryHolder inventory = e.getInventory().getHolder();

        if (!(inventory instanceof Container)) {
            return;
        }
        Container container = (Container) inventory;
        Block block = container.getBlock();
        AutoCrafter autoCrafter = AutoCraft.getAutoCrafter(block);
        if (autoCrafter == null) {
            return;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOpenInventory(InventoryOpenEvent e) {
        InventoryHolder inventory = e.getInventory().getHolder();

        if (!(inventory instanceof Container)) {
            return;
        }
        Container container = (Container) inventory;
        Block block = container.getBlock();
        AutoCrafter autoCrafter = AutoCraft.getAutoCrafter(block);
        if (autoCrafter == null) {
            return;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreakDispenser(final BlockBreakEvent e) {
        Block block = e.getBlock();
        Player player = e.getPlayer();
        AutoCraft.destroyAutoCrafter(block, player);
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDestroyItemFrame(final HangingBreakEvent e) {
        //Destroying the item frame breaks the autocrafter.
        Entity entity = e.getEntity();
        if (!(entity instanceof ItemFrame)) {
            return;
        }

        ItemFrame itemFrame = (ItemFrame) entity;
        Block block = itemFrame.getLocation().getBlock().getRelative(itemFrame.getAttachedFace());
        AutoCraft.destroyAutoCrafter(block);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemTakenFromItemFrame(final EntityDamageByEntityEvent e) {
        //Stealing the item from the item frame destroys the autocrafter.
        Entity entity = e.getEntity();
        if (!(entity instanceof ItemFrame)) {
            return;
        }


        ItemFrame itemFrame = (ItemFrame) entity;
        Block itemFrameBlock = itemFrame.getLocation().getBlock();
        Block block = itemFrameBlock.getRelative(itemFrame.getAttachedFace());

        Entity damager = e.getDamager();
        if (damager instanceof Player) {
            Player player = (Player) damager;
            AutoCraft.breakAutoCrafter(block, player);
        }else {
            AutoCraft.breakAutoCrafter(block);
        }
    }

    //This method specifically is needed because when droppers put the item directly into the neighbouring container the BlockDispenseEvent is not fired.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemMove(final InventoryMoveItemEvent e) {
        InventoryHolder initiator = e.getInitiator().getHolder();
        //Autocrafters can't drop items normally. This is to avoid dispensing ingredients when powered.
        if (!(initiator instanceof Container)) {
            return;
        }
        Container container = (Container) initiator;
        Block block = container.getBlock();

        // if the source is the inventory
        AutoCrafter autoCrafter = AutoCraft.getAutoCrafter(block);
        if (autoCrafter != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDispense(BlockDispenseEvent e) {
        Block block = e.getBlock();
        if (block.getType().equals(Material.DISPENSER)) {
            if (AutoCraft.isAutoCrafter(block)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlaceItemFrame(final HangingPlaceEvent e) {
        Hanging hanging = e.getEntity();
        if (!(hanging instanceof ItemFrame)) {
            return;
        }
        ItemFrame itemFrame = (ItemFrame) hanging;
        Block block = e.getBlock();
        if (AutoCraft.isAutoCrafter(block)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRightClickItemFrame(final PlayerInteractEntityEvent e) {
        // ignore clicking non-item frames
        Entity entity = e.getRightClicked();
        if (!entity.getType().equals(EntityType.ITEM_FRAME)) {
            return;
        }
        ItemFrame itemFrame = (ItemFrame) entity;

        // ignore clicking whilst holding nothing
        ItemStack heldItem;
        Player player = e.getPlayer();
        if (e.getHand() == EquipmentSlot.HAND) {
            heldItem = player.getInventory().getItemInMainHand();
        }else {
            heldItem = player.getInventory().getItemInOffHand();
        }
        if (heldItem == null || heldItem.getType() == Material.AIR) {
            return;
        }

        Block block = itemFrame.getLocation().getBlock().getRelative(itemFrame.getAttachedFace());
        if (block.getType().equals(Material.DISPENSER)) {
            //If there's already something in the item frame, cancel!
            //This prevents rotating the item in the item frame.
            if (itemFrame.getItem().getType() != Material.AIR) {
                e.setCancelled(true);
                return;
            }
            //Wait a second for the item to be put into the frame.

            new BukkitRunnable() {
                public void run() {
                    ItemStack item = ((ItemFrame) e.getRightClicked()).getItem();
                    AutoCraft.updateAutoCrafter(block, itemFrame, player);

                }
            }.runTaskLater(AutoCraft.INSTANCE, 1);
        }
    }
}
