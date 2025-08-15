package com.epicplayera10.joinguard.listeners;

import com.epicplayera10.joinguard.JoinGuard;
import com.epicplayera10.joinguard.config.DataConfiguration;
import com.epicplayera10.joinguard.managers.BlocklistManager;
import com.epicplayera10.joinguard.utils.ChatUtils;
import com.epicplayera10.joinguard.utils.JoinGuardAPI;
import com.google.common.hash.Hashing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class LoginListener implements Listener {
    private static final long ALT_TRACKING_EXPIRATION = 1000L * 60 * 60 * 24 * 30; // 30 days

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        String playerName = event.getName();
        UUID playerUuid = event.getUniqueId();
        String ipAddress = event.getAddress().getHostAddress();

        if (event.getAddress().isLoopbackAddress()) {
            // Ignore loopback addresses (localhost)
            JoinGuard.instance().getLogger().info("Ignoring loopback address for player: " + playerName);
            return;
        }

        // Track alts
        trackAltAccount(ipAddress, playerUuid);

        String hashedIp = hashIp(ipAddress);
        boolean isPlayerBlocked = isPlayerBlocked(playerName, playerUuid, hashedIp);

        // Check if the player is blocked
        if (isPlayerBlocked) {
            if (JoinGuard.instance().pluginConfiguration().standbyMode) {
                // Standby mode
                Bukkit.broadcast(
                    Component.text("[JoinGuard] ").color(NamedTextColor.AQUA)
                        .append(Component.text("Player " + playerName + " is on JoinGuard!").color(NamedTextColor.RED)),
                    "joinguard.notify.join"
                );
            } else {
                // Check if player is whitelisted
                if (!JoinGuard.instance().pluginConfiguration().whitelistedNicks.contains(playerName)) {
                    // Disallow player from joining
                    String kickMessage = ChatUtils.colorize("&cThis server is protected by JoinGuard");
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, kickMessage);
                }
            }
            // Send the attempt message
            JoinGuardAPI.sendAttemptMessage(playerName, playerUuid, ipAddress).whenComplete((v, e) -> {
                if (e != null) {
                    JoinGuard.instance().getLogger().log(Level.SEVERE, "Failed to send attempt message", e);
                }
            });
            return;
        }

        // Alt detection
        DataConfiguration.PlayerAlts playerAlts = JoinGuard.instance().dataConfiguration().playerAltsNew.getOrDefault(ipAddress, new DataConfiguration.PlayerAlts());
        if (playerAlts.uuids.size() > 1) {
            boolean hasBlockedAlt = containsBlockedAlt(playerAlts.uuids);

            if (hasBlockedAlt) {
                // Send the alt detection message
                JoinGuardAPI.sendAltDetectionMessage(playerName, playerUuid, ipAddress, playerAlts.uuids).whenComplete((v, e) -> {
                    if (e != null) {
                        JoinGuard.instance().getLogger().log(Level.SEVERE, "Failed to send alt detection message", e);
                    }
                });
            }
        }
    }

    @EventHandler
    public void onLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        if (JoinGuard.instance().pluginConfiguration().standbyMode) {
            if (isPlayerBlocked(player.getName(), player.getUniqueId(), event.getAddress().getHostAddress())) {
                // Add metadata to player if they are blocked in standby mode
                player.setMetadata("on_joinguard", new FixedMetadataValue(JoinGuard.instance(), true));
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
        if (!Bukkit.getServer().getOnlineMode()) {
            // If offline-mode, then also check nicknames
            if (BlocklistManager.getBlockedNicknames().contains(playerName)) {
                return true;
            }
        }

        return BlocklistManager.getBlockedUuids().contains(playerUuid.toString()) || BlocklistManager.getBlockedIpHashes().contains(hashedIp);
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
    private synchronized void trackAltAccount(String ipAddress, UUID uuid) {
        clearExpiredAlts();

        DataConfiguration.PlayerAlts alts = JoinGuard.instance().dataConfiguration().playerAltsNew.getOrDefault(ipAddress, new DataConfiguration.PlayerAlts());

        alts.lastLogin = System.currentTimeMillis();

        // Add the UUID if it doesn't exist already
        if (!alts.uuids.contains(uuid)) {
            alts.uuids.add(uuid);
        }
        JoinGuard.instance().dataConfiguration().save();
    }

    private void clearExpiredAlts() {
        long currentTime = System.currentTimeMillis();
        JoinGuard.instance().dataConfiguration().playerAltsNew.entrySet().removeIf(entry -> {
            DataConfiguration.PlayerAlts alts = entry.getValue();
            return currentTime - alts.lastLogin > ALT_TRACKING_EXPIRATION;
        });
    }

    private String hashIp(String ip) {
        return Hashing.sha512().hashString(ip, StandardCharsets.UTF_8).toString();
    }
}
