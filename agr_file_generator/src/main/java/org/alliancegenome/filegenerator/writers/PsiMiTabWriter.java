package org.alliancegenome.filegenerator.writers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Writer for PSI-MI TAB 2.7 interaction files. Same on-disk gzipping as TsvWriter, but the header
 * block follows the FMS interaction convention (long format-name, README link, NCBI:txid prefix on
 * taxon IDs, "# File generated" label) — and the column header line is prefixed with `#` per the
 * PSI-MITAB spec. The actual 42-column row layout is rendered from the configured field map, so
 * the interaction generator implementations decide which ES paths feed which PSI-MITAB column.
 */
public class PsiMiTabWriter implements RowWriter {

	private final Path path;
	private final BufferedWriter writer;
	private final Map<String, String> fieldMap;
	private long rowCount;

	public PsiMiTabWriter(Path path, String headerText, Map<String, String> fieldMap) throws IOException {
		this.path = path;
		this.fieldMap = fieldMap;
		Files.createDirectories(path.getParent());
		this.writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(path)), StandardCharsets.UTF_8));
		writer.write(headerText);
		writer.write("\n");
		// PSI-MITAB spec: column header row is prefixed with `#`.
		writer.write("#");
		writer.write(String.join("\t", fieldMap.keySet()));
		writer.write("\n");
	}

	@Override
	public synchronized void writeRow(JsonNode hit) throws IOException {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String esPath : fieldMap.values()) {
			if (!first) sb.append("\t");
			String v = JsonPath.resolveString(hit, esPath);
			sb.append(v == null || v.isEmpty() ? "-" : escape(v));
			first = false;
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
		return s.replace("\t", " ").replace("\n", " ").replace("\r", " ");
	}
}
