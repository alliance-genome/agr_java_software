package org.alliancegenome.indexer.config;

import org.alliancegenome.indexer.indexers.DiseaseAnnotationIndexer;
import org.alliancegenome.indexer.indexers.DiseaseIndexer;
import org.alliancegenome.indexer.indexers.GeneIndexer;
import org.alliancegenome.indexer.indexers.GoIndexer;
import org.alliancegenome.indexer.schema.mappings.DiseaseMappings;
import org.alliancegenome.indexer.schema.mappings.GeneMappings;
import org.alliancegenome.indexer.schema.mappings.GoMappings;

public enum TypeConfig {

    GeneIndexer("gene", GeneIndexer.class, GeneMappings.class, 2000),
    DiseaseIndexer("disease", DiseaseIndexer.class, DiseaseMappings.class, 15000),
    DiseaseAnnotationIndexer("diseaseAnnotation", DiseaseAnnotationIndexer.class, DiseaseMappings.class, 500),
    GoIndexer("go", GoIndexer.class, GoMappings.class, 2500),;
	
	private String typeName;
    private Class<?> indexClazz;
    private Class<?> mappingsClazz;
    private int fetchChunkSize;

    TypeConfig(String typeName, Class<?> indexClazz, Class<?> mappingsClazz, int fetchChunkSize) {
        this.typeName = typeName;
        this.indexClazz = indexClazz;
        this.mappingsClazz = mappingsClazz;
        this.fetchChunkSize = fetchChunkSize;
    }

    public String getTypeName() {
        return typeName;
    }
    public int getFetchChunkSize() {
        return fetchChunkSize;
    }

    public Class<?> getIndexClazz() {
        return indexClazz;
    }

    public Class<?> getMappingsClazz() {
        return mappingsClazz;
    }

}
