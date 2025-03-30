package com.epicplayera10.joinguard.utils;

import com.epicplayera10.joinguard.JoinGuard;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Class for interacting with the JoinGuard API
 */
public class JoinGuardAPI {
    /**
     * Check if server has been registered
     *
     * @return "ok" or "Invalid API key"
     */
    public static CompletableFuture<String> getServerRegistrationStatus() {
        HttpClient client = JoinGuard.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://joinguard.raidvm.com/api/check?api="+JoinGuard.instance().pluginConfiguration().serverId))
            .header("Accept", "application/json")
            .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            JsonObject json = JoinGuard.GSON.fromJson(response.body(), JsonObject.class);
            return json.get("status").getAsString();
        });
    }

    /**
     * Sends an attempt message to the JoinGuard API
     */
    public static CompletableFuture<Void> sendAttemptMessage(String nick, UUID uuid, String playerIp) {
        HttpClient client = JoinGuard.newHttpClient();

        JsonObject data = getServerInformation(nick, uuid, playerIp);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://joinguard.raidvm.com/api/attempt"))
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(data.toString()))
            .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {});
    }
    
    /**
     * Sends an alt account detection message to the JoinGuard API
     * @param nick Player nickname
     * @param uuid Player UUID
     * @param playerIp Player IP address
     * @param altUuids List of alt account UUIDs
     * @return CompletableFuture with void result
     */
    public static CompletableFuture<Void> sendAltDetectionMessage(String nick, UUID uuid, String playerIp, List<UUID> altUuids) {
        HttpClient client = JoinGuard.newHttpClient();

        JsonObject data = getServerInformation(nick, uuid, playerIp);
        
        // Add alt accounts
        JsonArray altsArray = new JsonArray();
        for (UUID altUuid : altUuids) {
            altsArray.add(altUuid.toString());
        }
        data.add("alts", altsArray);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://joinguard.raidvm.com/api/alts"))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(data.toString()))
            .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {});
    }

    private static JsonObject getServerInformation(String nick, UUID uuid, String playerIp) {
        JsonObject data = new JsonObject();

        // Player data
        JsonObject playerJson = new JsonObject();
        playerJson.addProperty("nick", nick);
        playerJson.addProperty("uuid", uuid.toString());
        playerJson.addProperty("ip", playerIp);
        // Server data
        JsonObject serverJson = new JsonObject();
        serverJson.addProperty("ip", JoinGuard.getServerIp().join());
        serverJson.addProperty("port", Bukkit.getPort());
        // Bind this data
        data.addProperty("api", JoinGuard.instance().pluginConfiguration().serverId);
        data.add("player", playerJson);
        data.add("server", serverJson);

        return data;
    }
}
