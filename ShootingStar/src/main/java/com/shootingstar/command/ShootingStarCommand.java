package com.shootingstar.command;

import com.shootingstar.ShootingStarPlugin;
import com.shootingstar.manager.EventScheduler;
import com.shootingstar.manager.StarEventManager;
import com.shootingstar.model.StarVariant;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.List;

public class ShootingStarCommand implements CommandExecutor, TabCompleter {

    private final ShootingStarPlugin plugin;
    private final StarEventManager   eventManager;
    private final EventScheduler     scheduler;

    public ShootingStarCommand(ShootingStarPlugin plugin,
                               StarEventManager eventManager,
                               EventScheduler scheduler) {
        this.plugin       = plugin;
        this.eventManager = eventManager;
        this.scheduler    = scheduler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("shootingstar.admin")) {
            eventManager.getMessageUtil().send(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "spawn"  -> handleSpawn(sender, args);
            case "stop"   -> handleStop(sender);
            case "reload" -> handleReload(sender);
            case "status" -> handleStatus(sender);
            default       -> { sendHelp(sender, label); yield true; }
        };
    }

    private boolean handleSpawn(CommandSender sender, String[] args) {
        if (eventManager.isEventActive()) {
            sender.sendMessage(Component.text("A star event is already running! Use /ss stop first.")
                    .color(NamedTextColor.RED));
            return true;
        }

        // /ss spawn <NORMAL|BLUE|RED|RAINBOW>
        if (args.length >= 2) {
            StarVariant variant;
            try {
                variant = StarVariant.valueOf(args[1].toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage(Component.text("Unknown variant. Choose: NORMAL, BLUE, RED, RAINBOW")
                        .color(NamedTextColor.RED));
                return true;
            }
            boolean ok = eventManager.startEventWithVariant(variant);
            if (ok) {
                sender.sendMessage(Component.text("[ShootingStar] Spawned a " + variant.getDisplayName() + " star!")
                        .color(NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("Failed to start event — check console for details.")
                        .color(NamedTextColor.RED));
            }
            return true;
        }

        // No variant — pick randomly
        boolean ok = eventManager.startEvent();
        if (ok) {
            eventManager.getMessageUtil().send(sender, "star-spawned");
        } else {
            sender.sendMessage(Component.text("Failed to start event — check console for details.")
                    .color(NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleStop(CommandSender sender) {
        if (!eventManager.isEventActive()) {
            sender.sendMessage(Component.text("No star event is currently running.")
                    .color(NamedTextColor.GRAY));
            return true;
        }
        eventManager.cancelCurrentEvent();
        scheduler.scheduleNext();
        eventManager.getMessageUtil().send(sender, "star-stopped");
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        plugin.reloadConfig();
        eventManager.getMessageUtil().send(sender, "plugin-reloaded");
        return true;
    }

    private boolean handleStatus(CommandSender sender) {
        if (!eventManager.isEventActive()) {
            sender.sendMessage(Component.text("No active star event.").color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text("Next event scheduled: " + (scheduler.isScheduled() ? "yes" : "no"))
                    .color(NamedTextColor.GRAY));
            return true;
        }
        var star   = eventManager.getActiveStar();
        var impact = star.getTargetLocation();
        sender.sendMessage(Component.text("★ Active Star Event").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("  Variant : " + star.getVariant().getDisplayName()).color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  Phase   : " + star.getPhase().name()).color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  Target  : X " + impact.getBlockX() + "  Z " + impact.getBlockZ()).color(NamedTextColor.YELLOW));
        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(Component.text("── ShootingStar Commands ──").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/" + label + " spawn               — random star").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/" + label + " spawn <variant>     — NORMAL, BLUE, RED, RAINBOW").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/" + label + " stop                — cancel current event").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/" + label + " reload              — reload config.yml").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/" + label + " status              — show event status").color(NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("spawn", "stop", "reload", "status").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
            return Arrays.stream(StarVariant.values())
                    .map(v -> v.name().toLowerCase())
                    .filter(s -> s.startsWith(args[1].toLowerCase())).toList();
        }
        return List.of();
    }
}
