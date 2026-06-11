package pl.admerpro.aBestCrates.service;

import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import pl.admerpro.aBestCrates.util.ColorUtil;

public class MessageService {
    private final JavaPlugin plugin;

    public MessageService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void send(CommandSender sender, String path) {
        send(sender, path, Map.of());
    }

    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        String message = plugin.getConfig().getString("messages." + path, path);
        String prefix = plugin.getConfig().getString("messages.prefix", "");
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
}
