package com.shootingstar.command;

import com.shootingstar.ShootingStarPlugin;
import com.shootingstar.manager.EventScheduler;
import com.shootingstar.manager.StarEventManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /shootingstar <spawn|reload|stop|status>
 * Aliases: /ss /star
 */
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
            case "spawn"  -> handleSpawn(sender);
            case "stop"   -> handleStop(sender);
            case "reload" -> handleReload(sender);
            case "status" -> handleStatus(sender);
            default       -> { sendHelp(sender, label); yield true; }
        };
    }

    private boolean handleSpawn(CommandSender sender) {
        if (eventManager.isEventActive()) {
            sender.sendMessage(Component.text("A star event is already running! Use /ss stop first.")
                    .color(NamedTextColor.RED));
            return true;
        }
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
        eventManager.getMessageUtil();   // MessageUtil reads from config on each call
        eventManager.getMessageUtil().send(sender, "plugin-reloaded");
        return true;
    }

    private boolean handleStatus(CommandSender sender) {
        if (!eventManager.isEventActive()) {
            sender.sendMessage(Component.text("No active star event.")
                    .color(NamedTextColor.GRAY));
            sender.sendMessage(Component.text("Next event scheduled: " + (scheduler.isScheduled() ? "yes" : "no"))
                    .color(NamedTextColor.GRAY));
            return true;
        }
        var star   = eventManager.getActiveStar();
        var impact = star.getTargetLocation();
        sender.sendMessage(Component.text("★ Active Star Event")
                .color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("  Variant : " + star.getVariant().getDisplayName())
                .color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  Phase   : " + star.getPhase().name())
                .color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  Target  : X " + impact.getBlockX() + "  Z " + impact.getBlockZ())
                .color(NamedTextColor.YELLOW));
        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(Component.text("── ShootingStar Commands ──").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/" + label + " spawn  — trigger a star event now").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/" + label + " stop   — cancel the current event").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/" + label + " reload — reload config.yml").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/" + label + " status — show event status").color(NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (args.length == 1) {
            return List.of("spawn", "stop", "reload", "status").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
