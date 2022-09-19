package org.alliancegenome.data_extractor.translators;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import org.alliancegenome.core.translators.EntityTSVTranslator;
import org.alliancegenome.neo4j.entity.node.DOTerm;

import lombok.Getter;

@Getter
public class DiseaseTSVTranslator extends EntityTSVTranslator<DOTerm> {

	public DiseaseTSVTranslator(PrintWriter writer) {
		super(writer);
	}

	@Override
	protected List<String> getHeaders() {
		return Arrays.asList(
				"Id",
				"Definition",
				"Prefix"
			);
	}
	
	@Override
	protected List<String> entityToRow(DOTerm entity) {
		
		return Arrays.asList(
			entity.getPrimaryKey(),
			entity.getDefinition(),
			entity.getDoPrefix()
		);
		
	}

}
