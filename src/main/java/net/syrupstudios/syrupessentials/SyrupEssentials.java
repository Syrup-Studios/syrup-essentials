package net.syrupstudios.syrupessentials;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.syrupstudios.syrupessentials.commands.TeleportCommands;
import net.syrupstudios.syrupessentials.data.PlayerData;
import net.syrupstudios.syrupessentials.util.DataManager;
import net.syrupstudios.syrupessentials.util.TeleportPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class SyrupEssentials implements ModInitializer {
	public static final String MOD_ID = "syrup-essentials";
	private DataManager dataManager;
	private long currentTick = 0;

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Syrup Essentials");
		CommandRegistrationCallback.EVENT.register((
				(commandDispatcher, commandBuildContext, commandSelection) ->
						TeleportCommands.register(commandDispatcher)));

		ServerLifecycleEvents.SERVER_STARTED.register((server) ->
				dataManager = new DataManager(server));

		ServerTickEvents.START_SERVER_TICK.register(this::tick);

		ServerPlayConnectionEvents.JOIN.register((phase, listener, server) ->
					playerJoin(phase));

		ServerLivingEntityEvents.AFTER_DEATH.register(((livingEntity, damageSource) -> {
			if (livingEntity instanceof ServerPlayer player) {
				saveDeathLocation(player);
			}
		}));
		ServerPlayConnectionEvents.DISCONNECT.register(this::playerLeave);
	}

	private void saveDeathLocation(ServerPlayer player) {
		PlayerData playerData = DataManager.getOrCreate(player).orElseThrow();
		TeleportPos deathLoc = new TeleportPos(player.level(), player.blockPosition(), player.getXRot(), player.getYRot());
		playerData.addTeleportHistory(deathLoc);
	}

	private void playerJoin(ServerGamePacketListenerImpl phase) {
		try {
			dataManager.loadPlayer(phase.getPlayer().getUUID());
		}
		catch (Exception e) {
			LOGGER.error("Error Loading Player: {}", phase.getPlayer().getDisplayName().getString());
		}
	}

	private void playerLeave(ServerGamePacketListenerImpl phase, MinecraftServer server) {
		try {
			dataManager.savePlayer(DataManager.getOrCreate(phase.getPlayer()).orElseThrow());
		}
		catch (Exception e) {
			LOGGER.error("Error Saving Player: {}", phase.getPlayer().getDisplayName().getString());
		}
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

	private void tick(MinecraftServer minecraftServer) {
		dataManager.onServerTick();
	}
}