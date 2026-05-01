package org.alliancegenome.filegenerator.writers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonMappedWriter implements RowWriter {

	private final Path path;
	private final BufferedWriter writer;
	private final Map<String, String> fieldMap;
	private final ObjectMapper om = new ObjectMapper();
	private long rowCount;
	private boolean firstRow = true;

	public JsonMappedWriter(Path path, Map<String, Object> metadata, Map<String, String> fieldMap) throws IOException {
		this.path = path;
		this.fieldMap = fieldMap;
		Files.createDirectories(path.getParent());
		this.writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(path)), StandardCharsets.UTF_8));
		writer.write("{\"metadata\": ");
		writer.write(om.writeValueAsString(metadata));
		writer.write(", \"data\": [");
	}

	@Override
	public synchronized void writeRow(JsonNode hit) throws IOException {
		Map<String, String> row = new LinkedHashMap<>();
		for (Map.Entry<String, String> entry : fieldMap.entrySet()) {
			row.put(entry.getKey(), JsonPath.resolveString(hit, entry.getValue()));
		}
		if (!firstRow) writer.write(",");
		writer.write(om.writeValueAsString(row));
		firstRow = false;
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
		writer.write("]}");
		writer.flush();
		writer.close();
	}
}
