package org.alliancegenome.filegenerator.writers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.alliancegenome.core.util.SmartAlphaComparator;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

/**
 * VCF v4.3 writer. Streams a VCFv4.3-compliant gzipped file with:
 *   1. Static `##` header (loaded from {@code vcf_header_template.txt} resource)
 *   2. `#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO` column header
 *   3. Tab-separated data rows
 *
 * The field map's 8 ordered keys must be exactly
 * {@code CHROM, POS, ID, REF, ALT, QUAL, FILTER, INFO}; the values are JSON paths or synthetic
 * `_*` field names populated by the generator's {@code customizeRow}. Empty values are rendered
 * as a single dot (`.`) per VCF spec for missing fields, and the INFO column should be a
 * pre-formatted {@code key="value";...} string.
 */
@Slf4j
public class VcfWriter implements RowWriter {

	private static final String COLUMN_HEADER = "#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO";
	private static final DateTimeFormatter FILE_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

	private final Path path;
	private final BufferedWriter writer;
	private final List<String> esPaths;
	// Rows are buffered then sorted on close so the table is emitted in CHROM (smart-alpha) then POS (numeric) order — matches the legacy VCF format and lets downstream tools rely on positional ordering.
	private final List<String[]> bufferedRows = new ArrayList<>();
	private long rowCount;
	private long skippedRowCount;

	public VcfWriter(Path path, Map<String, String> fieldMap) throws IOException {
		this(path, fieldMap, Map.of());
	}

	public VcfWriter(Path path, Map<String, String> fieldMap, Map<String, String> headerSubstitutions) throws IOException {
		this.path = path;
		this.esPaths = new ArrayList<>(fieldMap.values());
		Files.createDirectories(path.getParent());
		this.writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(path)), StandardCharsets.UTF_8));
		writer.write(loadVcfHeaderTemplate(headerSubstitutions));
		writer.write(COLUMN_HEADER);
		writer.write("\n");
	}

	private static String loadVcfHeaderTemplate(Map<String, String> extraSubstitutions) throws IOException {
		try (InputStream in = VcfWriter.class.getClassLoader().getResourceAsStream("vcf_header_template.txt")) {
			if (in == null) {
				throw new IOException("vcf_header_template.txt resource missing");
			}
			Map<String, String> subs = new LinkedHashMap<>();
			subs.put("{fileDate}", ZonedDateTime.now(ZoneOffset.UTC).format(FILE_DATE_FMT));
			if (extraSubstitutions != null) {
				for (Map.Entry<String, String> e : extraSubstitutions.entrySet()) {
					subs.put(e.getKey(), e.getValue() == null ? "" : e.getValue());
				}
			}
			StringBuilder sb = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					String replaced = applySubstitutions(line, subs);
					// A template line that is JUST a placeholder substituting to empty is dropped entirely so the header has no orphan blank line.
					if (isWhollyEmptiedPlaceholderLine(line, subs, replaced)) {
						continue;
					}
					sb.append(replaced).append("\n");
				}
			}
			return sb.toString();
		}
	}

	private static String applySubstitutions(String line, Map<String, String> subs) {
		String out = line;
		for (Map.Entry<String, String> e : subs.entrySet()) {
			out = out.replace(e.getKey(), e.getValue());
		}
		return out;
	}

	private static boolean isWhollyEmptiedPlaceholderLine(String original, Map<String, String> subs, String replaced) {
		if (!replaced.isEmpty()) {
			return false;
		}
		String trimmed = original.trim();
		for (Map.Entry<String, String> e : subs.entrySet()) {
			if (trimmed.equals(e.getKey()) && e.getValue().isEmpty()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public synchronized void writeRow(JsonNode hit) throws IOException {
		String[] cells = new String[esPaths.size()];
		for (int i = 0; i < esPaths.size(); i++) {
			String v = JsonPath.resolveString(hit, esPaths.get(i));
			cells[i] = v == null || v.isEmpty() ? "." : escape(v);
		}
		// VCF v4.3 §1.4.1: REF cannot be missing, and REF and ALT must differ. Drop rows where either is true — the underlying ES data is degenerate (e.g. `g.X_YinsZ` with no inserted base, or `g.PC>C` no-op SNVs) and emitting them produces files that htsjdk refuses to parse.
		if (".".equals(cells[3]) || cells[3].isEmpty() || cells[3].equals(cells[4])) {
			skippedRowCount++;
			return;
		}
		bufferedRows.add(cells);
		rowCount++;
	}

	@Override
	public Path getPath() {
		return path;
	}

	@Override
	public synchronized long getRowCount() {
		return rowCount;
	}

	@Override
	public synchronized void close() throws IOException {
		if (skippedRowCount > 0) {
			log.info("VcfWriter: {} — skipped {} row(s) for empty/duplicate REF (degenerate source data)", path.getFileName(), skippedRowCount);
		}
		// Field map ordering is fixed by VariantsVcf in FileGeneratorConfig — cells[0] is CHROM and cells[1] is POS, so sort directly on those indices.
		bufferedRows.sort(CHROM_THEN_POS);
		StringBuilder sb = new StringBuilder();
		for (String[] cells : bufferedRows) {
			for (int i = 0; i < cells.length; i++) {
				if (i > 0) {
					sb.append('\t');
				}
				sb.append(cells[i]);
			}
			sb.append('\n');
		}
		writer.write(sb.toString());
		writer.flush();
		writer.close();
	}

	// Smart-alpha CHROM ordering (2L < 2R < 3L < 3R < 4 < X for fly; 1 < 2 < ... < 19 < MT < X for mouse) plus numeric POS within a chrom.
	private static final Comparator<String[]> CHROM_THEN_POS = (a, b) -> {
		int c = SmartAlphaComparator.INSTANCE.compare(a[0], b[0]);
		if (c != 0) {
			return c;
		}
		return Long.compare(parsePos(a[1]), parsePos(b[1]));
	};

	private static long parsePos(String s) {
		if (s == null || s.isEmpty() || ".".equals(s)) {
			return Long.MAX_VALUE;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return Long.MAX_VALUE;
		}
	}

	private static String escape(String s) {
		// Strip embedded tabs/newlines from any cell to preserve VCF column alignment.
		return s.replace("\t", " ").replace("\n", " ").replace("\r", " ");
	}
}
