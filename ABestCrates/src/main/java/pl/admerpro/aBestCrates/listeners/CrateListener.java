package pl.admerpro.aBestCrates.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import pl.admerpro.aBestCrates.gui.GuiManager;
import pl.admerpro.aBestCrates.manager.CrateLocationManager;
import pl.admerpro.aBestCrates.model.Crate;
import pl.admerpro.aBestCrates.service.OpeningService;

public class CrateListener implements Listener {
    private final CrateLocationManager crateLocationManager;
    private final OpeningService openingService;
    private final GuiManager guiManager;

    public CrateListener(CrateLocationManager crateLocationManager, OpeningService openingService, GuiManager guiManager) {
        this.crateLocationManager = crateLocationManager;
        this.openingService = openingService;
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }

        Crate crate = crateLocationManager.getCrateAt(event.getClickedBlock()).orElse(null);
        if (crate == null) {
            return;
        }

        event.setCancelled(true);
        if (event.getAction() == Action.LEFT_CLICK_BLOCK && event.getPlayer().isSneaking()) {
            openingService.openAllKeys(event.getPlayer(), crate);
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            guiManager.openPreview(event.getPlayer(), crate);
            return;
        }
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            openingService.open(event.getPlayer(), crate);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Crate crate = crateLocationManager.getCrateAt(event.getBlock()).orElse(null);
        if (crate == null) {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().sendMessage(pl.admerpro.aBestCrates.util.ColorUtil.color("&cThis crate is protected."));
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(crateLocationManager::isCrateBlock);
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(crateLocationManager::isCrateBlock);
    }
}
