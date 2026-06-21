package pl.admerpro.aBestCrates;

import org.bukkit.plugin.java.JavaPlugin;
import pl.admerpro.aBestCrates.commands.CrateCommand;
import pl.admerpro.aBestCrates.gui.ChatInputManager;
import pl.admerpro.aBestCrates.gui.GuiManager;
import pl.admerpro.aBestCrates.integration.ABestCratesExpansion;
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

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("crates.yml", false);
        saveResource("messages.yml", false);
        saveResource("keys.yml", false);

        messageService = new MessageService(this);
        crateManager = new CrateManager(this);
        keyManager = new KeyManager(this, crateManager);
        crateLocationManager = new CrateLocationManager(this, crateManager);
        playerDataService = new PlayerDataService(this);
        PlaceholderService placeholderService = new PlaceholderService(this, keyManager);
        openingService = new AdvancedOpeningService(this, keyManager, messageService, playerDataService,
            new EconomyService(this), placeholderService);
        chatInputManager = new ChatInputManager(this);
        guiManager = new GuiManager(this, crateManager, keyManager, crateLocationManager, chatInputManager, messageService, openingService);
        visualService = new CrateVisualService(this, crateLocationManager, keyManager);
        crateLocationManager.setVisualRefresh(visualService::refreshVirtualDisplays);
        keyManager.setChangeListener(visualService::refreshVirtualDisplays);

        crateManager.load();
        keyManager.load();
        crateLocationManager.load();
        crateLocationManager.refreshHolograms();
        visualService.start();
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new ABestCratesExpansion(this, crateManager, keyManager, playerDataService).register();
        }

        CrateCommand command = new CrateCommand(this, crateManager, keyManager, crateLocationManager, openingService, guiManager, messageService);
        if (getCommand("abestcrates") != null) {
            getCommand("abestcrates").setExecutor(command);
            getCommand("abestcrates").setTabCompleter(command);
        }

        getServer().getPluginManager().registerEvents(chatInputManager, this);
        getServer().getPluginManager().registerEvents(guiManager, this);
        getServer().getPluginManager().registerEvents(openingService, this);
        getServer().getPluginManager().registerEvents(visualService, this);
        getServer().getPluginManager().registerEvents(new CrateListener(crateLocationManager, openingService, guiManager, keyManager, crateManager), this);

        getLogger().info("ABestCrates enabled.");
    }

    @Override
    public void onDisable() {
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
}
