package pl.admerpro.aBestCrates;

import org.bukkit.plugin.java.JavaPlugin;
import pl.admerpro.aBestCrates.commands.CrateCommand;
import pl.admerpro.aBestCrates.gui.ChatInputManager;
import pl.admerpro.aBestCrates.gui.GuiManager;
import pl.admerpro.aBestCrates.listeners.CrateListener;
import pl.admerpro.aBestCrates.manager.CrateLocationManager;
import pl.admerpro.aBestCrates.manager.CrateManager;
import pl.admerpro.aBestCrates.manager.KeyManager;
import pl.admerpro.aBestCrates.service.MessageService;
import pl.admerpro.aBestCrates.service.OpeningService;

public final class Main extends JavaPlugin {
    private CrateManager crateManager;
    private KeyManager keyManager;
    private CrateLocationManager crateLocationManager;
    private OpeningService openingService;
    private GuiManager guiManager;
    private ChatInputManager chatInputManager;
    private MessageService messageService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        messageService = new MessageService(this);
        crateManager = new CrateManager(this);
        keyManager = new KeyManager(this, crateManager);
        crateLocationManager = new CrateLocationManager(this, crateManager);
        openingService = new OpeningService(this, keyManager, messageService);
        chatInputManager = new ChatInputManager(this);
        guiManager = new GuiManager(this, crateManager, keyManager, crateLocationManager, chatInputManager, messageService);

        crateManager.load();
        keyManager.load();
        crateLocationManager.load();
        crateLocationManager.refreshHolograms();

        CrateCommand command = new CrateCommand(this, crateManager, keyManager, crateLocationManager, openingService, guiManager, messageService);
        if (getCommand("abestcrates") != null) {
            getCommand("abestcrates").setExecutor(command);
            getCommand("abestcrates").setTabCompleter(command);
        }

        getServer().getPluginManager().registerEvents(chatInputManager, this);
        getServer().getPluginManager().registerEvents(guiManager, this);
        getServer().getPluginManager().registerEvents(new CrateListener(crateLocationManager, openingService, guiManager), this);

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
