package org.alliancegenome.vep.debug;

/**
 * Minimal trace logger for debugging Perl vs Java parity.
 * Writes structured lines to stderr matching Perl's [TRACE] format:
 *
 *   [TRACE] Module.method key1=val1 key2=val2 ...
 *
 * Enable by setting -Dvep.trace=true on the JVM. No-op otherwise.
 */
public final class Trace {

	private static final boolean ENABLED = Boolean.parseBoolean(System.getProperty("vep.trace", "false"));

	private Trace() {}

	public static boolean enabled() { return ENABLED; }

	public static void log(String tag, String fmt, Object... args) {
		if (!ENABLED) return;
		System.err.println("[TRACE] " + tag + " " + String.format(fmt, args));
	}

	/** Format an int that might be undef in Perl (-1 → "undef"). */
	public static String undef(int v) { return v < 0 ? "undef" : String.valueOf(v); }

	/** Format a String that might be null (null → "undef"). */
	public static String undef(String v) { return v == null ? "undef" : v; }
}
