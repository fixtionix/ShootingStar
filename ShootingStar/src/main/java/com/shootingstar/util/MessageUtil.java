package com.shootingstar.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * Handles MiniMessage-formatted message sending and broadcasting.
 */
public class MessageUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final FileConfiguration config;

    public MessageUtil(FileConfiguration config) {
        this.config = config;
    }

    /** Resolve a message key from config and replace placeholders. */
    public Component resolve(String key, String... replacements) {
        String raw = config.getString("messages." + key, "<red>Missing message: " + key + "</red>");
        // replacements are pairs: placeholder, value
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            raw = raw.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return MM.deserialize(raw);
    }

    public void send(CommandSender sender, String key, String... replacements) {
        sender.sendMessage(resolve(key, replacements));
    }

    public void broadcast(String key, String... replacements) {
        Component msg = resolve(key, replacements);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(msg);
        }
    }
}
