package pl.admerpro.aBestCrates;

import java.io.File;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;
import pl.admerpro.aBestCrates.commands.CrateCommand;
import pl.admerpro.aBestCrates.gui.ChatInputManager;
import pl.admerpro.aBestCrates.gui.GuiManager;
import pl.admerpro.aBestCrates.integration.ABestCratesExpansion;
import pl.admerpro.aBestCrates.integration.CustomItemIntegrationService;
import pl.admerpro.aBestCrates.listeners.CrateListener;
import pl.admerpro.aBestCrates.manager.CrateLocationManager;
import pl.admerpro.aBestCrates.manager.CrateManager;
import pl.admerpro.aBestCrates.manager.KeyManager;
import pl.admerpro.aBestCrates.service.MessageService;
import pl.admerpro.aBestCrates.service.OpeningService;
import pl.admerpro.aBestCrates.service.AdvancedOpeningService;
import pl.admerpro.aBestCrates.service.CrateVisualService;
import pl.admerpro.aBestCrates.service.EconomyService;
import pl.admerpro.aBestCrates.service.PlaceholderService;
import pl.admerpro.aBestCrates.service.PlayerDataService;

public final class Main extends JavaPlugin {
    private CrateManager crateManager;
    private KeyManager keyManager;
    private CrateLocationManager crateLocationManager;
    private AdvancedOpeningService openingService;
    private GuiManager guiManager;
    private ChatInputManager chatInputManager;
    private MessageService messageService;
    private PlayerDataService playerDataService;
    private CrateVisualService visualService;
    private Metrics metrics;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        boolean configChanged = !getConfig().contains("config-version")
            || !getConfig().contains("metrics.enabled")
            || !getConfig().contains("metrics.plugin-id");
        getConfig().options().copyDefaults(true);
        if (configChanged) {
            saveConfig();
        }
        saveBundledResource("crates.yml");
        saveBundledResource("messages.yml");
        saveBundledResource("keys.yml");

        messageService = new MessageService(this);
        crateManager = new CrateManager(this);
        CustomItemIntegrationService customItems = new CustomItemIntegrationService(this);
        keyManager = new KeyManager(this, crateManager, customItems);
        crateLocationManager = new CrateLocationManager(this, crateManager);
        playerDataService = new PlayerDataService(this);
        PlaceholderService placeholderService = new PlaceholderService(this, keyManager);
        openingService = new AdvancedOpeningService(this, keyManager, messageService, playerDataService,
            new EconomyService(this), placeholderService, customItems);
        chatInputManager = new ChatInputManager(this, messageService);
        guiManager = new GuiManager(this, crateManager, keyManager, crateLocationManager, chatInputManager, messageService, openingService, playerDataService);
        visualService = new CrateVisualService(this, crateLocationManager, keyManager);
        crateLocationManager.setVisualRefresh(visualService::refreshVirtualDisplays);
        keyManager.setChangeListener(visualService::refreshVirtualDisplays);

        crateManager.load();
        keyManager.load();
        crateLocationManager.load();
        crateLocationManager.refreshHolograms();
        visualService.start();
        startMetrics();
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new ABestCratesExpansion(this, crateManager, keyManager, playerDataService).register();
        }

        CrateCommand command = new CrateCommand(this, crateManager, keyManager, crateLocationManager, openingService, guiManager, messageService, playerDataService);
        if (getCommand("abestcrates") != null) {
            getCommand("abestcrates").setExecutor(command);
            getCommand("abestcrates").setTabCompleter(command);
        }

        getServer().getPluginManager().registerEvents(chatInputManager, this);
        getServer().getPluginManager().registerEvents(guiManager, this);
        getServer().getPluginManager().registerEvents(openingService, this);
        getServer().getPluginManager().registerEvents(visualService, this);
        getServer().getPluginManager().registerEvents(new CrateListener(crateLocationManager, openingService, guiManager, keyManager, crateManager, messageService), this);

        getLogger().info("ABestCrates enabled.");
    }

    @Override
    public void onDisable() {
        if (openingService != null) {
            openingService.shutdown();
        }
        if (crateManager != null) {
            crateManager.save();
        }
        if (keyManager != null) {
            keyManager.save();
        }
        if (crateLocationManager != null) {
            crateLocationManager.clearHolograms();
            crateLocationManager.save();
        }
        if (visualService != null) {
            visualService.stop();
        }
        if (playerDataService != null) {
            playerDataService.save();
        }
    }

    public CrateManager getCrateManager() {
        return crateManager;
    }

    public KeyManager getKeyManager() {
        return keyManager;
    }

    public CrateLocationManager getCrateLocationManager() {
        return crateLocationManager;
    }

    public OpeningService getOpeningService() {
        return openingService;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public MessageService getMessageService() {
        return messageService;
    }

    private void startMetrics() {
        if (!getConfig().getBoolean("metrics.enabled", true)) {
            return;
        }
        int pluginId = getConfig().getInt("metrics.plugin-id", 0);
        if (pluginId <= 0) {
            getLogger().info("bStats metrics are enabled, but metrics.plugin-id is not configured yet.");
            return;
        }
        metrics = new Metrics(this, pluginId);
    }

    private void saveBundledResource(String resourceName) {
        if (!new File(getDataFolder(), resourceName).exists()) {
            saveResource(resourceName, false);
        }
    }
}
