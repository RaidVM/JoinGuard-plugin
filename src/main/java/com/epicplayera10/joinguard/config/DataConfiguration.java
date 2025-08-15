package com.epicplayera10.joinguard.config;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Header;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Header("~-~-~-~-~-~-~-~-~-~-~- #")
@Header("  JoinGuard Data File  #")
@Header("~-~-~-~-~-~-~-~-~-~-~- #")
public class DataConfiguration extends OkaeriConfig {
    @Comment("Player IP addresses and their UUID alts")
    @Comment("Do not edit manually")
    public Map<String, PlayerAlts> playerAltsNew = new HashMap<>();

    public static class PlayerAlts extends OkaeriConfig {
        public List<UUID> uuids = new ArrayList<>();
        public long lastLogin = System.currentTimeMillis();
    }

    // Old structure
    //public Map<String, List<UUID>> playerAlts = new HashMap<>();
} 