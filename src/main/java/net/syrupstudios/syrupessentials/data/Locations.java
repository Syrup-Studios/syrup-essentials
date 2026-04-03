package net.syrupstudios.syrupessentials.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.syrupstudios.syrupessentials.util.TeleportPos;

import java.util.Map;
import java.util.stream.Collectors;


@Data
@AllArgsConstructor
public abstract class Locations {
    private Map<String, TeleportPos> destinations;

    public void addLocation(String name, TeleportPos teleportPos){
        name = name.toLowerCase();
        destinations.put(name,teleportPos);
        update();
    }

    public boolean removeLocation(String name){
        if(destinations.remove(name.toLowerCase()) == null){
            return false;
        }
        update();
        return true;
    }

    public CompoundTag writeNBT() {
        CompoundTag tag = new CompoundTag();
        destinations.forEach((name, destination) -> tag.put(name, destination.toNBT()));
        return tag;
    }


    public void readNBT(CompoundTag tag) {
        destinations.clear();
        for (String key : tag.getAllKeys()) {
            destinations.put(key, TeleportPos.fromNBT(tag.getCompound(key)));
        }
    }

    public String listNames(){
        return String.join(System.lineSeparator(), destinations.keySet());
    }


    protected abstract void update();
}
