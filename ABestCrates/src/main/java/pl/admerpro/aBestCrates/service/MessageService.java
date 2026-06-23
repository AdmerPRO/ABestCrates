package pl.admerpro.aBestCrates.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import pl.admerpro.aBestCrates.util.ColorUtil;

public class MessageService {
    private final JavaPlugin plugin;
    private YamlConfiguration messages;

    public MessageService(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        messages = YamlConfiguration.loadConfiguration(file);
        mergeDefaults(file);
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

    public List<String> getList(String path) {
        String fullPath = "messages." + path;
        if (plugin.getConfig().isList(fullPath)) {
            return plugin.getConfig().getStringList(fullPath);
        }
        return messages.getStringList(fullPath);
    }

    public void sendList(CommandSender sender, String path) {
        for (String line : getList(path)) {
            sender.sendMessage(ColorUtil.component(line));
        }
    }

    private void mergeDefaults(File file) {
        try (InputStream stream = plugin.getResource("messages.yml")) {
            if (stream == null) {
                return;
            }
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
            if (copyMissing(defaults, "")) {
                messages.save(file);
            }
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not update missing messages.yml defaults: " + exception.getMessage());
        }
    }

    private boolean copyMissing(ConfigurationSection defaults, String path) {
        boolean changed = false;
        for (String key : defaults.getKeys(false)) {
            String fullPath = path.isEmpty() ? key : path + "." + key;
            ConfigurationSection section = defaults.getConfigurationSection(key);
            if (section != null) {
                changed |= copyMissing(section, fullPath);
                continue;
            }
            if (!messages.contains(fullPath)) {
                messages.set(fullPath, defaults.get(key));
                changed = true;
            }
        }
        return changed;
    }
}
