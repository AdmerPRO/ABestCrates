package pl.admerpro.aBestCrates.service;

import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import pl.admerpro.aBestCrates.util.ColorUtil;

public class MessageService {
    private final JavaPlugin plugin;
    private YamlConfiguration messages;

    public MessageService(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        messages = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));
    }

    public void send(CommandSender sender, String path) {
        send(sender, path, Map.of());
    }

    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        String message = get(path);
        String prefix = plugin.getConfig().contains("messages.prefix")
            ? plugin.getConfig().getString("messages.prefix", "")
            : messages.getString("messages.prefix", "");
        if (!message.startsWith("<no-prefix>")) {
            message = prefix + message;
        } else {
            message = message.substring("<no-prefix>".length());
        }
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }
        sender.sendMessage(ColorUtil.component(message));
    }

    public String get(String path) {
        String fullPath = "messages." + path;
        return plugin.getConfig().contains(fullPath)
            ? plugin.getConfig().getString(fullPath, path)
            : messages.getString(fullPath, path);
    }
}
