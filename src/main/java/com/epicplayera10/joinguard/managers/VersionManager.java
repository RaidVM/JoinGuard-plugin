package com.epicplayera10.joinguard.managers;

import com.epicplayera10.joinguard.JoinGuard;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class VersionManager implements Listener {
    private static String latestVersion = null;
    private static String currentVersion = null;
    private static boolean updateAvailable = false;

    public static void initialize() {
        currentVersion = JoinGuard.instance().getDescription().getVersion();
        checkForUpdates();
        
        // Register join listener for update notifications
        Bukkit.getPluginManager().registerEvents(new VersionManager(), JoinGuard.instance());
        
        // Schedule periodic update checks (every 6 hours)
        Bukkit.getScheduler().runTaskTimerAsynchronously(
            JoinGuard.instance(),
            VersionManager::checkForUpdates,
            20 * 60 * 60 * 6, // Initial delay: 6 hours
            20 * 60 * 60 * 6  // Repeat every 6 hours
        );
    }

    public static CompletableFuture<Void> checkForUpdates() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        CompletableFuture.runAsync(() -> {
            HttpClient client = JoinGuard.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/repos/RaidVM/JoinGuard-plugin/releases/latest"))
                .header("Accept", "application/json")
                .build();

            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JsonObject releaseObj = JoinGuard.GSON.fromJson(response.body(), JsonObject.class);
                    latestVersion = releaseObj.get("tag_name").getAsString();
                    
                    // Compare versions directly without prefix handling
                    updateAvailable = !currentVersion.equals(latestVersion);
                    
                    if (updateAvailable) {
                        JoinGuard.instance().getLogger().info("A new version of JoinGuard is available: " + latestVersion);
                    }
                    
                    future.complete(null);
                } else {
                    JoinGuard.instance().getLogger().warning("Failed to check for updates. Status code: " + response.statusCode());
                    future.complete(null);
                }
            } catch (IOException | InterruptedException e) {
                JoinGuard.instance().getLogger().log(Level.WARNING, "Failed to check for updates", e);
                future.completeExceptionally(e);
            }
        });
        
        return future;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (updateAvailable && player.hasPermission("joinguard.notify.updatecheck")) {
            Bukkit.getScheduler().runTaskLater(JoinGuard.instance(), () -> {
                player.sendMessage("§6[JoinGuard] §eA new version is available: §6" + latestVersion);
                player.sendMessage("§6[JoinGuard] §eCurrent version: §6" + currentVersion);
                player.sendMessage("§6[JoinGuard] §eDownload from: §6https://github.com/RaidVM/JoinGuard-plugin/releases/latest");
            }, 40L); // Send message 2 seconds after join
        }
    }
    
    public static boolean isUpdateAvailable() {
        return updateAvailable;
    }
    
    public static String getLatestVersion() {
        return latestVersion;
    }
    
    public static String getCurrentVersion() {
        return currentVersion;
    }
} 