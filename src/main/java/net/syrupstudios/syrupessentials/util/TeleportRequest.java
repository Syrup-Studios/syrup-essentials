package net.syrupstudios.syrupessentials.util;

import lombok.Data;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

@Data
public class TeleportRequest {
    private UUID receiverPlayerUUID;
    private ServerPlayer senderPlayer;
    private long expiresAtTick;
    private TeleportRequestType teleportRequestType;

    public TeleportRequest(
            UUID receiverPlayerUUID,
            ServerPlayer senderPlayer,
            long expiresAtTick,
            boolean isTpaHere
    ) {
        this.receiverPlayerUUID = receiverPlayerUUID;
        this.senderPlayer = senderPlayer;
        this.expiresAtTick = expiresAtTick;
        this.teleportRequestType = isTpaHere ? TeleportRequestType.TPAHERE : TeleportRequestType.TPA;
    }

    public boolean isTpaHere(){
        return teleportRequestType.equals(TeleportRequestType.TPAHERE);
    }
}

enum TeleportRequestType {
    TPA,       // sender teleports to receiver
    TPAHERE    // receiver teleports to sender
}
