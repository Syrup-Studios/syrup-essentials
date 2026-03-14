package net.syrupstudios.syrupessentials.util;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Data;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

@Data
public class TeleportPos {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ResourceKey<Level> dimensionId;
    private final BlockPos pos;
    private final long time;

    public static final Codec<TeleportPos> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dim").forGetter(p -> p.dimensionId),
            BlockPos.CODEC.fieldOf("pos").forGetter(TeleportPos::getPos),
            Codec.LONG.fieldOf("time").forGetter(p -> p.time)
    ).apply(builder, TeleportPos::new));

    public TeleportPos(ResourceKey<Level> dimensionId, BlockPos pos) {
        this.dimensionId = dimensionId;
        this.pos = pos;
        this.time = System.currentTimeMillis();
    }

    public TeleportPos(ResourceKey<Level> dimensionId, BlockPos blockPos, Long aLong) {
        this.dimensionId = dimensionId;
        this.pos = blockPos;
        this.time = aLong;
    }

    public TeleportPos(Level world, BlockPos p) {
        this(world.dimension(), p);
    }

    public static TeleportPos fromNBT(CompoundTag tag) {
        return CODEC.parse(NbtOps.INSTANCE, tag)
                .resultOrPartial(LOGGER::error)
                .orElse(null);
    }

    public Tag toNBT() {
        return CODEC.encodeStart(NbtOps.INSTANCE, this).resultOrPartial(LOGGER::error).orElse(null);
    }
}
