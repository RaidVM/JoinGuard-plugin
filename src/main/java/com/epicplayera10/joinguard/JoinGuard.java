package com.epicplayera10.joinguard;

import co.aikar.commands.PaperCommandManager;
import com.epicplayera10.joinguard.commands.JoinGuardCommand;
import com.epicplayera10.joinguard.config.ConfigurationFactory;
import com.epicplayera10.joinguard.config.DataConfiguration;
import com.epicplayera10.joinguard.config.PluginConfiguration;
import com.epicplayera10.joinguard.listeners.LoginListener;
import com.epicplayera10.joinguard.managers.BlocklistManager;
import com.epicplayera10.joinguard.managers.VersionManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class JoinGuard extends JavaPlugin {
    public static final Gson GSON = new Gson();
    private final File pluginConfigurationFile = new File(this.getDataFolder(), "config.yml");
    private final File dataConfigurationFile = new File(this.getDataFolder(), "data.yml");

    private PluginConfiguration pluginConfiguration;
    private DataConfiguration dataConfiguration;

    private static JoinGuard instance;

    @Override
    public void onEnable() {
        instance = this;

        // Metrics
        setupMetrics();

        this.pluginConfiguration = ConfigurationFactory.createPluginConfiguration(this.pluginConfigurationFile);
        this.dataConfiguration = ConfigurationFactory.createDataConfiguration(this.dataConfigurationFile);

        registerCommands();

        Bukkit.getPluginManager().registerEvents(new LoginListener(), this);

        // Setup blocklist manager
        this.getLogger().info("Fetching blocklists...");
        BlocklistManager.updateAll().join();
        this.getLogger().info("Blocklists fetched!");
        Bukkit.getScheduler().runTaskTimerAsynchronously(JoinGuard.instance(), BlocklistManager::updateAll,
            0,
            20 * 60 * 3 // Run task every 3 minutes
        );

        // Initialize version manager for update checking
        VersionManager.initialize();
    }

    @Override
    public void onDisable() {
        // Save data configuration
        this.dataConfiguration.save();
    }

    private void registerCommands() {
        PaperCommandManager manager = new PaperCommandManager(this);

        manager.enableUnstableAPI("help");

        manager.getCommandCompletions().registerCompletion("not_whitelisted",
                c -> Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> !pluginConfiguration.whitelistedNicks.contains(name))
                        .collect(Collectors.toList()));

        manager.getCommandCompletions().registerCompletion("whitelist",
                c -> pluginConfiguration.whitelistedNicks);

        manager.registerCommand(new JoinGuardCommand());
    }

    public static JoinGuard instance() {
        return instance;
    }

    public PluginConfiguration pluginConfiguration() {
        return pluginConfiguration;
    }
    
    public DataConfiguration dataConfiguration() {
        return dataConfiguration;
    }

    public void reloadConfiguration() {
        this.pluginConfiguration.load();
        this.dataConfiguration.load();
    }

    public static HttpClient newHttpClient() {
        return HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(60))
            .build();
    }

    public static CompletableFuture<String> getServerIp() {
        return CompletableFuture.supplyAsync(() -> {
            HttpClient client = newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://ipinfo.io/json"))
                .header("Accept", "application/json")
                .build();


            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                return GSON.fromJson(response.body(), JsonObject.class).get("ip").getAsString();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void setupMetrics() {
        int pluginId = 25361;
        Metrics metrics = new Metrics(this, pluginId);
    }
}
