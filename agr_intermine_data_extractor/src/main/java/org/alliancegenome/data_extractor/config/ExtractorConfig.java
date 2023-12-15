package org.alliancegenome.data_extractor.config;

import org.alliancegenome.data_extractor.extractors.FMSExtractor;
import org.alliancegenome.data_extractor.extractors.GeneExtractor;

public enum ExtractorConfig {

	// AlleleExtractor(AlleleExtractor.class),
	// DiseaseExtractor(DiseaseExtractor.class),
	GeneExtractor(GeneExtractor.class),
	FMSExtractor(FMSExtractor.class);

	private String extractorName;
	private Class<?> extractorClass;

	ExtractorConfig(Class<?> extractorClass) {
		this.extractorName = extractorClass.getSimpleName();
		this.extractorClass = extractorClass;
	}

	public String getExtractorName() {
		return extractorName;
	}

	public Class<?> getCacherClass() {
		return extractorClass;
	}
}
