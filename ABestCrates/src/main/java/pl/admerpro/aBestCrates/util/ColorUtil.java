package pl.admerpro.aBestCrates.util;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class ColorUtil {
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();
    private static final String LEGACY_CODES = "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx";

    private ColorUtil() {
    }

    public static String color(String text) {
        return LEGACY_SECTION.serialize(component(text));
    }

    public static List<String> color(List<String> lines) {
        return lines == null ? List.of() : lines.stream().map(ColorUtil::color).toList();
    }

    public static Component component(String text) {
        return LEGACY_SECTION.deserialize(toSectionCodes(text));
    }

    public static List<Component> components(List<String> lines) {
        return lines == null ? List.of() : lines.stream().map(ColorUtil::component).toList();
    }

    public static String legacy(Component component) {
        return LEGACY_SECTION.serialize(component == null ? Component.empty() : component);
    }

    public static String removeColor(String text) {
        return PLAIN_TEXT.serialize(component(text));
    }

    private static String toSectionCodes(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        char[] chars = text.toCharArray();
        for (int index = 0; index < chars.length - 1; index++) {
            if (chars[index] == '&' && LEGACY_CODES.indexOf(chars[index + 1]) >= 0) {
                chars[index] = LegacyComponentSerializer.SECTION_CHAR;
            }
        }
        return new String(chars);
    }
}
