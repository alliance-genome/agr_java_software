package org.alliancegenome.data_extractor.config;

import org.alliancegenome.data_extractor.extractors.*;

public enum ExtractorConfig {
    
    //AlleleExtractor(AlleleExtractor.class),
    GeneExtractor(GeneExtractor.class),
    DiseaseExtractor(DiseaseExtractor.class),
    FMSExtractor(FMSExtractor.class)
    ;
    
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
