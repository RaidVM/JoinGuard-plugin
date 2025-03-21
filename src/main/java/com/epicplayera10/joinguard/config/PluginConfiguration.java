package com.epicplayera10.joinguard.config;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Header;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Header("~-~-~-~-~-~-~-~-~-~-~- #")
@Header("       JoinGuard       #")
@Header("~-~-~-~-~-~-~-~-~-~-~- #")
public class PluginConfiguration extends OkaeriConfig {
    @Comment("")
    @Comment("Server id - DO NOT TOUCH THIS")
    public String serverId = UUID.randomUUID().toString();

    @Comment("")
    @Comment("Whitelisted players that are allowed to join the server even if they are blocked")
    public List<String> whitelistedNicks = new ArrayList<>();

    @Comment("")
    @Comment("Messages configuration in MiniMessage format https://docs.advntr.dev/minimessage/format")
    public Messages messages = new Messages();

    public static class Messages extends OkaeriConfig {
        public String blockedMessage = "&cThis server is protected by JoinGuard";
    }
}
