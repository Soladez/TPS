package dev.tpsmonitor;

/**
 * Misst die Tick-Zeiten des Servers selbst über Fabric-Tick-Events.
 * Wird ausschließlich vom Server-Thread aufgerufen.
 */
public final class TickTracker {
	/** 1200 Ticks = 1 Minute bei 20 TPS. */
	private static final int HISTORY = 1200;
	/** 100 Ticks = 5 Sekunden bei 20 TPS. */
	public static final int SHORT_WINDOW = 100;

	private final long[] intervals = new long[HISTORY]; // Abstand zwischen zwei Tick-Starts (ns)
	private final long[] durations = new long[HISTORY]; // Dauer eines Ticks (ns)
	private int index;
	private int count;
	private long lastStart = -1;
	private long currentStart;

	public void onTickStart() {
		long now = System.nanoTime();
		if (lastStart >= 0) {
			intervals[index] = now - lastStart;
		}
		lastStart = now;
		currentStart = now;
	}

	public void onTickEnd() {
		if (intervals[index] == 0) {
			// Erster Tick nach dem Start – noch kein Intervall vorhanden.
			return;
		}
		durations[index] = System.nanoTime() - currentStart;
		index = (index + 1) % HISTORY;
		if (count < HISTORY) {
			count++;
		}
	}

	public void reset() {
		java.util.Arrays.fill(intervals, 0);
		java.util.Arrays.fill(durations, 0);
		index = 0;
		count = 0;
		lastStart = -1;
	}

	/** Durchschnittliche TPS über die letzten {@code ticks} Ticks. */
	public double tps(int ticks) {
		double avgInterval = average(intervals, ticks);
		return avgInterval <= 0 ? 20.0 : 1_000_000_000.0 / avgInterval;
	}

	/** Durchschnittliche Millisekunden pro Tick über die letzten {@code ticks} Ticks. */
	public double mspt(int ticks) {
		return average(durations, ticks) / 1_000_000.0;
	}

	public double tpsLong() {
		return tps(HISTORY);
	}

	private double average(long[] values, int ticks) {
		int n = Math.min(ticks, count);
		if (n == 0) {
			return 0;
		}
		long sum = 0;
		for (int i = 1; i <= n; i++) {
			sum += values[(index - i + HISTORY) % HISTORY];
		}
		return (double) sum / n;
	}
}
