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
    @Comment("Standby mode - if enabled, the plugin will not block players from joining, will only log attempts")
    public boolean standbyMode = false;
}
