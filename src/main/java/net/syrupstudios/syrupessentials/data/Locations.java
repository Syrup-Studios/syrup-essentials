package net.syrupstudios.syrupessentials.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.syrupstudios.syrupessentials.util.TeleportPos;

import java.util.HashMap;
import java.util.Map;


@Data
@AllArgsConstructor
public abstract class Locations {
    private HashMap<String, TeleportPos> destinations;

    public void addLocation(String name, TeleportPos teleportPos){
        name = name.toLowerCase();
        destinations.put(name,teleportPos);
        update();
    }

    public boolean removeLocation(String name){
        if(destinations.remove(name) == null){
            return false;
        }
        update();
        return true;
    }

    public String listNames(){
        return String.join("\n", destinations.keySet());
    }

    protected abstract void update();
}
