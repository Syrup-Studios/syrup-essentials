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
import net.syrupstudios.syrupessentials.util.TeleportManager;
import net.syrupstudios.syrupessentials.util.TeleportPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyrupEssentials implements ModInitializer {
	public static final String MOD_ID = "syrup-essentials";
	private DataManager dataManager;
	private TeleportManager teleportManager;

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Syrup Essentials");
		CommandRegistrationCallback.EVENT.register((
				(commandDispatcher, commandBuildContext, commandSelection) ->
						TeleportCommands.register(commandDispatcher)));

		ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
					dataManager = new DataManager(server);
					teleportManager = new TeleportManager();
					createWorld(server);
				});

		ServerTickEvents.START_SERVER_TICK.register(this::tick);

		ServerPlayConnectionEvents.JOIN.register((phase, listener, server) ->
					playerJoin(phase));

		ServerLivingEntityEvents.AFTER_DEATH.register(((livingEntity, damageSource) -> {
			if (livingEntity instanceof ServerPlayer player) {
				saveDeathLocation(player);
			}
		}));

		ServerPlayConnectionEvents.DISCONNECT.register(this::playerLeave);

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
				dataManager.saveWorld(server);
				dataManager.savePlayers(server);
				dataManager.flush();
		});
	}

	private void saveDeathLocation(ServerPlayer player) {
		PlayerData playerData = DataManager.getOrCreatePlayer(player).orElseThrow();
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

	private void createWorld(MinecraftServer server){
		try {
			dataManager.loadWorld(server);
		} catch (Exception e) {
			LOGGER.error("Error Loading World: {}", server.getWorldData().getLevelName());
		}
	}

	private void playerLeave(ServerGamePacketListenerImpl phase, MinecraftServer server) {
		try {
			dataManager.savePlayer(DataManager.getOrCreatePlayer(phase.getPlayer()).orElseThrow());
		}
		catch (Exception e) {
			LOGGER.error("Error Saving Player: {}", phase.getPlayer().getDisplayName().getString());
		}
	}

	private void tick(MinecraftServer minecraftServer) {
		dataManager.onServerTick();
		teleportManager.onServerTick();
	}
}