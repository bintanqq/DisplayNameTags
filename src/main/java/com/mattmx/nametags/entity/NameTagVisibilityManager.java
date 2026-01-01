package com.mattmx.nametags.entity;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages player preferences for nametag visibility
 */
public class NameTagVisibilityManager {
    private final @NotNull JavaPlugin plugin;
    private final @NotNull Map<UUID, Boolean> visibilityMap = new HashMap<>();
    private final @NotNull File dataFile;
    private @NotNull FileConfiguration config;

    public NameTagVisibilityManager(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "player-visibility.yml");
        loadData();
    }

    private void loadData() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create player-visibility.yml: " + e.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(dataFile);

        // Load all saved preferences
        if (config.contains("players")) {
            for (String key : config.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    boolean hidden = config.getBoolean("players." + key);
                    visibilityMap.put(uuid, hidden);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in player-visibility.yml: " + key);
                }
            }
        }
    }

    public void saveData() {
        for (Map.Entry<UUID, Boolean> entry : visibilityMap.entrySet()) {
            config.set("players." + entry.getKey().toString(), entry.getValue());
        }

        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save player-visibility.yml: " + e.getMessage());
        }
    }

    /**
     * Set whether a player's nametag should be hidden
     * @param player The player
     * @param hidden True to hide, false to show
     */
    public void setHidden(@NotNull Player player, boolean hidden) {
        visibilityMap.put(player.getUniqueId(), hidden);
        saveData();
    }

    /**
     * Check if a player's nametag is hidden
     * @param player The player
     * @return True if hidden, false otherwise
     */
    public boolean isHidden(@NotNull Player player) {
        return visibilityMap.getOrDefault(player.getUniqueId(), false);
    }

    /**
     * Check if a player's nametag is hidden by UUID
     * @param uuid The player's UUID
     * @return True if hidden, false otherwise
     */
    public boolean isHidden(@NotNull UUID uuid) {
        return visibilityMap.getOrDefault(uuid, false);
    }

    /**
     * Remove a player's preference (returns to default visibility)
     * @param player The player
     */
    public void remove(@NotNull Player player) {
        visibilityMap.remove(player.getUniqueId());
        config.set("players." + player.getUniqueId().toString(), null);
        saveData();
    }
}