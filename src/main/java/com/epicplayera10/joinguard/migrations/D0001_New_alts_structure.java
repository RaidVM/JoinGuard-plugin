package com.epicplayera10.joinguard.migrations;

import com.epicplayera10.joinguard.config.DataConfiguration;
import eu.okaeri.configs.migrate.builtin.NamedMigration;
import eu.okaeri.configs.migrate.view.RawConfigView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static eu.okaeri.configs.migrate.ConfigMigrationDsl.*;

public class D0001_New_alts_structure extends NamedMigration {
    public D0001_New_alts_structure() {
        super(
            "New alts structure",
            (config, view) -> {
                if (!view.exists("playerAlts")) {
                    return false;
                }

                Map<String, List<UUID>> ipToUuids = (Map<String, List<UUID>>) view.get("playerAlts");
                Map<String, Map<String, Object>> playerAltsNew = new HashMap<>();
                // Map the old structure to the new one
                for (var entry : ipToUuids.entrySet()) {
                    String ip = entry.getKey();
                    List<UUID> uuids = entry.getValue();

                    playerAltsNew.put(ip, Map.of(
                        "uuids", uuids,
                        "lastLogin", System.currentTimeMillis()
                    ));
                }
                view.set("playerAltsNew", playerAltsNew);
                // Remove old field
                view.remove("playerAlts");

                return true;
            }
        );
    }
}
