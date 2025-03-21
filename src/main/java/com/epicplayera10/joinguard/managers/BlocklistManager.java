package com.epicplayera10.joinguard.managers;

import com.epicplayera10.joinguard.JoinGuard;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

public class BlocklistManager {
    private static final List<String> blockedNicknames = new CopyOnWriteArrayList<>();
    private static final List<String> blockedUuids = new CopyOnWriteArrayList<>();
    private static final List<String> blockedIpHashes = new CopyOnWriteArrayList<>();

    /**
     * Updates all blocklists
     */
    public static CompletableFuture<Void> updateAll() {
        return CompletableFuture.allOf(
            update("name", blockedNicknames),
            update("uuid", blockedUuids),
            update("ip", blockedIpHashes)
        );
    }

    private static CompletableFuture<Void> update(String listName, List<String> list) {
        HttpClient client = JoinGuard.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://joinguard.raidvm.com/list/" + listName))
            .header("Accept", "application/json")
            .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .whenComplete((response, throwable) -> {
                if (throwable != null) {
                    JoinGuard.instance().getLogger().log(Level.SEVERE, "Failed to fetch blocklist", throwable);
                    if (client instanceof AutoCloseable) {
                        try {
                            ((AutoCloseable) client).close();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                    return;
                }

                if (response.statusCode() != 200) {
                    JoinGuard.instance().getLogger().severe("Failed to fetch blocklist: " + response.statusCode());
                    if (client instanceof AutoCloseable) {
                        try {
                            ((AutoCloseable) client).close();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                    return;
                }

                // Parse json
                JsonObject json = JoinGuard.GSON.fromJson(response.body(), JsonObject.class);
                List<String> newList = new ArrayList<>();
                for (JsonElement element : json.getAsJsonArray("data")) {
                    newList.add(element.getAsString());
                }

                // Replace list
                list.clear();
                list.addAll(newList);
                if (client instanceof AutoCloseable) {
                    try {
                        ((AutoCloseable) client).close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            })
            .thenApply((response) -> null);
    }

    public static List<String> getBlockedNicknames() {
        return Collections.unmodifiableList(blockedNicknames);
    }

    public static List<String> getBlockedUuids() {
        return Collections.unmodifiableList(blockedUuids);
    }

    public static List<String> getBlockedIpHashes() {
        return Collections.unmodifiableList(blockedIpHashes);
    }
}
