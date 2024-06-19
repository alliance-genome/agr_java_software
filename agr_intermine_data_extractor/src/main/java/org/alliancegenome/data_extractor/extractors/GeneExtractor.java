package org.alliancegenome.data_extractor.extractors;

import java.io.PrintWriter;

import org.alliancegenome.data_extractor.translators.GeneTSVTranslator;
import org.alliancegenome.neo4j.repository.DataExtractorRepository;

public class GeneExtractor extends DataExtractor {

	private DataExtractorRepository repo = new DataExtractorRepository();

	@Override
	protected void extract(PrintWriter writer) {

		GeneTSVTranslator translator = new GeneTSVTranslator(writer);

		translator.translateResult(repo.getAllGenes());

	}

	@Override
	protected String getFileName() {
		return "Gene.tsv";
	}

	@Override
	protected String getDirName() {
		return "genes";
	}

}