package com.epicplayera10.joinguard.listeners;

import com.epicplayera10.joinguard.JoinGuard;
import com.epicplayera10.joinguard.managers.BlocklistManager;
import com.epicplayera10.joinguard.utils.ChatUtils;
import com.epicplayera10.joinguard.utils.JoinGuardAPI;
import com.google.common.hash.Hashing;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class LoginListener implements Listener {
    @EventHandler
    public void onLogin(AsyncPlayerPreLoginEvent event) {
        // Check if player is whitelisted
        if (JoinGuard.instance().pluginConfiguration().whitelistedNicks.contains(event.getName())) {
            return;
        }

        String hashedIp = hashIp(event.getAddress().getHostAddress());

        // Check if player is blocked
        if (BlocklistManager.getBlockedNicknames().contains(event.getName()) ||
            BlocklistManager.getBlockedUuids().contains(event.getUniqueId().toString()) ||
            BlocklistManager.getBlockedIpHashes().contains(hashedIp)
        ) {
            // Disallow player from joining
            String kickMessage = ChatUtils.colorize(
                JoinGuard.instance().pluginConfiguration().messages.blockedMessage
            );
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, kickMessage);
            // Send an attempt message
            JoinGuardAPI.sendAttemptMessage(event.getName(), event.getUniqueId(), event.getAddress().getHostAddress()).whenComplete((v, e) -> {
                System.out.println("done");
                if (e != null) {
                    JoinGuard.instance().getLogger().log(Level.SEVERE, "Failed to send attempt message", e);
                }
            });
        }
    }

    private String hashIp(String ip) {
        return Hashing.sha512().hashString(ip, StandardCharsets.UTF_8).toString();
    }
}
