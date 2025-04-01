package com.epicplayera10.joinguard.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.HelpCommand;
import co.aikar.commands.annotation.Subcommand;
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

@CommandAlias("joinguard")
@CommandPermission("joinguard.admin")
public class JoinGuardCommand extends BaseCommand {
    @HelpCommand
    public void doHelp(CommandSender sender, CommandHelp help) {
        help.showHelp();
    }

    @Subcommand("reload")
    @Description("Przeładuj konfigurację")
    public void reload(CommandSender sender) {
        sender.sendMessage("Reloading configuration...");
        JoinGuard.instance().reloadConfiguration();
        sender.sendMessage("Configuration reloaded!");
    }

    @Subcommand("whitelist add")
    @Description("Dodaj gracza do whitelisty")
    public void whitelistAdd(CommandSender sender, String playerName) {
        JoinGuard.instance().pluginConfiguration().whitelistedNicks.add(playerName);
        JoinGuard.instance().pluginConfiguration().save();
        sender.sendMessage("Player added to whitelist!");
    }

    @Subcommand("whitelist remove")
    @Description("Usuń gracza z whitelisty")
    public void whitelistRemove(CommandSender sender, String playerName) {
        JoinGuard.instance().pluginConfiguration().whitelistedNicks.remove(playerName);
        JoinGuard.instance().pluginConfiguration().save();
        sender.sendMessage("Player removed from whitelist!");
    }

    @Subcommand("whitelist list")
    @Description("Wyświetl listę graczy na whitelistcie")
    public void whitelistList(CommandSender sender) {
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
    @Description("Zaloguj się przez Discorda, aby funkcja zgłoszeń działała")
    public void login(CommandSender sender) {
        CompletableFuture.runAsync(() -> {
            String status = JoinGuardAPI.getServerRegistrationStatus().join();
            if (status.equals("ok")) {
                sender.sendMessage(ChatUtils.colorize("&aJesteś już zalogowany!"));
            } else if (status.equals("Invalid API key")) {
                String url = "https://joinguard.raidvm.com/api/register?state=" + JoinGuard.instance().pluginConfiguration().serverId;

                if (sender instanceof Player) {
                    BaseComponent component = new TextComponent(TextComponent.fromLegacyText(ChatUtils.colorize("&b&nKliknij tutaj aby się zarejestrować!")));
                    component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
                    sender.spigot().sendMessage(component);
                } else {
                    sender.sendMessage(ChatUtils.colorize("&b&nKliknij tutaj aby się zarejestrować!"));
                    sender.sendMessage(ChatUtils.colorize("&7" + url));
                }
            } else {
                sender.sendMessage(ChatUtils.colorize("&cWystąpił błąd podczas łączenia z serwerem!"));
            }
        });
    }

    @Subcommand("report")
    @Description("Zgłoś gracza")
    public void reportPlayer(Player player, OnlinePlayer reportedPlayer) {
        CompletableFuture.runAsync(() -> {
            JsonObject report = createReportJson(reportedPlayer.getPlayer(), player).join();
            String encodedReport = Base64.getEncoder().encodeToString(report.toString().getBytes());
            String url = "https://joinguard.raidvm.com/login/oauth2?state=" + encodedReport;

            BaseComponent component = new TextComponent(TextComponent.fromLegacyText(ChatUtils.colorize("&b&nKliknij tutaj aby wysłać zgłoszenie!")));
            component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
            player.spigot().sendMessage(component);
        });
    }

    private CompletableFuture<JsonObject> createReportJson(Player reported, Player reporter) {
        return CompletableFuture.supplyAsync(() -> {
            // Reporter
            JsonObject reportedJson = new JsonObject();
            reportedJson.addProperty("nick", reported.getName());
            reportedJson.addProperty("uuid", reported.getUniqueId().toString());
            reportedJson.addProperty("ip", reported.getAddress().getHostString());
            // Reported
            JsonObject reportedPlayerJson = new JsonObject();
            reportedPlayerJson.addProperty("nick", reporter.getName());
            // Server data
            JsonObject serverJson = new JsonObject();
            serverJson.addProperty("ip", JoinGuard.getServerIp().join());
            serverJson.addProperty("port", Bukkit.getPort());

            // Bind this data
            JsonObject reportJson = new JsonObject();
            reportJson.add("reported", reportedJson);
            reportJson.add("reporting", reportedPlayerJson);
            reportJson.add("server", serverJson);

            return reportJson;
        });
    }
}
