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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class LoginListener implements Listener {
    @EventHandler
    public void onLogin(AsyncPlayerPreLoginEvent event) {
        String playerName = event.getName();
        UUID playerUuid = event.getUniqueId();
        String ipAddress = event.getAddress().getHostAddress();

        // Track alts
        trackAltAccount(ipAddress, playerUuid);
        
        // Check if player is whitelisted
        if (JoinGuard.instance().pluginConfiguration().whitelistedNicks.contains(playerName)) {
            return;
        }

        String hashedIp = hashIp(ipAddress);
        boolean isPlayerBlocked = isPlayerBlocked(playerName, playerUuid, hashedIp);

        // Check if the player is blocked
        if (isPlayerBlocked) {
            // Disallow player from joining
            String kickMessage = ChatUtils.colorize(
                JoinGuard.instance().pluginConfiguration().messages.blockedMessage
            );
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, kickMessage);

            // Send the attempt message
            JoinGuardAPI.sendAttemptMessage(playerName, playerUuid, ipAddress).whenComplete((v, e) -> {
                if (e != null) {
                    JoinGuard.instance().getLogger().log(Level.SEVERE, "Failed to send attempt message", e);
                }
            });
            return;
        }

        // Alt detection
        List<UUID> altAccounts = JoinGuard.instance().dataConfiguration().playerAlts.getOrDefault(ipAddress, new ArrayList<>());
        if (altAccounts.size() > 1) {
            boolean hasBlockedAlt = containsBlockedAlt(altAccounts);

            if (hasBlockedAlt) {
                // Send the alt detection message
                JoinGuardAPI.sendAltDetectionMessage(playerName, playerUuid, ipAddress, altAccounts).whenComplete((v, e) -> {
                    if (e != null) {
                        JoinGuard.instance().getLogger().log(Level.SEVERE, "Failed to send alt detection message", e);
                    }
                });
            }
        }
    }
    
    /**
     * Checks if a player is blocked based on nickname, UUID, or IP
     * 
     * @param playerName Player's nickname
     * @param playerUuid Player's UUID as string
     * @param hashedIp Player's hashed IP address
     * @return true if player is blocked, false otherwise
     */
    private boolean isPlayerBlocked(String playerName, UUID playerUuid, String hashedIp) {
        return BlocklistManager.getBlockedNicknames().contains(playerName) ||
               BlocklistManager.getBlockedUuids().contains(playerUuid.toString()) ||
               BlocklistManager.getBlockedIpHashes().contains(hashedIp);
    }
    
    /**
     * Checks if any of the alt accounts are blocked
     * 
     * @param altAccounts List of alt account UUIDs
     * @return true if any alt is blocked, false otherwise
     */
    private boolean containsBlockedAlt(List<UUID> altAccounts) {
        for (UUID uuid : altAccounts) {
            if (BlocklistManager.getBlockedUuids().contains(uuid.toString())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Track an alt account
     * @param ipAddress Player's IP address
     * @param uuid Player's UUID
     */
    private void trackAltAccount(String ipAddress, UUID uuid) {
        List<UUID> alts = JoinGuard.instance().dataConfiguration().playerAlts.getOrDefault(ipAddress, new ArrayList<>());
        
        // Add the UUID if it doesn't exist already
        if (!alts.contains(uuid)) {
            alts.add(uuid);
            JoinGuard.instance().dataConfiguration().playerAlts.put(ipAddress, alts);
            JoinGuard.instance().dataConfiguration().save();
        }
    }

    private String hashIp(String ip) {
        return Hashing.sha512().hashString(ip, StandardCharsets.UTF_8).toString();
    }
}
