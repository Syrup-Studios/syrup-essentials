package net.syrupstudios.syrupessentials.util;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.syrupstudios.syrupessentials.data.PlayerData;
import net.syrupstudios.syrupessentials.data.WorldData;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class DataManager {
    private static final String MOD_PATH = "syrup_essential_data";
    private static final String WORLD_PATH = "world_data";
    private static final String DATA_FILE = "data.snbt";
    private static final String PLAYER_PATH = "player_data";
    private static final String CONFIG_PATH = "config";
    private final File dataDirectory;
    private final File worldDirectory;
    private final File playerDirectory;
    private final File configDirectory;
    private static final Logger LOGGER = LogUtils.getLogger();
    private final MinecraftServer minecraftServer;
    private static final Map<UUID, PlayerData> PLAYERS = new HashMap<>();
    private static final Map<UUID, TeleportPos> PENDING_TELEPORTS = new HashMap<>();
    private WorldData worldData;
    @Nullable
    private static DataManager INSTANCE;

    public DataManager(MinecraftServer server) {
        this.dataDirectory = server.getServerDirectory().toPath().resolve(MOD_PATH).toFile();
        this.worldDirectory = dataDirectory.toPath().resolve(WORLD_PATH).toFile();
        this.playerDirectory = dataDirectory.toPath().resolve(PLAYER_PATH).toFile();
        this.configDirectory = dataDirectory.toPath().resolve(CONFIG_PATH).toFile();
        this.minecraftServer = server;
        
        mkDirsIfNotExisting(dataDirectory);
        mkDirsIfNotExisting(worldDirectory);
        mkDirsIfNotExisting(playerDirectory);
        mkDirsIfNotExisting(configDirectory);
    }

    public static DataManager getInstance(){
        return Objects.requireNonNull(INSTANCE);
    }

    private void mkDirsIfNotExisting(File file) {
        if(!file.exists()){
            file.mkdirs();
        }
    }

    public void loadPlayer(UUID playerUUID){
        Path path = playerDirectory.toPath().resolve(playerUUID + ".snbt");
        PlayerData playerData = getOrCreate(minecraftServer,playerUUID).orElseThrow();
        if (Files.exists(path)) {
            try {
                playerData.readNbt(TagParser.parseTag(new String(Files.readAllBytes(path))));
                PLAYERS.put(playerUUID, playerData);
            } catch (Exception e) {
                LOGGER.error("Error while reading player data: {}", e.toString());
            }
        }
        else {
            try {
                Files.createFile(path);
                PLAYERS.put(playerUUID, playerData);
            } catch (Exception e) {
                LOGGER.error("Error while creating player data: {}", e.toString());
            }
        }
    }

    public void savePlayer(PlayerData playerData) {
        playerData.checkForHomeUpdates();
        if(playerData.isUpdate()){
            Path playerFile = playerDirectory.toPath().resolve(playerData.getPlayerId()+".snbt");
            try{
                Files.writeString(playerFile, formatString(playerData.writeNbt().toString()));
                System.out.println("Saved Player File for "+playerData.getPlayerName());
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        }
    }

    public void savePlayers(MinecraftServer server){
        LOGGER.info("Saving all player data..");
        server.getPlayerList().getPlayers().stream().map(ServerPlayer::getUUID).forEach(
                UUID -> {
                    if(PLAYERS.containsKey(UUID)){
                        savePlayer(PLAYERS.get(UUID));
                    }
                }
        );
        LOGGER.info("All player data saved.");
    }

    private String formatString(String snbt) {
        String result = "";
        int indent = 0;
        boolean inQuote = false;
        boolean inTypedArray = false;
        char last = 0;
        for (int i = 0; i < snbt.length(); i++) {
            char c = snbt.charAt(i);
            if (c == '"' && last != '\\') inQuote = !inQuote;
            if (inQuote) {
                result += c;
            } else if (inTypedArray) {
                if (c == ',') result += ", ";
                else {
                    result += c;
                    if (c == ']') inTypedArray = false;
                }
            } else {
                switch (c) {
                    case '{' -> {
                        result += "{\n";
                        indent++;
                        for (int j = 0; j < indent; j++) result += "  ";
                    }
                    case '}' -> {
                        result += "\n";
                        indent--;
                        for (int j = 0; j < indent; j++) result += "  ";
                        result += "}";
                    }
                    case '[' -> {
                        if (i + 2 < snbt.length() && snbt.charAt(i + 2) == ';') {
                            result += "[";
                            inTypedArray = true;
                        } else {
                            result += "[\n";
                            indent++;
                            for (int j = 0; j < indent; j++) result += "  ";
                        }
                    }
                    case ']' -> {
                        result += "\n";
                        indent--;
                        for (int j = 0; j < indent; j++) result += "  ";
                        result += "]";
                    }
                    case ',' -> {
                        result += ",\n";
                        for (int j = 0; j < indent; j++) result += "  ";
                    }
                    case ':' -> result += ": ";
                    default -> result += c;
                }
            }
            last = c;
        }
        return result;
    }

    public static Optional<PlayerData> getOrCreate(MinecraftServer server, UUID playerId) {
        if (PLAYERS.containsKey(playerId)) {
            return Optional.of(PLAYERS.get(playerId));
        }

        // Check if the player file exists
        return server.getProfileCache().get(playerId)
                .map(profile -> PLAYERS.computeIfAbsent(playerId, k -> new PlayerData(playerId, profile.getName())));
    }

    public static Optional<PlayerData> getOrCreate(@Nullable Player player) {
        if (player == null) {
            return Optional.empty();
        }

        return Optional.of(PLAYERS.computeIfAbsent(player.getUUID(), k -> new PlayerData(player.getUUID(), player.getGameProfile().getName())));
    }

}
