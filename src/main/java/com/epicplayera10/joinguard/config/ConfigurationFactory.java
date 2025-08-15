package com.epicplayera10.joinguard.config;

import com.epicplayera10.joinguard.JoinGuard;
import com.epicplayera10.joinguard.migrations.D0001_New_alts_structure;
import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.serdes.commons.SerdesCommons;
import eu.okaeri.configs.validator.okaeri.OkaeriValidator;
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import eu.okaeri.configs.yaml.bukkit.serdes.SerdesBukkit;

import java.io.File;

public class ConfigurationFactory {

    private ConfigurationFactory(){
    }

    public static PluginConfiguration createPluginConfiguration(File file) {
        return ConfigManager.create(PluginConfiguration.class, (it) -> {
            it.withConfigurer(new OkaeriValidator(new YamlBukkitConfigurer()));
            it.withSerdesPack(registry -> {
                registry.register(new SerdesCommons());
                registry.register(new SerdesBukkit());
            });

            it.withBindFile(file);
            it.withLogger(JoinGuard.instance().getLogger());
            it.saveDefaults();
            it.load(true);
        });
    }
    
    public static DataConfiguration createDataConfiguration(File file) {
        return ConfigManager.create(DataConfiguration.class, (it) -> {
            it.withConfigurer(new OkaeriValidator(new YamlBukkitConfigurer()));
            it.withSerdesPack(registry -> {
                registry.register(new SerdesCommons());
                registry.register(new SerdesBukkit());
            });

            it.withBindFile(file);
            it.withLogger(JoinGuard.instance().getLogger());
            it.saveDefaults();
            it.load(true);

            it.migrate(
                new D0001_New_alts_structure()
            );
        });
    }
}
