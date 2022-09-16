package org.alliancegenome.core.translators;

import java.io.PrintWriter;
import java.util.List;
import java.util.StringJoiner;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.neo4j.entity.Neo4jEntity;

public abstract class EntityTSVTranslator<E extends Neo4jEntity> {

	private PrintWriter writer;
	
	public EntityTSVTranslator(PrintWriter writer) {
		this.writer = writer;
		writer.print(getHeaderLine());
	}
	
	public void translateEntities(Iterable<E> entities) {
		for (E entity : entities) {
			writer.print(getLine(entity));
		}
	}
	
	public void translateEntity(E entity) {
		writer.print(getLine(entity));
	}

	private String getHeaderLine() {
		StringBuilder builder = new StringBuilder();
		StringJoiner headerJoiner = new StringJoiner("\t");
		for(String s: getHeaders()) {
			headerJoiner.add(s);
		}
		builder.append(headerJoiner.toString());
		builder.append(ConfigHelper.getJavaLineSeparator());
		return builder.toString();
	}
	
	private String getLine(E entity) {
		StringBuilder builder = new StringBuilder();
		StringJoiner joiner = new StringJoiner("\t");
		for(String s: entityToRow(entity)) {
			joiner.add(s);
		}
		builder.append(joiner.toString());
		builder.append(ConfigHelper.getJavaLineSeparator());
		return builder.toString();
	}

	protected abstract List<String> getHeaders();
	protected abstract List<String> entityToRow(E entity);

}
