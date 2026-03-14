package net.syrupstudios.syrupessentials.data;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Data;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import net.syrupstudios.syrupessentials.util.TeleportPos;
import org.slf4j.Logger;

import java.util.Optional;

@Data
public class WorldData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private Warps warps;

    public WorldData(){
        this.warps = new Warps(this);
    }

    public WorldData(Warps warps){
        this.warps = warps;
    }

    public static final Codec<WorldData> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            Warps.CODEC.fieldOf("warps").forGetter(worldData -> worldData.warps)).apply(builder, WorldData::new));

    public void readNbt(CompoundTag tag){
        Optional<WorldData> worldData = CODEC.parse(NbtOps.INSTANCE, tag)
                .resultOrPartial(LOGGER::error);

        worldData.ifPresent(data -> warps = data.getWarps());
    }

    public void createWarp(String name, ServerPlayer serverPlayer) {
        this.warps.createWarp(name, new TeleportPos(serverPlayer.level().dimension(), serverPlayer.blockPosition()));
    }

    public void deleteWarp(String name) {
        this.warps.removeWarp(name);
    }

}
