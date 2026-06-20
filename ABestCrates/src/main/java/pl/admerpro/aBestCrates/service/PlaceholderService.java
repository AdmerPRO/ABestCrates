package pl.admerpro.aBestCrates.service;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pl.admerpro.aBestCrates.manager.KeyManager;

public class PlaceholderService {
    private static final Pattern VIRTUAL_KEY_PATTERN = Pattern.compile("%abestcrates_virtual_([^%]+)%", Pattern.CASE_INSENSITIVE);
    private final JavaPlugin plugin;
    private final KeyManager keyManager;

    public PlaceholderService(JavaPlugin plugin, KeyManager keyManager) {
        this.plugin = plugin;
        this.keyManager = keyManager;
    }

    public String apply(Player player, String text) {
        String result = text == null ? "" : text;
        Matcher matcher = VIRTUAL_KEY_PATTERN.matcher(result);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            int amount = keyManager.getVirtualKeys(player.getUniqueId(), matcher.group(1));
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(String.valueOf(amount)));
        }
        matcher.appendTail(buffer);
        result = buffer.toString();

        if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                Class<?> placeholderApi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                Method setPlaceholders = placeholderApi.getMethod("setPlaceholders", OfflinePlayer.class, String.class);
                return String.valueOf(setPlaceholders.invoke(null, player, result));
            } catch (ReflectiveOperationException exception) {
                plugin.getLogger().fine("PlaceholderAPI parsing unavailable: " + exception.getMessage());
            }
        }
        return result;
    }
}
