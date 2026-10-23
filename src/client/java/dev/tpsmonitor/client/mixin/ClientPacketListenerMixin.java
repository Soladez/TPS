package dev.tpsmonitor.client.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.tpsmonitor.client.ClientTpsEstimator;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
	// TAIL statt HEAD: handleSetTime wird zuerst im Netzwerk-Thread aufgerufen und bricht dort
	// ab, um sich in den Client-Thread umzuplanen. Das Ende erreicht nur der zweite Aufruf.
	@Inject(method = "handleSetTime", at = @At("TAIL"))
	private void tpsmonitor$onSetTime(ClientboundSetTimePacket packet, CallbackInfo ci) {
		ClientTpsEstimator.onTimePacket(packet.gameTime());
	}
}
