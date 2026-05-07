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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import com.fasterxml.jackson.databind.JsonNode;

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
public class VcfWriter implements RowWriter {

	private static final String COLUMN_HEADER = "#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO";
	private static final DateTimeFormatter FILE_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

	private final Path path;
	private final BufferedWriter writer;
	private final List<String> esPaths;
	private long rowCount;

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
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < esPaths.size(); i++) {
			if (i > 0) {
				sb.append("\t");
			}
			String v = JsonPath.resolveString(hit, esPaths.get(i));
			sb.append(v == null || v.isEmpty() ? "." : escape(v));
		}
		sb.append("\n");
		writer.write(sb.toString());
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
		writer.flush();
		writer.close();
	}

	private static String escape(String s) {
		// Strip embedded tabs/newlines from any cell to preserve VCF column alignment.
		return s.replace("\t", " ").replace("\n", " ").replace("\r", " ");
	}
}
