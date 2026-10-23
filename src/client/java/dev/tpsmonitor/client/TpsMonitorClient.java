package dev.tpsmonitor.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

import dev.tpsmonitor.TpsMonitor;
import dev.tpsmonitor.TpsPayload;

public class TpsMonitorClient implements ClientModInitializer {
	/** Nach so vielen ms ohne Update gilt der Wert als veraltet (Server hängt). */
	private static final long STALE_AFTER_MS = 3000;
	private static final int COLOR_GREEN = 0xFF55FF55;
	private static final int COLOR_YELLOW = 0xFFFFFF55;
	private static final int COLOR_RED = 0xFFFF5555;

	private static volatile TpsPayload latest;
	private static volatile long lastUpdate;
	private static boolean hudEnabled = true;

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(TpsPayload.TYPE, (payload, context) -> {
			latest = payload;
			lastUpdate = System.currentTimeMillis();
		});

		ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> latest = null);

		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, TpsMonitor.id("tps_hud"), TpsMonitorClient::extractHud);

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) ->
				dispatcher.register(ClientCommands.literal("tpshud").executes(context -> {
					hudEnabled = !hudEnabled;
					context.getSource().sendFeedback(Component.literal("TPS-Anzeige " + (hudEnabled ? "an" : "aus")));
					return 1;
				})));
	}

	private static void extractHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		TpsPayload data = latest;
		Minecraft minecraft = Minecraft.getInstance();
		if (!hudEnabled || data == null) {
			return;
		}

		Font font = minecraft.font;
		boolean stale = System.currentTimeMillis() - lastUpdate > STALE_AFTER_MS;
		String tpsText = "TPS: " + TpsMonitor.format(data.tps());
		String msptText = "  MSPT: " + TpsMonitor.format(data.mspt()) + (stale ? "  (keine Antwort)" : "");

		int x = 4;
		int y = 4;
		int width = font.width(tpsText) + font.width(msptText);
		graphics.fill(x - 2, y - 2, x + width + 2, y + font.lineHeight + 1, 0x90000000);

		int tpsColor = stale ? COLOR_RED : hudColor(TpsMonitor.tpsColor(data.tps()));
		int msptColor = stale ? COLOR_RED : hudColor(TpsMonitor.msptColor(data.mspt()));
		graphics.text(font, tpsText, x, y, tpsColor);
		graphics.text(font, msptText, x + font.width(tpsText), y, msptColor);
	}

	private static int hudColor(ChatFormatting formatting) {
		return switch (formatting) {
			case GREEN -> COLOR_GREEN;
			case YELLOW -> COLOR_YELLOW;
			default -> COLOR_RED;
		};
	}
}
