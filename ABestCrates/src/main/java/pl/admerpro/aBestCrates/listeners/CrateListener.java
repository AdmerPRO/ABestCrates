package pl.admerpro.aBestCrates.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import pl.admerpro.aBestCrates.gui.GuiManager;
import pl.admerpro.aBestCrates.manager.CrateLocationManager;
import pl.admerpro.aBestCrates.manager.KeyManager;
import pl.admerpro.aBestCrates.manager.CrateManager;
import pl.admerpro.aBestCrates.model.Crate;
import pl.admerpro.aBestCrates.service.AdvancedOpeningService;
import pl.admerpro.aBestCrates.util.ColorUtil;

public class CrateListener implements Listener {
    private final CrateLocationManager crateLocationManager;
    private final AdvancedOpeningService openingService;
    private final GuiManager guiManager;
    private final KeyManager keyManager;
    private final CrateManager crateManager;

    public CrateListener(CrateLocationManager crateLocationManager, AdvancedOpeningService openingService,
                         GuiManager guiManager, KeyManager keyManager, CrateManager crateManager) {
        this.crateLocationManager = crateLocationManager;
        this.openingService = openingService;
        this.guiManager = guiManager;
        this.keyManager = keyManager;
        this.crateManager = crateManager;
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
            openingService.open(event.getPlayer(), crate, event.getClickedBlock().getLocation());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        keyManager.getCrateIdFromCrateItem(event.getItemInHand()).flatMap(crateManager::getCrate)
            .ifPresent(crate -> {
                event.getBlockPlaced().setType(crate.getBlockMaterial());
                crateLocationManager.linkCrateBlock(event.getBlockPlaced(), crate);
            });
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Crate crate = crateLocationManager.getCrateAt(event.getBlock()).orElse(null);
        if (crate == null) {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().sendMessage(ColorUtil.component("&cThis crate is protected."));
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(crateLocationManager::isCrateBlock);
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(crateLocationManager::isCrateBlock);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (crateLocationManager.isCrateHologram(event.getEntity())) {
            event.setCancelled(true);
        }
    }
}
