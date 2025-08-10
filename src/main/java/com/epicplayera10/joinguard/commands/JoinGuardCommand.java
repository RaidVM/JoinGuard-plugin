package com.epicplayera10.joinguard.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import com.epicplayera10.joinguard.JoinGuard;
import com.epicplayera10.joinguard.utils.ChatUtils;
import com.epicplayera10.joinguard.utils.JoinGuardAPI;
import com.google.gson.JsonObject;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Base64;
import java.util.concurrent.CompletableFuture;

@CommandAlias("joinguard|jg")
@CommandPermission("joinguard.admin")
public class JoinGuardCommand extends BaseCommand {
    @HelpCommand
    public void doHelp(CommandSender sender, CommandHelp help) {
        help.showHelp();
    }

    @Subcommand("reload")
    @Description("Reload the configuration")
    public void reload(CommandSender sender) {
        sender.sendMessage("Reloading configuration...");
        JoinGuard.instance().reloadConfiguration();
        sender.sendMessage("Configuration reloaded!");
    }

    @Subcommand("whitelist add")
    @Syntax("<player>")
    @CommandCompletion("@not_whitelisted")
    @Description("Add a player to the whitelist")
    public void whitelistAdd(CommandSender sender, String playerName) {
        if (JoinGuard.instance().pluginConfiguration().whitelistedNicks.contains(playerName)) {
            sender.sendMessage(ChatUtils.colorize("&cThat player is already on the whitelist"));
            return;
        }
        JoinGuard.instance().pluginConfiguration().whitelistedNicks.add(playerName);
        JoinGuard.instance().pluginConfiguration().save();
        sender.sendMessage("Added " + playerName + " to the whitelist");
    }

    @Subcommand("whitelist remove")
    @Syntax("<player>")
    @CommandCompletion("@whitelist")
    @Description("Remove a player from the whitelist")
    public void whitelistRemove(CommandSender sender, String playerName) {
        if (!JoinGuard.instance().pluginConfiguration().whitelistedNicks.contains(playerName)) {
            sender.sendMessage(ChatUtils.colorize("&cThat player is not on the whitelist"));
            return;
        }
        JoinGuard.instance().pluginConfiguration().whitelistedNicks.remove(playerName);
        JoinGuard.instance().pluginConfiguration().save();
        sender.sendMessage("Removed " + playerName + " from the whitelist");
    }

    @Subcommand("whitelist list")
    @Description("Show the list of whitelisted players")
    public void whitelistList(CommandSender sender) {
        if (JoinGuard.instance().pluginConfiguration().whitelistedNicks.isEmpty()) {
            sender.sendMessage("There are no players on the whitelist");
            return;
        }
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (String nick : JoinGuard.instance().pluginConfiguration().whitelistedNicks) {
            if (!first) {
                builder.append(", ");
            }
            builder.append(nick);
            first = false;
        }
        sender.sendMessage("Whitelisted players: " + builder);
    }

    @Subcommand("login")
    @Description("Log in via Discord to enable the report feature")
    public void login(CommandSender sender) {
        CompletableFuture.runAsync(() -> {
            String status = JoinGuardAPI.getServerRegistrationStatus().join();
            if (status.equals("ok")) {
                sender.sendMessage(ChatUtils.colorize("&aYou are already logged in!"));
            } else if (status.equals("Invalid API key")) {
                String url = "https://joinguard.raidvm.com/api/register?state=" + JoinGuard.instance().pluginConfiguration().serverId;

                if (sender instanceof Player) {
                    BaseComponent component = new TextComponent(TextComponent.fromLegacyText(ChatUtils.colorize("&b&nClick here to register!")));
                    component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
                    sender.spigot().sendMessage(component);
                } else {
                    sender.sendMessage(ChatUtils.colorize("&b&nClick here to register!"));
                    sender.sendMessage(ChatUtils.colorize("&7" + url));
                }
            } else {
                sender.sendMessage(ChatUtils.colorize("&cAn error occurred while connecting to the server!"));
            }
        });
    }

    @Subcommand("report")
    @Syntax("<player>")
    @Description("Report a player")
    public void reportPlayer(Player player, OnlinePlayer reportedPlayer) {
        CompletableFuture.runAsync(() -> {
            JsonObject report = createReportJson(reportedPlayer.getPlayer(), player).join();
            String encodedReport = Base64.getEncoder().encodeToString(report.toString().getBytes());
            String url = "https://joinguard.raidvm.com/login/oauth2?state=" + encodedReport;

            BaseComponent component = new TextComponent(TextComponent.fromLegacyText(ChatUtils.colorize("&b&nClick here to send the report!")));
            component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
            player.spigot().sendMessage(component);
        });
    }

    private CompletableFuture<JsonObject> createReportJson(Player reported, Player reporter) {
        return CompletableFuture.supplyAsync(() -> {
            // Reported player
            JsonObject reportedJson = new JsonObject();
            reportedJson.addProperty("nick", reported.getName());
            reportedJson.addProperty("uuid", reported.getUniqueId().toString());
            reportedJson.addProperty("ip", reported.getAddress().getHostString());
            // Reporter
            JsonObject reporterJson = new JsonObject();
            reporterJson.addProperty("nick", reporter.getName());
            // Server data
            JsonObject serverJson = new JsonObject();
            serverJson.addProperty("ip", JoinGuard.getServerIp().join());
            serverJson.addProperty("port", Bukkit.getPort());

            // Combine the data
            JsonObject reportJson = new JsonObject();
            reportJson.add("reported", reportedJson);
            reportJson.add("reporting", reporterJson);
            reportJson.add("server", serverJson);

            return reportJson;
        });
    }
}
