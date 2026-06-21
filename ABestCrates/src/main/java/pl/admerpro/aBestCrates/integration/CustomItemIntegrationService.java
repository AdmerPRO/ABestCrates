package pl.admerpro.aBestCrates.integration;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/** Refreshes serialized custom items through their owning plugin when its public API is available. */
public final class CustomItemIntegrationService {
    private final JavaPlugin plugin;
    private final Set<String> warned = new java.util.HashSet<>();

    public CustomItemIntegrationService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public ItemStack refresh(ItemStack source) {
        if (source == null || source.getType().isAir()) {
            return source == null ? null : source.clone();
        }
        int amount = source.getAmount();
        for (String provider : List.of("Nexo", "ItemsAdder", "Oraxen", "MMOItems", "ExecutableItems")) {
            if (!Bukkit.getPluginManager().isPluginEnabled(provider)) {
                continue;
            }
            try {
                ItemStack refreshed = switch (provider) {
                    case "ItemsAdder" -> refreshItemsAdder(source);
                    case "Oraxen" -> refreshBuilderApi("io.th0rgal.oraxen.api.OraxenItems", source);
                    case "Nexo" -> refreshBuilderApi("com.nexomc.nexo.api.NexoItems", source);
                    case "MMOItems" -> refreshMmoItems(source);
                    case "ExecutableItems" -> refreshExecutableItems(source);
                    default -> null;
                };
                if (refreshed != null && !refreshed.getType().isAir()) {
                    refreshed.setAmount(amount);
                    return refreshed;
                }
            } catch (ReflectiveOperationException | LinkageError exception) {
                warnOnce(provider, exception);
            }
        }
        return source.clone();
    }

    private ItemStack refreshItemsAdder(ItemStack source) throws ReflectiveOperationException {
        Class<?> customStack = Class.forName("dev.lone.itemsadder.api.CustomStack");
        Object wrapped = invokeStatic(customStack, List.of("byItemStack", "getInstance"), source);
        return extractItem(wrapped);
    }

    private ItemStack refreshBuilderApi(String className, ItemStack source) throws ReflectiveOperationException {
        Class<?> api = Class.forName(className);
        Object id = invokeStatic(api, List.of("idFromItem", "getIdByItem"), source);
        if (!(id instanceof String stringId) || stringId.isBlank()) {
            return null;
        }
        Object builder = invokeStatic(api, List.of("itemFromId", "getItemById"), stringId);
        return extractItem(builder);
    }

    private ItemStack refreshMmoItems(ItemStack source) throws ReflectiveOperationException {
        Class<?> api = Class.forName("net.Indyuce.mmoitems.MMOItems");
        Object typeId = invokeStatic(api, List.of("getType"), source);
        Object itemId = invokeStatic(api, List.of("getID", "getId"), source);
        if (!(typeId instanceof String type) || !(itemId instanceof String id) || type.isBlank() || id.isBlank()) {
            return null;
        }
        Field pluginField = api.getField("plugin");
        Object mmoPlugin = pluginField.get(null);
        Object types = invoke(mmoPlugin, List.of("getTypes"));
        Object itemType = invoke(types, List.of("get"), type);
        Object built = invoke(mmoPlugin, List.of("getMMOItem", "getItem"), itemType, id);
        return extractItem(built);
    }

    private ItemStack refreshExecutableItems(ItemStack source) throws ReflectiveOperationException {
        Class<?> api = Class.forName("com.ssomar.score.api.executableitems.ExecutableItemsAPI");
        Object manager = invokeStatic(api, List.of("getExecutableItemsManager"));
        Object identified = unwrap(invoke(manager, List.of("getExecutableItem"), source));
        if (identified == null) {
            return null;
        }
        Object id = invoke(identified, List.of("getId", "getID"));
        if (!(id instanceof String stringId) || stringId.isBlank()) {
            return null;
        }
        Object definition = unwrap(invoke(manager, List.of("getExecutableItem"), stringId));
        ItemStack direct = extractItem(definition);
        if (direct != null) {
            return direct;
        }
        Object built = invokeOptional(definition, List.of("buildItem", "getItem"), 1, Optional.empty());
        return extractItem(built);
    }

    private ItemStack extractItem(Object value) throws ReflectiveOperationException {
        value = unwrap(value);
        if (value instanceof ItemStack itemStack) {
            return itemStack.clone();
        }
        if (value == null) {
            return null;
        }
        Object built = invokeOptional(value, List.of("build", "getItemStack", "getItem"));
        built = unwrap(built);
        return built instanceof ItemStack itemStack ? itemStack.clone() : null;
    }

    private Object invokeStatic(Class<?> type, List<String> names, Object... arguments)
        throws ReflectiveOperationException {
        Method method = findMethod(type, names, true, arguments);
        return method == null ? null : method.invoke(null, arguments);
    }

    private Object invoke(Object target, List<String> names, Object... arguments) throws ReflectiveOperationException {
        if (target == null) {
            return null;
        }
        Method method = findMethod(target.getClass(), names, false, arguments);
        return method == null ? null : method.invoke(target, arguments);
    }

    private Object invokeOptional(Object target, List<String> names, Object... arguments) {
        try {
            return invoke(target, names, arguments);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private Method findMethod(Class<?> type, List<String> names, boolean requireStatic, Object[] arguments) {
        for (Method method : type.getMethods()) {
            if (!names.contains(method.getName()) || method.getParameterCount() != arguments.length
                || (requireStatic && !Modifier.isStatic(method.getModifiers()))) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            boolean compatible = true;
            for (int index = 0; index < arguments.length; index++) {
                if (arguments[index] != null && !boxed(parameterTypes[index]).isInstance(arguments[index])) {
                    compatible = false;
                    break;
                }
            }
            if (compatible) {
                return method;
            }
        }
        return null;
    }

    private Class<?> boxed(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        return switch (type.getName()) {
            case "int" -> Integer.class;
            case "long" -> Long.class;
            case "double" -> Double.class;
            case "float" -> Float.class;
            case "boolean" -> Boolean.class;
            case "byte" -> Byte.class;
            case "short" -> Short.class;
            case "char" -> Character.class;
            default -> type;
        };
    }

    private Object unwrap(Object value) {
        return value instanceof Optional<?> optional ? optional.orElse(null) : value;
    }

    private void warnOnce(String provider, Throwable exception) {
        if (warned.add(provider)) {
            plugin.getLogger().log(Level.WARNING, provider
                + " is installed, but its custom-item API was not compatible. Stored ItemStack data will be used safely.", exception);
        }
    }
}
