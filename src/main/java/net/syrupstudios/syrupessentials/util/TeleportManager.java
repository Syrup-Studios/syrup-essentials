package net.syrupstudios.syrupessentials.util;

import com.mojang.logging.LogUtils;
import lombok.NoArgsConstructor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.syrupstudios.syrupessentials.data.PlayerData;
import org.slf4j.Logger;

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
    private static final Logger LOGGER = LogUtils.getLogger();

    public static int teleportRequest(
            ServerPlayer sender,
            MinecraftServer server,
            String destinationPlayerName,
            boolean isTpaHere
    ) {
        ServerPlayer receiver = server.getPlayerList().getPlayerByName(destinationPlayerName);

        if (receiver == null) {
            return 0;
        }

        if (receiver.getUUID().equals(sender.getUUID())) {
            sender.sendSystemMessage(Component.literal("You are already where you are...??"));
            return 0;
        }

        TELEPORT_APPROVAL_REQUESTS.put(
                sender.getUUID(),
                new TeleportRequest(
                        receiver.getUUID(),
                        sender,
                        currentTick + TIMEOUT_THRESHOLD,
                        isTpaHere
                )
        );

        receiver.sendSystemMessage(createTeleportRequestMessage(sender, receiver, isTpaHere));

        return 1;
    }

    private static Component createTeleportRequestMessage(
            ServerPlayer sender,
            ServerPlayer receiver,
            boolean isTpaHere
    ) {
        String senderName = sender.getDisplayName().getString();
        String receiverName = receiver.getDisplayName().getString();

        MutableComponent header;

        if (isTpaHere) {
            header = Component.literal("Teleport-Here request: ")
                    .withStyle(ChatFormatting.WHITE)
                    .append(Component.literal("[ ").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(receiverName).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" ➤ ").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(senderName).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" ]").withStyle(ChatFormatting.WHITE));
        } else {
            header = Component.literal("Teleport request: ")
                    .withStyle(ChatFormatting.WHITE)
                    .append(Component.literal("[ ").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(senderName).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" ➤ ").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(receiverName).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" ]").withStyle(ChatFormatting.WHITE));
        }

        return header
                .append(Component.literal("\n Select an option: ")
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal("Accept ✔")
                        .withStyle(style -> style
                                .withColor(ChatFormatting.GREEN)
                                .withBold(true)
                                .withClickEvent(new ClickEvent(
                                        ClickEvent.Action.RUN_COMMAND,
                                        "/tpaccept " + sender.getUUID()
                                ))
                                .withHoverEvent(new HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        Component.literal("Accept teleport request")
                                ))
                        ))
                .append(Component.literal(" | ")
                        .withStyle(ChatFormatting.WHITE))
                .append(Component.literal("Deny ✘")
                        .withStyle(style -> style
                                .withColor(ChatFormatting.RED)
                                .withBold(true)
                                .withClickEvent(new ClickEvent(
                                        ClickEvent.Action.RUN_COMMAND,
                                        "/tpdeny " + sender.getUUID()
                                ))
                                .withHoverEvent(new HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        Component.literal("Deny teleport request")
                                ))
                        ));
    }

    public static int tpaRequest(ServerPlayer sender, MinecraftServer server, String targetName) {
        return teleportRequest(sender, server, targetName, false);
    }

    public static int tpaHereRequest(ServerPlayer sender, MinecraftServer server, String targetName) {
        return teleportRequest(sender, server, targetName, true);
    }

    public static int approveTeleportRequest(ServerPlayer receiver){
        Optional<TeleportRequest> possibleTeleportRequest =
                TELEPORT_APPROVAL_REQUESTS.values()
                        .stream()
                        .filter(tr -> tr.getReceiverPlayerUUID().equals(receiver.getUUID()))
                        .findFirst();
        return processTeleportRequestApproval(receiver, possibleTeleportRequest);
    }

    public static int approveTeleportRequestPlayer(ServerPlayer receiver, ServerPlayer target) {
        Optional<TeleportRequest> possibleTeleportRequest =
                TELEPORT_APPROVAL_REQUESTS.values()
                        .stream()
                        .filter(tr -> tr.getReceiverPlayerUUID().equals(receiver.getUUID()) && tr.getSenderPlayer().getUUID().equals(target.getUUID()))
                        .findFirst();
        return processTeleportRequestApproval(receiver, possibleTeleportRequest);
    }

    private static int processTeleportRequestApproval(
            ServerPlayer receiver,
            Optional<TeleportRequest> possibleTeleportRequest
    ) {
        if (possibleTeleportRequest.isPresent()) {
            TeleportRequest request = possibleTeleportRequest.get();

            sendApprovalMessages(receiver, request);

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

    private static void sendApprovalMessages(ServerPlayer receiver, TeleportRequest request) {
        ServerPlayer sender = request.getSenderPlayer();

        if (!request.isTpaHere()) {
            sender.sendSystemMessage(Component.literal("Teleport Request Approved, Teleporting.."));
            receiver.sendSystemMessage(Component.literal(
                    "Teleport Request Approved, Teleporting "
                            + sender.getDisplayName().getString()
                            + " to you.."
            ));
        } else if (request.isTpaHere()) {
            sender.sendSystemMessage(Component.literal(
                    receiver.getDisplayName().getString()
                            + " approved your request. Teleporting them to you.."
            ));
            receiver.sendSystemMessage(Component.literal(
                    "Teleport Request Approved, Teleporting you to "
                            + sender.getDisplayName().getString()
                            + ".."
            ));
        }
    }

    public static int denyTeleportRequest(ServerPlayer receiver){
        Optional<TeleportRequest> possibleTeleportRequest =
                TELEPORT_APPROVAL_REQUESTS.values()
                        .stream()
                        .filter(tr -> tr.getReceiverPlayerUUID().equals(receiver.getUUID()))
                        .findFirst();
        return processTeleportRequestDenial(receiver, possibleTeleportRequest);
    }

    public static int denyTeleportRequestPlayer(ServerPlayer receiver, ServerPlayer target){
        Optional<TeleportRequest> possibleTeleportRequest =
                TELEPORT_APPROVAL_REQUESTS.values()
                        .stream()
                        .filter(tr -> tr.getReceiverPlayerUUID().equals(receiver.getUUID()) && tr.getSenderPlayer().getUUID().equals(target.getUUID()))
                        .findFirst();
        return processTeleportRequestDenial(receiver, possibleTeleportRequest);
    }

    private static int processTeleportRequestDenial(ServerPlayer receiver, Optional<TeleportRequest> possibleTeleportRequest) {
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

    private static boolean executeApprovedTeleport(TeleportRequest request) {
        MinecraftServer server = request.getSenderPlayer().getServer();

        if (server == null) {
            return false;
        }

        ServerPlayer sender = request.getSenderPlayer();
        ServerPlayer receiver = server.getPlayerList().getPlayer(request.getReceiverPlayerUUID());

        if (sender == null || receiver == null) {
            return false;
        }

        if (!request.isTpaHere()) {
            return teleportPlayer(
                    new TeleportPos(
                            receiver.level(),
                            receiver.position(),
                            receiver.getXRot(),
                            receiver.getYRot()
                    ),
                    sender,
                    true
            );
        }

        if (request.isTpaHere()) {
            return teleportPlayer(
                    new TeleportPos(
                            sender.level(),
                            sender.position(),
                            sender.getXRot(),
                            sender.getYRot()
                    ),
                    receiver,
                    true
            );
        }

        return false;
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

        APPROVED_TELEPORTS.entrySet().removeIf(entry -> {
            try {
                return executeApprovedTeleport(entry.getValue());
            } catch (Exception e) {
                LOGGER.error("Error while processing approved teleport for {}", entry, e);
                return false;
            }
        });
    }

    public void flush(){
        TELEPORT_APPROVAL_REQUESTS.clear();
        APPROVED_TELEPORTS.clear();
        currentTick =0;
    }
}
