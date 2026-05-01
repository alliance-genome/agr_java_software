package org.alliancegenome.filegenerator.writers;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonNode;

public interface RowWriter extends Closeable {

	void writeRow(JsonNode hit) throws IOException;

	Path getPath();

	long getRowCount();
}
