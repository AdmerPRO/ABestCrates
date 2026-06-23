package pl.admerpro.aBestCrates.gui;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import pl.admerpro.aBestCrates.service.MessageService;
import pl.admerpro.aBestCrates.util.ColorUtil;

public class ChatInputManager implements Listener {
    private final JavaPlugin plugin;
    private final MessageService messageService;
    private final Map<UUID, PendingInput> pendingInputs = new ConcurrentHashMap<>();

    public ChatInputManager(JavaPlugin plugin, MessageService messageService) {
        this.plugin = plugin;
        this.messageService = messageService;
    }

    public void request(Player player, String prompt, Consumer<String> callback, Runnable cancelCallback) {
        pendingInputs.put(player.getUniqueId(), new PendingInput(callback, cancelCallback));
        player.closeInventory();
        player.sendMessage(ColorUtil.component(prompt));
        messageService.send(player, "chat-cancel-instruction");
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
            Bukkit.getScheduler().runTask(plugin, () -> {
                messageService.send(event.getPlayer(), "chat-cancelled");
                pendingInput.cancelCallback().run();
            });
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> pendingInput.callback().accept(message));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendingInputs.remove(event.getPlayer().getUniqueId());
    }

    private record PendingInput(Consumer<String> callback, Runnable cancelCallback) {
    }
}
