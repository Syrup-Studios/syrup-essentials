package net.syrupstudios.syrupessentials.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.syrupstudios.syrupessentials.data.PlayerData;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class TeleportManager {
    static final HashMap<UUID, TeleportRequest> APPROVED_TELEPORTS = new HashMap<>();
    static final HashMap<UUID, TeleportRequest> TELEPORT_APPROVAL_REQUESTS = new HashMap<>();
    private MinecraftServer minecraftServer;
    private static long currentTick;

    public TeleportManager(MinecraftServer minecraftServer){
        this.minecraftServer = minecraftServer;
    }

    public static int teleportRequest(ServerPlayer serverPlayer, MinecraftServer server, String destinationPlayerName){
        ServerPlayer target = server.getPlayerList().getPlayerByName(destinationPlayerName);
        if (target!=null) {
            TELEPORT_APPROVAL_REQUESTS.put(
                    serverPlayer.getUUID(),
                    new TeleportRequest(
                            new TeleportPos(target.level(), target.blockPosition(), target.getXRot(), target.getYRot()),
                            target.getUUID(),
                            serverPlayer,
                            currentTick + 600 //replace with config timeout eventually
                    ));
            target.sendSystemMessage(Component.literal("Receiving teleport request from: "+serverPlayer.getDisplayName().getString()));
            target.sendSystemMessage(Component.literal("Type /tpaccept or /tpdeny to respond.."));
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
            TELEPORT_APPROVAL_REQUESTS.remove(receiver.getUUID());
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
            possibleTeleportRequest.get().getSenderPlayer().sendSystemMessage(Component.literal("Teleport Request Denied."));
            receiver.sendSystemMessage(Component.literal("Teleport Request Denied."));
            TELEPORT_APPROVAL_REQUESTS.remove(receiver.getUUID());
            return 1;
        }
        receiver.sendSystemMessage(Component.literal("No currently pending teleports found."));
        return 0;
    }

    public static boolean teleportPlayer(TeleportPos tpos, ServerPlayer serverPlayer, boolean addToTeleportHistory){
        PlayerData player = DataManager.getOrCreate(serverPlayer).orElseThrow();
        if(addToTeleportHistory) {
            player.addTeleportHistory(serverPlayer);
        }
        serverPlayer.teleportTo(
                Objects.requireNonNull(serverPlayer.getServer()).getLevel(tpos.getDimensionId()),
                tpos.getPos().getX()+0.5,
                tpos.getPos().getY(),
                tpos.getPos().getZ()+0.5,
                tpos.getYaw(),
                tpos.getPitch()
        );
        return true;
    }

    public void onServerTick(){
        currentTick++;
        TELEPORT_APPROVAL_REQUESTS.entrySet().removeIf(
                entry -> currentTick >= entry.getValue().getExpiresAtTick());

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
}
