package net.syrupstudios.syrupessentials.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {
    private final Map<UUID, PlayerData> playerDataMap;
    private final File dataDirectory;

    public PlayerDataManager(MinecraftServer server) {
        this.playerDataMap = new HashMap<>();

        // Save in the world folder: <world>/syrup_essential_data/
        // Get the world directory from the overworld
        File worldDirectory = server.getRunDirectory();
        if (server.getOverworld() != null) {
            worldDirectory = server.getOverworld().getServer().getRunDirectory();
        }

        // Navigate to saves/<world_name>/syrup_essential_data
        File savesDir = new File(worldDirectory, "saves");
        if (server.getSaveProperties() != null) {
            String worldName = server.getSaveProperties().getLevelName();
            File worldDir = new File(savesDir, worldName);
            this.dataDirectory = new File(worldDir, "syrup_essential_data");
        } else {
            // Fallback
            this.dataDirectory = new File(worldDirectory, "syrup_essential_data");
        }

        if (!dataDirectory.exists()) {
            dataDirectory.mkdirs();
        }

        net.syrupstudios.syrupessentials.SyrupEssentials.LOGGER.info("Player data directory: {}", dataDirectory.getAbsolutePath());
    }

    public PlayerData getPlayerData(UUID uuid) {
        return playerDataMap.computeIfAbsent(uuid, this::loadPlayerData);
    }

    public PlayerData getPlayerData(ServerPlayerEntity player) {
        return getPlayerData(player.getUuid());
    }

    private PlayerData loadPlayerData(UUID uuid) {
        File playerFile = new File(dataDirectory, uuid.toString() + ".snbt");

        if (playerFile.exists()) {
            try {
                String snbtContent = Files.readString(playerFile.toPath());
                NbtCompound nbt = StringNbtReader.parse(snbtContent);
                return PlayerData.fromNbt(nbt);
            } catch (Exception e) {
                net.syrupstudios.syrupessentials.SyrupEssentials.LOGGER.error("Failed to load player data for " + uuid, e);
            }
        }

        return new PlayerData(uuid);
    }

    public void savePlayerData(UUID uuid) {
        PlayerData data = playerDataMap.get(uuid);
        if (data == null) return;

        File playerFile = new File(dataDirectory, uuid.toString() + ".snbt");

        try {
            NbtCompound nbt = data.toNbt();
            String snbtContent = nbt.asString(); // Convert to SNBT string

            // Write to file with nice formatting
            try (FileWriter writer = new FileWriter(playerFile)) {
                writer.write(snbtContent);
            }
        } catch (IOException e) {
            net.syrupstudios.syrupessentials.SyrupEssentials.LOGGER.error("Failed to save player data for " + uuid, e);
        }
    }

    public void savePlayerData(ServerPlayerEntity player) {
        savePlayerData(player.getUuid());
    }

    public void saveAll() {
        net.syrupstudios.syrupessentials.SyrupEssentials.LOGGER.info("Saving all player data...");
        for (UUID uuid : playerDataMap.keySet()) {
            savePlayerData(uuid);
        }
    }
}