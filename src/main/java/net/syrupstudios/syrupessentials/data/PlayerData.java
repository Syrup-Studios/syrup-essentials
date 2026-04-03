package net.syrupstudios.syrupessentials.data;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Data;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.syrupstudios.syrupessentials.util.TeleportPos;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Data
public class PlayerData {
    private static final Map<UUID, PlayerData> PLAYERS = new HashMap<>();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_HISTORY_LOCATIONS = 10;
    private final UUID playerId;
    private final String playerName;
    private TeleportPos lastLocation;
    private Homes homes;
    private LinkedList<TeleportPos> locationHistory;
    private boolean update;
    private boolean isMuted;
    private boolean canFly;
    private boolean godmode;
    private String nickname;

    public PlayerData(UUID playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.update = false;
        this.lastLocation = null;
        this.homes = new Homes(this);
        this.locationHistory = new LinkedList<>();
        this.isMuted = false;
        this.canFly = false;
        this.nickname = "";
    }

    public PlayerData(UUID playerId, String playerName, TeleportPos lastLocation, Homes homes, boolean isMuted, boolean canFly, boolean godmode, String nickname) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.lastLocation = lastLocation;
        this.homes = homes;
        this.isMuted = isMuted;
        this.canFly = canFly;
        this.godmode = godmode;
        this.nickname = nickname;
    }

    public static final Codec<PlayerData> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            UUIDUtil.CODEC.fieldOf("playerId").forGetter(playerData -> playerData.playerId),
            Codec.STRING.fieldOf("playerName").forGetter(playerData ->playerData.playerName),
            TeleportPos.CODEC.fieldOf("lastLocation").forGetter(playerData -> playerData.lastLocation),
            Homes.CODEC.fieldOf("homes").forGetter(playerData -> playerData.homes),
            Codec.BOOL.fieldOf("isMuted").forGetter(playerData -> playerData.isMuted),
            Codec.BOOL.fieldOf("canFly").forGetter(playerData -> playerData.canFly),
            Codec.BOOL.fieldOf("godmode").forGetter(playerData -> playerData.godmode),
            Codec.STRING.fieldOf("nickname").forGetter(playerData ->playerData.nickname)
            ).apply(builder, PlayerData::new));

    public Optional<TeleportPos> getLastSeenPos() {
        return Optional.ofNullable(lastLocation);
    }

    public void triggerUpdate(){
        this.update = true;
    }

    public static void addTeleportHistory(ServerPlayer player, ResourceKey<Level> dimension, BlockPos pos) {
        getOrCreate(player).ifPresent(data -> data.addTeleportHistory(new TeleportPos(dimension, pos)));
    }

    public void addTeleportHistory(ServerPlayer player) {
        addTeleportHistory(player, player.level().dimension(), player.blockPosition());
    }

    public void addTeleportHistory(TeleportPos pos) {
        locationHistory.add(pos);

        while (locationHistory.size() > MAX_HISTORY_LOCATIONS) {
            locationHistory.removeFirst();
        }

        triggerUpdate();
    }

    public void addHome(String name, ServerPlayer serverPlayer){
        this.homes.addHome(name, new TeleportPos(serverPlayer.level(), serverPlayer.blockPosition()));
    }

    public void popLocationHistory() {
        if (!locationHistory.isEmpty()) {
            locationHistory.removeLast();
            triggerUpdate();
        }
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

    public void readNbt(CompoundTag tag){
        Optional<PlayerData> readPlayer = CODEC.parse(NbtOps.INSTANCE, tag)
                .resultOrPartial(LOGGER::error);

        if (readPlayer.isPresent()){
            PlayerData playerData = readPlayer.get();
            this.lastLocation = playerData.lastLocation;
            this.homes = playerData.homes;
            this.isMuted = playerData.isMuted;
            this.canFly = playerData.canFly;
            this.godmode = playerData.godmode;
            this.nickname = playerData.nickname;
        }
    }

    public CompoundTag writeNbt() {
        CompoundTag tag = new CompoundTag();
        tag.put("playerData",CODEC.encodeStart(NbtOps.INSTANCE, this).resultOrPartial(LOGGER::error).orElse(null));
        return tag;
    }

}
