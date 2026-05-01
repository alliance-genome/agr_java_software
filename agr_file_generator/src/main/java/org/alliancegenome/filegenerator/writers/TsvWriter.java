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

public class TsvWriter implements RowWriter {

	private final Path path;
	private final BufferedWriter writer;
	private final Map<String, String> fieldMap;
	private long rowCount;

	public TsvWriter(Path path, String headerText, Map<String, String> fieldMap) throws IOException {
		this.path = path;
		this.fieldMap = fieldMap;
		Files.createDirectories(path.getParent());
		this.writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(path)), StandardCharsets.UTF_8));
		writer.write(headerText);
		writer.write("\n");
		writer.write(String.join("\t", fieldMap.keySet()));
		writer.write("\n");
	}

	@Override
	public synchronized void writeRow(JsonNode hit) throws IOException {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String esPath : fieldMap.values()) {
			if (!first) {
				sb.append("\t");
			}
			sb.append(escape(JsonPath.resolveString(hit, esPath)));
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
		if (s == null || s.isEmpty()) {
			return "";
		}
		return s.replace("\t", " ").replace("\n", " ").replace("\r", " ");
	}
}
