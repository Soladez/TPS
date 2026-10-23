package dev.tpsmonitor;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Wird jede Sekunde vom Server an Clients mit installierter Mod geschickt. */
public record TpsPayload(float tps, float mspt) implements CustomPacketPayload {
	public static final Type<TpsPayload> TYPE = new Type<>(TpsMonitor.id("tps"));
	public static final StreamCodec<FriendlyByteBuf, TpsPayload> CODEC = StreamCodec.ofMember(TpsPayload::write, TpsPayload::read);

	private static TpsPayload read(FriendlyByteBuf buf) {
		return new TpsPayload(buf.readFloat(), buf.readFloat());
	}

	private void write(FriendlyByteBuf buf) {
		buf.writeFloat(this.tps);
		buf.writeFloat(this.mspt);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
