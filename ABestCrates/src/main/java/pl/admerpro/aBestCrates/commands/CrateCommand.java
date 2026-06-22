package pl.admerpro.aBestCrates.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pl.admerpro.aBestCrates.gui.GuiManager;
import pl.admerpro.aBestCrates.manager.CrateLocationManager;
import pl.admerpro.aBestCrates.manager.CrateManager;
import pl.admerpro.aBestCrates.manager.KeyManager;
import pl.admerpro.aBestCrates.model.Crate;
import pl.admerpro.aBestCrates.service.MessageService;
import pl.admerpro.aBestCrates.service.OpeningService;
import pl.admerpro.aBestCrates.util.ColorUtil;

public class CrateCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of(
        "gui",
        "reload",
        "create",
        "delete",
        "deletecrate",
        "spawncrate",
        "edit",
        "givekey",
        "giveall",
        "givecrate",
        "addkeys",
        "removekeys",
        "forceopen"
    );

    private final JavaPlugin plugin;
    private final CrateManager crateManager;
    private final KeyManager keyManager;
    private final CrateLocationManager crateLocationManager;
    private final OpeningService openingService;
    private final GuiManager guiManager;
    private final MessageService messageService;

    public CrateCommand(JavaPlugin plugin, CrateManager crateManager, KeyManager keyManager, CrateLocationManager crateLocationManager,
                        OpeningService openingService, GuiManager guiManager, MessageService messageService) {
        this.plugin = plugin;
        this.crateManager = crateManager;
        this.keyManager = keyManager;
        this.crateLocationManager = crateLocationManager;
        this.openingService = openingService;
        this.guiManager = guiManager;
        this.messageService = messageService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            openGui(sender);
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        switch (subcommand) {
            case "gui" -> openGui(sender);
            case "reload" -> reload(sender);
            case "create" -> create(sender, args);
            case "delete" -> delete(sender, args);
            case "deletecrate" -> deletePlacedCrate(sender);
            case "spawncrate" -> spawnCrate(sender, args);
            case "edit" -> edit(sender, args);
            case "givekey" -> giveKey(sender, args);
            case "giveall" -> giveAll(sender, args);
            case "givecrate" -> giveCrate(sender, args);
            case "addkeys" -> addKeys(sender, args);
            case "removekeys" -> removeKeys(sender, args);
            case "forceopen" -> forceOpen(sender, args);
            default -> sendHelp(sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && List.of("givekey", "givecrate", "addkeys", "removekeys", "forceopen").contains(subcommand)) {
            return filter(playerNames(), args[1]);
        }
        if (args.length == 2 && List.of("delete", "spawncrate", "edit").contains(subcommand)) {
            return filter(crateNames(), args[1]);
        }
        if (args.length == 2 && subcommand.equals("giveall")) {
            return filter(crateNames(), args[1]);
        }
        if (args.length == 3 && List.of("givekey", "givecrate", "addkeys", "removekeys", "forceopen").contains(subcommand)) {
            return filter(crateNames(), args[2]);
        }
        if (args.length == 4 && List.of("givekey", "givecrate", "addkeys", "removekeys").contains(subcommand)) {
            return filter(List.of("1", "2", "3", "5", "10"), args[3]);
        }
        if (args.length == 3 && subcommand.equals("giveall")) {
            return filter(List.of("1", "2", "3", "5", "10"), args[2]);
        }
        if (args.length == 4 && subcommand.equals("giveall")) {
            return filter(List.of("physical", "virtual"), args[3]);
        }
        return List.of();
    }

    private void openGui(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sendHelp(sender);
            return;
        }
        if (!has(sender, "abestcrates.admin")) {
            messageService.send(sender, "no-permission");
            return;
        }
        guiManager.openMain(player);
    }

    private void reload(CommandSender sender) {
        if (!has(sender, "abestcrates.reload")) {
            messageService.send(sender, "no-permission");
            return;
        }
        plugin.reloadConfig();
        messageService.reload();
        crateManager.load();
        keyManager.load();
        crateLocationManager.load();
        crateLocationManager.refreshHolograms();
        messageService.send(sender, "reloaded");
    }

    private void create(CommandSender sender, String[] args) {
        if (!has(sender, "abestcrates.create")) {
            messageService.send(sender, "no-permission");
            return;
        }
        if (args.length < 2) {
            messageService.send(sender, "usage", Map.of("%usage%", "/abestcrates create <name>"));
            return;
        }
        if (!Crate.isValidId(args[1])) {
            messageService.send(sender, "crate-id-invalid");
            return;
        }
        if (crateManager.exists(args[1])) {
            messageService.send(sender, "crate-exists", Map.of("%crate%", args[1]));
            return;
        }
        Crate crate = crateManager.createCrate(args[1]);
        messageService.send(sender, "crate-created", Map.of("%crate%", crate.getId()));
        if (sender instanceof Player player) {
            guiManager.openEdit(player, crate);
        }
    }

    private void delete(CommandSender sender, String[] args) {
        if (!has(sender, "abestcrates.create")) {
            messageService.send(sender, "no-permission");
            return;
        }
        if (args.length < 2) {
            messageService.send(sender, "usage", Map.of("%usage%", "/abestcrates delete <name>"));
            return;
        }
        if (crateManager.deleteCrate(args[1])) {
            crateLocationManager.removeCratePlacements(args[1]);
            keyManager.removeVirtualCrate(args[1]);
            messageService.send(sender, "crate-deleted", Map.of("%crate%", args[1]));
        } else {
            messageService.send(sender, "crate-missing", Map.of("%crate%", args[1]));
        }
    }

    private void deletePlacedCrate(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messageService.send(sender, "player-only");
            return;
        }
        if (!has(sender, "abestcrates.create")) {
            messageService.send(sender, "no-permission");
            return;
        }

        Block targetBlock = player.getTargetBlockExact(8);
        if (targetBlock == null || !crateLocationManager.removePlacedCrate(targetBlock)) {
            messageService.send(sender, "placed-crate-missing");
            return;
        }
        messageService.send(sender, "placed-crate-deleted");
    }


    private void spawnCrate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messageService.send(sender, "player-only");
            return;
        }
        if (!has(sender, "abestcrates.create")) {
            messageService.send(sender, "no-permission");
            return;
        }
        if (args.length < 2) {
            messageService.send(sender, "usage", Map.of("%usage%", "/abestcrates spawncrate <name>"));
            return;
        }
        crateManager.getCrate(args[1]).ifPresentOrElse(crate -> {
            crateLocationManager.placeCrate(player, crate);
            messageService.send(sender, "crate-spawned", Map.of("%crate%", crate.getId()));
        }, () -> messageService.send(sender, "crate-missing", Map.of("%crate%", args[1])));
    }

    private void edit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messageService.send(sender, "player-only");
            return;
        }
        if (!has(sender, "abestcrates.admin")) {
            messageService.send(sender, "no-permission");
            return;
        }
        if (args.length < 2) {
            messageService.send(sender, "usage", Map.of("%usage%", "/abestcrates edit <name>"));
            return;
        }
        crateManager.getCrate(args[1]).ifPresentOrElse(crate -> guiManager.openEdit(player, crate),
            () -> messageService.send(sender, "crate-missing", Map.of("%crate%", args[1])));
    }

    private void giveKey(CommandSender sender, String[] args) {
        if (!has(sender, "abestcrates.givekey")) {
            messageService.send(sender, "no-permission");
            return;
        }
        if (args.length < 4) {
            messageService.send(sender, "usage", Map.of("%usage%", "/abestcrates givekey <player> <crate> <amount>"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            messageService.send(sender, "player-offline", Map.of("%player%", args[1]));
            return;
        }
        int amount = parseAmount(args[3]);
        crateManager.getCrate(args[2]).ifPresentOrElse(crate -> {
            givePhysicalKeys(target, crate, amount);
            messageService.send(sender, "key-given", Map.of("%player%", target.getName(), "%crate%", crate.getId(), "%amount%", String.valueOf(amount)));
        }, () -> messageService.send(sender, "crate-missing", Map.of("%crate%", args[2])));
    }

    private void giveCrate(CommandSender sender, String[] args) {
        if (!has(sender, "abestcrates.crateitem")) {
            messageService.send(sender, "no-permission");
            return;
        }
        if (args.length < 4) {
            messageService.send(sender, "usage", Map.of("%usage%", "/abestcrates givecrate <player> <crate> <amount>"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            messageService.send(sender, "player-offline", Map.of("%player%", args[1]));
            return;
        }
        int amount = parseAmount(args[3]);
        crateManager.getCrate(args[2]).ifPresentOrElse(crate -> {
            int remaining = amount;
            while (remaining > 0) {
                int stackAmount = Math.min(crate.getBlockMaterial().getMaxStackSize(), remaining);
                target.getInventory().addItem(keyManager.createCrateItem(crate, stackAmount)).values()
                    .forEach(item -> target.getWorld().dropItemNaturally(target.getLocation(), item));
                remaining -= stackAmount;
            }
            messageService.send(sender, "crate-items-given", Map.of(
                "%player%", target.getName(), "%crate%", crate.getId(), "%amount%", String.valueOf(amount)));
        }, () -> messageService.send(sender, "crate-missing", Map.of("%crate%", args[2])));
    }

    private void giveAll(CommandSender sender, String[] args) {
        if (!has(sender, "abestcrates.givekey")) {
            messageService.send(sender, "no-permission");
            return;
        }
        if (args.length < 3) {
            messageService.send(sender, "usage", Map.of("%usage%", "/abestcrates giveall <crate> <amount> [physical|virtual]"));
            return;
        }
        int amount = parseAmount(args[2]);
        boolean virtual = args.length >= 4 && args[3].equalsIgnoreCase("virtual");
        crateManager.getCrate(args[1]).ifPresentOrElse(crate -> {
            int players = 0;
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (virtual) {
                    keyManager.addVirtualKeys(target, crate.getId(), amount);
                } else {
                    givePhysicalKeys(target, crate, amount);
                }
                players++;
            }
            messageService.send(sender, "keys-given-all", Map.of(
                "%amount%", String.valueOf(amount), "%crate%", crate.getId(),
                "%players%", String.valueOf(players), "%type%", virtual ? "virtual" : "physical"));
        }, () -> messageService.send(sender, "crate-missing", Map.of("%crate%", args[1])));
    }

    private void addKeys(CommandSender sender, String[] args) {
        changeVirtualKeys(sender, args, true);
    }

    private void removeKeys(CommandSender sender, String[] args) {
        changeVirtualKeys(sender, args, false);
    }

    private void changeVirtualKeys(CommandSender sender, String[] args, boolean add) {
        if (!has(sender, "abestcrates.givekey")) {
            messageService.send(sender, "no-permission");
            return;
        }
        if (args.length < 4) {
            String usage = add ? "/abestcrates addkeys <player> <crate> <amount>" : "/abestcrates removekeys <player> <crate> <amount>";
            messageService.send(sender, "usage", Map.of("%usage%", usage));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        int amount = parseAmount(args[3]);
        crateManager.getCrate(args[2]).ifPresentOrElse(crate -> {
            if (add) {
                keyManager.addVirtualKeys(target, crate.getId(), amount);
                messageService.send(sender, "virtual-keys-added", Map.of("%player%", target.getName() == null ? args[1] : target.getName(), "%crate%", crate.getId(), "%amount%", String.valueOf(amount)));
            } else {
                keyManager.removeVirtualKeys(target.getUniqueId(), crate.getId(), amount);
                messageService.send(sender, "virtual-keys-removed", Map.of("%player%", target.getName() == null ? args[1] : target.getName(), "%crate%", crate.getId(), "%amount%", String.valueOf(amount)));
            }
        }, () -> messageService.send(sender, "crate-missing", Map.of("%crate%", args[2])));
    }

    private void forceOpen(CommandSender sender, String[] args) {
        if (!has(sender, "abestcrates.open")) {
            messageService.send(sender, "no-permission");
            return;
        }
        if (args.length < 3) {
            messageService.send(sender, "usage", Map.of("%usage%", "/abestcrates forceopen <player> <crate>"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            messageService.send(sender, "player-offline", Map.of("%player%", args[1]));
            return;
        }
        crateManager.getCrate(args[2]).ifPresentOrElse(crate -> openingService.forceOpen(target, crate),
            () -> messageService.send(sender, "crate-missing", Map.of("%crate%", args[2])));
    }

    private void sendHelp(CommandSender sender) {
        Arrays.asList(
            "&5ABestCrates &7commands:",
            "&f/abestcrates &8- &7Open GUI",
            "&f/abestcrates gui",
            "&f/abestcrates create <name>",
            "&f/abestcrates delete <name>",
            "&f/abestcrates deletecrate",
            "&f/abestcrates spawncrate <name>",
            "&f/abestcrates edit <name>",
            "&f/abestcrates givekey <player> <crate> <amount>",
            "&f/abestcrates giveall <crate> <amount> [physical|virtual]",
            "&f/abestcrates givecrate <player> <crate> <amount>",
            "&f/abestcrates addkeys <player> <crate> <amount>",
            "&f/abestcrates removekeys <player> <crate> <amount>",
            "&f/abestcrates forceopen <player> <crate>",
            "&f/abestcrates reload"
        ).forEach(line -> sender.sendMessage(ColorUtil.component(line)));
    }

    private boolean has(CommandSender sender, String permission) {
        return sender.hasPermission("abestcrates.admin") || sender.hasPermission(permission);
    }

    private int parseAmount(String value) {
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (NumberFormatException exception) {
            return 1;
        }
    }

    private List<String> crateNames() {
        return crateManager.getCrates().stream().map(Crate::getId).toList();
    }

    private List<String> playerNames() {
        return Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .toList();
    }

    private List<String> filter(List<String> values, String prefix) {
        String normalizedPrefix = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix)) {
                result.add(value);
            }
        }
        return result;
    }

    private void givePhysicalKeys(Player target, Crate crate, int amount) {
        int remaining = amount;
        int maxStack = Math.max(1, crate.getKeyDefinition().getMaterial().getMaxStackSize());
        while (remaining > 0) {
            int stackAmount = Math.min(maxStack, remaining);
            target.getInventory().addItem(keyManager.createPhysicalKey(crate, stackAmount)).values()
                .forEach(item -> target.getWorld().dropItemNaturally(target.getLocation(), item));
            remaining -= stackAmount;
        }
    }
}
