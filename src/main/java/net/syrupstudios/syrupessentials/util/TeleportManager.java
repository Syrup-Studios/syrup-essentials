package net.syrupstudios.syrupessentials.util;

import lombok.NoArgsConstructor;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.syrupstudios.syrupessentials.data.PlayerData;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@NoArgsConstructor
public class TeleportManager {
    private static final HashMap<UUID, TeleportRequest> APPROVED_TELEPORTS = new HashMap<>();
    private static final HashMap<UUID, TeleportRequest> TELEPORT_APPROVAL_REQUESTS = new HashMap<>();
    private static final int TIMEOUT_THRESHOLD = 600; //TODO: replace w/ config timeout
    private static long currentTick;

    public static int teleportRequest(ServerPlayer serverPlayer, MinecraftServer server, String destinationPlayerName){
        ServerPlayer target = server.getPlayerList().getPlayerByName(destinationPlayerName);
        if (target!=null) {
            if(Objects.equals(destinationPlayerName, serverPlayer.getDisplayName().getString()))
            {
                serverPlayer.sendSystemMessage(Component.literal("You are already where you are...??"));
                return -1;
            }
            TELEPORT_APPROVAL_REQUESTS.put(
                    serverPlayer.getUUID(),
                    new TeleportRequest(
                            new TeleportPos(target.level(), target.position(), target.getXRot(), target.getYRot()),
                            target.getUUID(),
                            serverPlayer,
                            currentTick + TIMEOUT_THRESHOLD
                    ));
            target.sendSystemMessage(Component.literal("Receiving teleport request from: "+serverPlayer.getDisplayName().getString()));
            target.sendSystemMessage(Component.literal("Type /tpaccept or /tpdeny to respond"));
            return 1;
        }
        return -1;
    }

    public static int approveTeleportRequest(ServerPlayer receiver){
        Optional<TeleportRequest> possibleTeleportRequest =
                TELEPORT_APPROVAL_REQUESTS.values()
                        .stream()
                        .filter(tr -> tr.getReceiverPlayerUUID().equals(receiver.getUUID()))
                        .findFirst();
        if(possibleTeleportRequest.isPresent()){
            possibleTeleportRequest.get().getSenderPlayer().sendSystemMessage(Component.literal("Teleport Request Approved, Teleporting.."));
            receiver.sendSystemMessage(
                    Component.literal(
                            String.format("Teleport Request Approved, Teleporting %s to you..", possibleTeleportRequest.get().getSenderPlayer().getDisplayName().getString())));
            TeleportRequest request = possibleTeleportRequest.get();
            APPROVED_TELEPORTS.put(
                    request.getSenderPlayer().getUUID(),
                    request
            );
            TELEPORT_APPROVAL_REQUESTS.remove(request.getSenderPlayer().getUUID());
            return 1;
        }
        receiver.sendSystemMessage(Component.literal("Teleport Request Has Expired"));
        return 0;
    }

    public static int denyTeleportRequest(ServerPlayer receiver){
        Optional<TeleportRequest> possibleTeleportRequest =
                TELEPORT_APPROVAL_REQUESTS.values()
                        .stream()
                        .filter(tr -> tr.getReceiverPlayerUUID().equals(receiver.getUUID()))
                        .findFirst();
        if(possibleTeleportRequest.isPresent()){
            TeleportRequest request = possibleTeleportRequest.get();
            request.getSenderPlayer().sendSystemMessage(Component.literal("Teleport Request Denied."));
            receiver.sendSystemMessage(Component.literal("Teleport Request Denied."));
            TELEPORT_APPROVAL_REQUESTS.remove(request.getSenderPlayer().getUUID());
            return 1;
        }
        receiver.sendSystemMessage(Component.literal("No currently pending teleports found."));
        return 0;
    }

    public static boolean teleportPlayer(TeleportPos tpos, ServerPlayer serverPlayer, boolean addToTeleportHistory){
        PlayerData player = DataManager.getOrCreatePlayer(serverPlayer).orElseThrow();
        if(addToTeleportHistory) {
            player.addTeleportHistory(serverPlayer);
        }
        //TODO: delay here, if 5 seconds elapses without interruption proceed to teleport

        serverPlayer.teleportTo(
                Objects.requireNonNull(serverPlayer.getServer()).getLevel(tpos.getDimensionId()),
                tpos.getPos().x,
                tpos.getPos().y,
                tpos.getPos().z,
                tpos.getYaw(),
                tpos.getPitch()
        );
        return true;
    }

    public void onServerTick(){
        currentTick++;

        TELEPORT_APPROVAL_REQUESTS.entrySet().removeIf(
                entry -> {
                    if(currentTick >= entry.getValue().getExpiresAtTick()){
                        entry.getValue().getSenderPlayer().sendSystemMessage(Component.literal("Teleport Request Timed Out."));
                        return true;
                    }
                    return false;
                }
        );

        APPROVED_TELEPORTS.entrySet().removeIf(entry ->
                teleportPlayer(entry.getValue().getTeleportPos(), entry.getValue().getSenderPlayer(), true));
    }

    public void flush(){
        TELEPORT_APPROVAL_REQUESTS.clear();
        APPROVED_TELEPORTS.clear();
        currentTick =0;
    }
}
