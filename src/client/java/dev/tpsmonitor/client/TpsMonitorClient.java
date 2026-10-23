package dev.tpsmonitor.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

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
	private static final int COLOR_GRAY = 0xFFAAAAAA;

	/** Genaue Werte, falls der Server die Mod ebenfalls hat. */
	private static volatile TpsPayload latest;
	private static volatile long lastUpdate;
	private static boolean hudEnabled = true;

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(TpsPayload.TYPE, (payload, context) -> {
			latest = payload;
			lastUpdate = System.currentTimeMillis();
		});

		ClientPlayConnectionEvents.JOIN.register((listener, sender, client) -> resetAll());
		ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> resetAll());

		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, TpsMonitor.id("tps_hud"), TpsMonitorClient::extractHud);

		// Clientseitiges /tps: funktioniert auf jedem Server, auch ohne die Mod dort.
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> {
			dispatcher.register(ClientCommands.literal("tps").executes(context -> {
				context.getSource().sendFeedback(tpsMessage());
				return 1;
			}));
			dispatcher.register(ClientCommands.literal("tpshud").executes(context -> {
				hudEnabled = !hudEnabled;
				context.getSource().sendFeedback(Component.literal("TPS-Anzeige " + (hudEnabled ? "an" : "aus")));
				return 1;
			}));
		});
	}

	private static void resetAll() {
		latest = null;
		ClientTpsEstimator.reset();
	}

	private static boolean hasServerData() {
		return latest != null && System.currentTimeMillis() - lastUpdate <= STALE_AFTER_MS;
	}

	private static boolean isStale() {
		long last = latest != null ? lastUpdate : ClientTpsEstimator.lastPacket();
		return System.currentTimeMillis() - last > STALE_AFTER_MS;
	}

	private static Component tpsMessage() {
		if (hasServerData()) {
			TpsPayload data = latest;
			return Component.literal("Server-TPS: ").withStyle(ChatFormatting.GOLD)
					.append(Component.literal(TpsMonitor.format(data.tps())).withStyle(TpsMonitor.tpsColor(data.tps())))
					.append(Component.literal("  MSPT: ").withStyle(ChatFormatting.GOLD))
					.append(Component.literal(TpsMonitor.format(data.mspt())).withStyle(TpsMonitor.msptColor(data.mspt())));
		}

		double tps = ClientTpsEstimator.tps();
		if (tps < 0) {
			return Component.literal("Noch keine Daten – warte ein paar Sekunden und versuche es erneut.").withStyle(ChatFormatting.GRAY);
		}

		MutableComponent message = Component.literal("Server-TPS: ").withStyle(ChatFormatting.GOLD)
				.append(Component.literal(TpsMonitor.format(tps)).withStyle(TpsMonitor.tpsColor(tps)))
				.append(Component.literal(" (gemessen am Client, ca. 10 s)").withStyle(ChatFormatting.GRAY));
		if (isStale()) {
			message.append(Component.literal("  Server antwortet nicht!").withStyle(ChatFormatting.RED));
		}
		return message;
	}

	private static void extractHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		if (!hudEnabled) {
			return;
		}

		TpsPayload data = hasServerData() ? latest : null;
		double tps = data != null ? data.tps() : ClientTpsEstimator.tps();
		if (tps < 0) {
			return;
		}

		Font font = Minecraft.getInstance().font;
		boolean stale = isStale();
		String tpsText = "TPS: " + TpsMonitor.format(tps);
		String extraText = data != null ? "  MSPT: " + TpsMonitor.format(data.mspt()) : "";
		if (stale) {
			extraText += "  (keine Antwort)";
		}

		int x = 4;
		int y = 4;
		int width = font.width(tpsText) + font.width(extraText);
		graphics.fill(x - 2, y - 2, x + width + 2, y + font.lineHeight + 1, 0x90000000);

		int tpsColor = stale ? COLOR_RED : hudColor(TpsMonitor.tpsColor(tps));
		int extraColor = stale ? COLOR_RED : data != null ? hudColor(TpsMonitor.msptColor(data.mspt())) : COLOR_GRAY;
		graphics.text(font, tpsText, x, y, tpsColor);
		graphics.text(font, extraText, x + font.width(tpsText), y, extraColor);
	}

	private static int hudColor(ChatFormatting formatting) {
		return switch (formatting) {
			case GREEN -> COLOR_GREEN;
			case YELLOW -> COLOR_YELLOW;
			default -> COLOR_RED;
		};
	}
}
