package pl.admerpro.aBestCrates.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import pl.admerpro.aBestCrates.util.ColorUtil;

public class ChatInputManager implements Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, PendingInput> pendingInputs = new HashMap<>();

    public ChatInputManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void request(Player player, String prompt, Consumer<String> callback) {
        pendingInputs.put(player.getUniqueId(), new PendingInput(callback));
        player.closeInventory();
        player.sendMessage(ColorUtil.color(prompt));
        player.sendMessage(ColorUtil.color("&7Type &ccancel &7to cancel."));
    }

    @EventHandler
    @SuppressWarnings("deprecation")
    public void onChat(AsyncPlayerChatEvent event) {
        PendingInput pendingInput = pendingInputs.remove(event.getPlayer().getUniqueId());
        if (pendingInput == null) {
            return;
        }

        event.setCancelled(true);
        String message = event.getMessage();
        if (message.equalsIgnoreCase("cancel")) {
            event.getPlayer().sendMessage(ColorUtil.color("&cCancelled."));
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> pendingInput.callback().accept(message));
    }

    private record PendingInput(Consumer<String> callback) {
    }
}
