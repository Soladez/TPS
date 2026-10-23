package dev.tpsmonitor;

import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class TpsMonitor implements ModInitializer {
	public static final String MOD_ID = "tpsmonitor";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final TickTracker TRACKER = new TickTracker();
	private static int ticksSinceSync;

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.clientboundPlay().register(TpsPayload.TYPE, TpsPayload.CODEC);

		ServerLifecycleEvents.SERVER_STARTING.register(server -> TRACKER.reset());
		ServerTickEvents.START_SERVER_TICK.register(server -> TRACKER.onTickStart());
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			TRACKER.onTickEnd();

			// Einmal pro Sekunde (20 Ticks) an alle Spieler mit der Mod senden.
			if (++ticksSinceSync >= 20) {
				ticksSinceSync = 0;
				TpsPayload payload = new TpsPayload(
						(float) TRACKER.tps(TickTracker.SHORT_WINDOW),
						(float) TRACKER.mspt(TickTracker.SHORT_WINDOW));

				for (ServerPlayer player : server.getPlayerList().getPlayers()) {
					if (ServerPlayNetworking.canSend(player, TpsPayload.TYPE)) {
						ServerPlayNetworking.send(player, payload);
					}
				}
			}
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
				dispatcher.register(Commands.literal("tps").executes(context -> {
					double tps5s = TRACKER.tps(TickTracker.SHORT_WINDOW);
					double tps1m = TRACKER.tpsLong();
					double mspt = TRACKER.mspt(TickTracker.SHORT_WINDOW);

					context.getSource().sendSuccess(() -> Component.literal("TPS (5s / 1m): ").withStyle(ChatFormatting.GOLD)
							.append(colored(tps5s))
							.append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
							.append(colored(tps1m))
							.append(Component.literal("  MSPT: ").withStyle(ChatFormatting.GOLD))
							.append(Component.literal(format(mspt)).withStyle(msptColor(mspt))), false);
					return 1;
				})));

		LOGGER.info("TPS Monitor geladen");
	}

	private static MutableComponent colored(double tps) {
		return Component.literal(format(tps)).withStyle(tpsColor(tps));
	}

	public static String format(double value) {
		return String.format(Locale.ROOT, "%.1f", value);
	}

	public static ChatFormatting tpsColor(double tps) {
		if (tps >= 19.5) return ChatFormatting.GREEN;
		if (tps >= 15.0) return ChatFormatting.YELLOW;
		return ChatFormatting.RED;
	}

	public static ChatFormatting msptColor(double mspt) {
		if (mspt <= 40.0) return ChatFormatting.GREEN;
		if (mspt <= 50.0) return ChatFormatting.YELLOW;
		return ChatFormatting.RED;
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
