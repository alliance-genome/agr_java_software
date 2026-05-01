package org.alliancegenome.filegenerator.generators;

import org.alliancegenome.filegenerator.config.FileGeneratorConfig;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MolecularInteractionFileGenerator extends BaseInteractionFileGenerator {

	public MolecularInteractionFileGenerator(FileGeneratorConfig config) {
		super(config);
	}

	@Override
	protected String docRoot() {
		return "geneMolecularInteraction";
	}
}
