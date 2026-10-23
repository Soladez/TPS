package dev.tpsmonitor.client;

/**
 * Schätzt die Server-TPS ohne Server-Mod: Vanilla-Server schicken alle 20 Ticks die Spielzeit
 * an jeden Client. Aus vergangenen Ticks pro echter Zeit ergibt sich die TPS.
 * Wird nur im Client-Thread aufgerufen.
 */
public final class ClientTpsEstimator {
	/** Anzahl der Zeit-Pakete im Mittelwert (ca. 10 Sekunden). */
	private static final int SAMPLES = 10;

	private static final long[] gameTimes = new long[SAMPLES];
	private static final long[] receivedAt = new long[SAMPLES];
	private static int index;
	private static int count;
	private static volatile double tps = -1;
	private static volatile long lastPacket;

	private ClientTpsEstimator() {
	}

	public static void onTimePacket(long gameTime) {
		long now = System.nanoTime();
		if (count > 0) {
			int newest = (index - 1 + SAMPLES) % SAMPLES;
			// Spielzeit zurückgesprungen (z. B. Serverwechsel): neu anfangen.
			if (gameTime < gameTimes[newest]) {
				reset();
			}
		}

		gameTimes[index] = gameTime;
		receivedAt[index] = now;
		index = (index + 1) % SAMPLES;
		if (count < SAMPLES) {
			count++;
		}
		lastPacket = System.currentTimeMillis();

		if (count >= 2) {
			int newest = (index - 1 + SAMPLES) % SAMPLES;
			int oldest = (index - count + SAMPLES) % SAMPLES;
			double seconds = (receivedAt[newest] - receivedAt[oldest]) / 1_000_000_000.0;
			if (seconds > 0) {
				// Auf 20 begrenzen, Netzwerk-Schwankungen ergeben sonst Werte wie 20.3.
				tps = Math.min(20.0, (gameTimes[newest] - gameTimes[oldest]) / seconds);
			}
		}
	}

	public static void reset() {
		index = 0;
		count = 0;
		tps = -1;
	}

	/** Geschätzte TPS oder -1, solange noch nicht genug Daten da sind. */
	public static double tps() {
		return tps;
	}

	public static long lastPacket() {
		return lastPacket;
	}
}
