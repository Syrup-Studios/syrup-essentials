package net.syrupstudios.syrupessentials;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.syrupstudios.syrupessentials.commands.TeleportCommands;
import net.syrupstudios.syrupessentials.data.PlayerData;
import net.syrupstudios.syrupessentials.util.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyrupEssentials implements ModInitializer {
	public static final String MOD_ID = "syrup-essentials";
	private DataManager dataManager;

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Syrup Essentials");
		CommandRegistrationCallback.EVENT.register((
				(commandDispatcher, commandBuildContext, commandSelection) ->
						TeleportCommands.register(commandDispatcher)));

		ServerLifecycleEvents.SERVER_STARTED.register((server) ->
				dataManager = new DataManager(server));

		ServerPlayConnectionEvents.JOIN.register((phase, listener, server) ->
					playerJoin(phase));

		ServerPlayConnectionEvents.DISCONNECT.register(this::playerLeave);
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
}