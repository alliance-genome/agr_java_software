package org.alliancegenome.vep.plugin;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Fast, memory-mapped lookup of SIFT/PolyPhen prediction matrices.
 *
 * Uses pre-built binary files (from build_mmap.py):
 *   {ORG}_{ANALYSIS}.idx - sorted index of MD5 -> offset/length
 *   {ORG}_{ANALYSIS}.dat - concatenated raw prediction matrices
 *
 * Lookup is O(log n) binary search on the index, then a direct
 * 2-byte read from the memory-mapped data file.
 */
public class PredictionLookup implements AutoCloseable {

	private static final String AMINO_ACIDS = "ACDEFGHIKLMNPQRSTVWY";
	private static final int NUM_AAS = 20;
	private static final int BYTES_PER_PRED = 2;
	private static final int NO_PREDICTION = 0xFFFF;
	private static final int PRED_SHIFT = 14;
	private static final int SCORE_MASK = 0x3FF;

	private static final String[] SIFT_LABELS = {
		"tolerated", "deleterious",
		"tolerated low confidence", "deleterious low confidence"
	};

	private static final String[] POLYPHEN_LABELS = {
		"probably damaging", "possibly damaging", "benign", "unknown"
	};

	private static final int IDX_HEADER = 4;
	private static final int IDX_ENTRY_SIZE = 40;
	private static final int IDX_MD5_LEN = 32;

	private final MappedByteBuffer dataBuf;
	private final MappedByteBuffer idxBuf;
	private final int entryCount;
	private final String[] labels;

	public PredictionLookup(Path dir, String organism, String analysis) throws IOException {
		this.labels = analysis.startsWith("pph") || analysis.startsWith("polyphen")
			? POLYPHEN_LABELS : SIFT_LABELS;

		String base = organism + "_" + analysis;

		try (RandomAccessFile idxRaf = new RandomAccessFile(
					dir.resolve(base + ".idx").toFile(), "r");
			 RandomAccessFile datRaf = new RandomAccessFile(
					dir.resolve(base + ".dat").toFile(), "r")) {

			idxBuf = idxRaf.getChannel().map(
				FileChannel.MapMode.READ_ONLY, 0, idxRaf.length());
			idxBuf.order(ByteOrder.LITTLE_ENDIAN);

			dataBuf = datRaf.getChannel().map(
				FileChannel.MapMode.READ_ONLY, 0, datRaf.length());
			dataBuf.order(ByteOrder.LITTLE_ENDIAN);
		}

		entryCount = idxBuf.getInt(0);
	}

	/**
	 * Look up a prediction.
	 *
	 * @param translationMd5 hex MD5 of the full peptide sequence (32 chars)
	 * @param position       1-based position in the protein
	 * @param altAA          alternate amino acid (single uppercase char)
	 * @return {prediction, score} or null if not found
	 */
	public String[] getPrediction(String translationMd5, int position, char altAA) {
		int aaIndex = AMINO_ACIDS.indexOf(altAA);
		if (aaIndex < 0 || position < 1) return null;

		int lo = 0, hi = entryCount - 1;
		while (lo <= hi) {
			int mid = (lo + hi) >>> 1;
			int cmp = compareMd5(mid, translationMd5);
			if (cmp < 0) lo = mid + 1;
			else if (cmp > 0) hi = mid - 1;
			else return readPrediction(mid, position, aaIndex);
		}
		return null;
	}

	public int getPeptideLength(String translationMd5) {
		int lo = 0, hi = entryCount - 1;
		while (lo <= hi) {
			int mid = (lo + hi) >>> 1;
			int cmp = compareMd5(mid, translationMd5);
			if (cmp < 0) lo = mid + 1;
			else if (cmp > 0) hi = mid - 1;
			else {
				int entryPos = IDX_HEADER + mid * IDX_ENTRY_SIZE;
				int length = idxBuf.getInt(entryPos + IDX_MD5_LEN + 4);
				return length / BYTES_PER_PRED / NUM_AAS;
			}
		}
		return -1;
	}

	@Override
	public void close() {
		// MappedByteBuffers are unmapped when GC'd
	}

	private int compareMd5(int entryIndex, String targetMd5) {
		int entryPos = IDX_HEADER + entryIndex * IDX_ENTRY_SIZE;
		for (int i = 0; i < IDX_MD5_LEN; i++) {
			int a = idxBuf.get(entryPos + i) & 0xFF;
			int b = targetMd5.charAt(i);
			if (a != b) return a - b;
		}
		return 0;
	}

	private String[] readPrediction(int entryIndex, int position, int aaIndex) {
		int entryPos = IDX_HEADER + entryIndex * IDX_ENTRY_SIZE;
		int dataOffset = idxBuf.getInt(entryPos + IDX_MD5_LEN);
		int dataLength = idxBuf.getInt(entryPos + IDX_MD5_LEN + 4);

		int peptideLength = dataLength / BYTES_PER_PRED / NUM_AAS;
		if (position > peptideLength) return null;

		int byteOffset = dataOffset
			+ ((position - 1) * NUM_AAS + aaIndex) * BYTES_PER_PRED;
		int val = dataBuf.getShort(byteOffset) & 0xFFFF;

		if (val == NO_PREDICTION) return null;

		int predIndex = val >> PRED_SHIFT;
		double score = (val & SCORE_MASK) / 1000.0;
		String prediction = predIndex < labels.length ? labels[predIndex] : "unknown";

		// VEP uses Perl's default float stringification which strips trailing zeros
		String scoreStr = String.format("%.3f", score)
			.replaceAll("0+$", "").replaceAll("\\.$", "");
		return new String[]{ prediction, scoreStr };
	}

	public static String md5Hex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(32);
			for (byte b : digest) sb.append(String.format("%02x", b));
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
}
