package pl.admerpro.aBestCrates.util;

import java.util.List;
import org.bukkit.ChatColor;

public final class ColorUtil {
    private ColorUtil() {
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    public static List<String> color(List<String> lines) {
        return lines.stream().map(ColorUtil::color).toList();
    }

    public static String removeColor(String text) {
        return ChatColor.stripColor(color(text));
    }
}
