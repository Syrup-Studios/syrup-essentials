package net.syrupstudios.syrupessentials.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

@Data
@AllArgsConstructor
public class TeleportRequest {
    private TeleportPos teleportPos;
    private UUID receiverPlayerUUID;
    private ServerPlayer senderPlayer;
    private long expiresAtTick;
}
