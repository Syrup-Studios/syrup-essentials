package net.syrupstudios.syrupessentials.util;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.syrupstudios.syrupessentials.data.PlayerData;
import net.syrupstudios.syrupessentials.data.WorldData;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private final File dataDirectory;
    private final File worldDirectory;
    private final File playerDirectory;
    private static final Logger LOGGER = LogUtils.getLogger();
    private final MinecraftServer minecraftServer;
    private static final Map<UUID, PlayerData> PLAYERS = new HashMap<>();
    private WorldData worldData;
    @Nullable
    private static DataManager INSTANCE;

    public DataManager(MinecraftServer server) {
        this.dataDirectory = server.getServerDirectory().toPath().resolve(MOD_PATH).toFile();
        this.worldDirectory = dataDirectory.toPath().resolve(WORLD_PATH).toFile();
        this.playerDirectory = dataDirectory.toPath().resolve(PLAYER_PATH).toFile();
        this.minecraftServer = server;
        
        mkDirsIfNotExisting(dataDirectory);
        mkDirsIfNotExisting(worldDirectory);
        mkDirsIfNotExisting(playerDirectory);
    }

    public static DataManager getInstance(){
        return Objects.requireNonNull(INSTANCE);
    }

    private void mkDirsIfNotExisting(File file) {
        if(!file.exists()){
            file.mkdirs();
        }
    }

    public void loadPlayer(UUID playerUUID) {
        Path path = Paths.get(PLAYER_PATH, playerUUID + ".snbt");
        PlayerData playerData = getOrCreate(minecraftServer,playerUUID).orElseThrow();
        if (Files.exists(path)) {
            try {
                File playerFile = playerDirectory.toPath().resolve(playerUUID+".snbt").toFile();
                playerData.readNbt(NbtIo.read(playerFile));
                PLAYERS.put(playerUUID, playerData);
            } catch (Exception e) {
                LOGGER.error("Error while reading player data: {}", e.getMessage());
            }
        }
        else {
            try {
                Files.createFile(path);
                PLAYERS.put(playerUUID, playerData);
            } catch (Exception e) {
                LOGGER.error("Error while creating player data: {}", e.getMessage());
            }
        }
    }

    public void savePlayer(PlayerData playerData) {
        if(playerData.isUpdate()){
            File playerFile = playerDirectory.toPath().resolve(playerData.getPlayerId()+".snbt").toFile();
            try{
                NbtIo.write(playerData.writeNbt(),playerFile);
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        }
    }

    public void loadWorld(WorldData worldData){
        File worldFile = worldDirectory.toPath().resolve(DATA_FILE).toFile();
        if(worldFile.exists()) {
            try{
                worldData.readNbt(NbtIo.read(worldFile));
            } catch (Exception e) {
                LOGGER.error("Error while reading world data: {}", e.getMessage());
            }
        }
    }

    public static String formatNbt(CompoundTag nbt) {
        String snbt = nbt.toString();
        StringBuilder formatted = new StringBuilder();
        int indent = 0;
        int listDepth = 0;
        int compactDepth = 0;
        boolean inQuote = false;
        char lastChar = 0;

        for (int i = 0; i < snbt.length(); i++) {
            char c = snbt.charAt(i);

            if (c == '"' && lastChar != '\\') {
                inQuote = !inQuote;
            }

            if (!inQuote) {
                if (compactDepth > 0) {
                    if (c == '}' || c == ']') {
                        compactDepth--;
                        formatted.append(c);
                    } else if (c == '{' || c == '[') {
                        compactDepth++;
                        formatted.append(c);
                    } else if (c == ',') {
                        formatted.append(", ");
                    } else if (c == ':') {
                        formatted.append(": ");
                    } else {
                        formatted.append(c);
                    }
                }
                else {
                    if (c == '{') {
                        if (listDepth > 0) {
                            compactDepth = 1;
                            formatted.append(c);
                        } else {
                            formatted.append("{\n");
                            indent++;
                            appendIndent(formatted, indent);
                        }
                    } else if (c == '[') {
                        if (i + 2 < snbt.length() && snbt.charAt(i + 2) == ';') {
                            compactDepth = 1;
                            formatted.append(c);
                        } else {
                            formatted.append("[\n");
                            indent++;
                            listDepth++;
                            appendIndent(formatted, indent);
                        }
                    } else if (c == '}') {
                        formatted.append("\n");
                        indent--;
                        appendIndent(formatted, indent);
                        formatted.append(c);
                    } else if (c == ']') {
                        formatted.append("\n");
                        indent--;
                        listDepth--;
                        appendIndent(formatted, indent);
                        formatted.append(c);
                    } else if (c == ',') {
                        formatted.append(",\n");
                        appendIndent(formatted, indent);
                    } else if (c == ':') {
                        formatted.append(": ");
                    } else {
                        formatted.append(c);
                    }
                }
            } else {
                formatted.append(c);
            }
            lastChar = c;
        }
        return formatted.toString();
    }

    private static void appendIndent(StringBuilder sb, int indent) {
        sb.append("  ".repeat(Math.max(0, indent)));
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
