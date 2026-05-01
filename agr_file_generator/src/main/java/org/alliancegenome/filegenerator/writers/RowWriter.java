package org.alliancegenome.filegenerator.writers;

import java.io.Closeable;
import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

public interface RowWriter extends Closeable {

	void writeRow(JsonNode hit) throws IOException;

	java.nio.file.Path getPath();

	long getRowCount();
}
