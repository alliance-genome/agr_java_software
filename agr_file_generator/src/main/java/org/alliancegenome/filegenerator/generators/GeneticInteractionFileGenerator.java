package org.alliancegenome.filegenerator.generators;

import org.alliancegenome.filegenerator.config.FileGeneratorConfig;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeneticInteractionFileGenerator extends BaseInteractionFileGenerator {

	public GeneticInteractionFileGenerator(FileGeneratorConfig config) {
		super(config);
	}

	@Override
	protected String docRoot() {
		return "geneGeneticInteraction";
	}
}
