package net.syrupstudios.syrupessentials.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.syrupstudios.syrupessentials.util.TeleportPos;

import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class Warps extends Locations {
    private final WorldData worldData;
    private boolean requireUpdate;

    @Override
    protected void update() {
        this.requireUpdate = true;
    }

    public boolean requiresUpdate(){
        return this.requireUpdate;
    }

    public Warps(WorldData worldData, Map<String, TeleportPos> destinations){
        super(new HashMap<>(destinations));
        this.worldData = worldData;
    }

    public Warps(WorldData worldData) {
        super(new HashMap<>());
        this.worldData = worldData;
    }

    public static final Codec<Warps> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            WorldData.CODEC.fieldOf("worldData").forGetter(warps -> warps.worldData),
            Codec.unboundedMap(Codec.STRING, TeleportPos.CODEC)
                        .fieldOf("destinations")
                        .forGetter(Locations::getDestinations))
            .apply(builder, Warps::new));

}
