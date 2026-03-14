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
public class Homes extends Locations {
    private final PlayerData playerData;

    public Homes(PlayerData playerData, Map<String, TeleportPos> destinations) {
        super(destinations);
        this.playerData = playerData;
    }
    public Homes(PlayerData playerData){
        super(new HashMap<>());
        this.playerData = playerData;
    }

    public void addHome(String name, TeleportPos pos) {
        this.addLocation(name, pos);
    }

    public boolean removeHome(String name) {
        return this.removeLocation(name);
    }

    @Override
    protected void update() {
        playerData.triggerUpdate();
    }

    public static final Codec<Homes> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            PlayerData.CODEC.fieldOf("player").forGetter(homes -> homes.playerData),
            Codec.unboundedMap(Codec.STRING, TeleportPos.CODEC)
                    .fieldOf("destinations")
                    .forGetter(Locations::getDestinations)
    ).apply(builder, Homes::new));

}
