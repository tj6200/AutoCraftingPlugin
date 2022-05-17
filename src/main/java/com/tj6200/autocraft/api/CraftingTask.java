package com.tj6200.autocraft.api;

import com.tj6200.autocraft.AutoCraft;
import org.bukkit.scheduler.BukkitRunnable;

public class CraftingTask extends BukkitRunnable {
    AutoCrafter autoCrafter;
    boolean ranOnce;

    public CraftingTask(AutoCrafter autoCrafter) {
        this.autoCrafter = autoCrafter;
        this.ranOnce = false;
        runTaskTimer(AutoCraft.INSTANCE, 2L, AutoCraft.craftCooldown);
    }

    @Override
    public void cancel() {
        try {
            super.cancel();
            autoCrafter.task = null;
            if (ranOnce) {
                AutoCraft.LOGGER.log(autoCrafter + " stopped.");
            }
        }catch (Exception e) {
            AutoCraft.LOGGER.log("Task cancellation unsuccessful.");
        }
    }

    @Override
    public void run() {
        if (!AutoCraft.redstoneMode.equalsIgnoreCase("disabled")) { // redstone powering type check
            if ((AutoCraft.redstoneMode.equalsIgnoreCase("indirect")
                    && this.autoCrafter.block.isBlockIndirectlyPowered()) || autoCrafter.block.isBlockPowered()) {
                return;
            }
        }
        if (autoCrafter.handle() == false) {
            this.cancel();
        }else if (!ranOnce) {
            AutoCraft.LOGGER.log(autoCrafter + " started running.");
            ranOnce = true;
        }
    }
}
