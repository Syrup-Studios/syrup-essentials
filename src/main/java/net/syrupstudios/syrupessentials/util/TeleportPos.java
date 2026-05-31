package net.syrupstudios.syrupessentials.util;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Data;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

@Data
public class TeleportPos {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ResourceKey<Level> dimensionId;
    private final Vec3 pos;
    private final float yaw;
    private final float pitch;
    private final long time;

    public static final Codec<TeleportPos> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dim").forGetter(p -> p.dimensionId),
            Vec3.CODEC.fieldOf("pos").forGetter(TeleportPos::getPos),
            Codec.FLOAT.fieldOf("yaw").forGetter(p -> p.yaw),
            Codec.FLOAT.fieldOf("pitch").forGetter(p -> p.pitch),
            Codec.LONG.fieldOf("time").forGetter(p -> p.time)
    ).apply(builder, TeleportPos::new));

    public TeleportPos(ResourceKey<Level> dimensionId, Vec3 pos, float pitch, float yaw) {
        this.dimensionId = dimensionId;
        this.pos = pos;
        this.time = System.currentTimeMillis();
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public TeleportPos(ResourceKey<Level> dimensionId, Vec3 blockPos, float yaw, float pitch, Long time) {
        this.dimensionId = dimensionId;
        this.pos = blockPos;
        this.yaw = yaw;
        this.pitch = pitch;
        this.time = time;
    }

    public TeleportPos(Level world, Vec3 p, float pitch, float yaw) {
        this(world.dimension(), p, pitch, yaw);
    }
 }
