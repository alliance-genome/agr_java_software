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
		this.path = path;
		this.esPaths = new ArrayList<>(fieldMap.values());
		Files.createDirectories(path.getParent());
		this.writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(path)), StandardCharsets.UTF_8));
		writer.write(loadVcfHeaderTemplate());
		writer.write(COLUMN_HEADER);
		writer.write("\n");
	}

	private static String loadVcfHeaderTemplate() throws IOException {
		try (InputStream in = VcfWriter.class.getClassLoader().getResourceAsStream("vcf_header_template.txt")) {
			if (in == null) {
				throw new IOException("vcf_header_template.txt resource missing");
			}
			StringBuilder sb = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
				String fileDate = ZonedDateTime.now(ZoneOffset.UTC).format(FILE_DATE_FMT);
				String line;
				while ((line = reader.readLine()) != null) {
					sb.append(line.replace("{fileDate}", fileDate)).append("\n");
				}
			}
			return sb.toString();
		}
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
