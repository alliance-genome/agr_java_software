package org.alliancegenome.data_extractor.extractors;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import org.alliancegenome.data_extractor.translators.GeneTSVTranslator;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.DataExtractorRepository;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.neo4j.ogm.model.Result;

public class GeneExtractor extends DataExtractor {

    private DataExtractorRepository repo = new DataExtractorRepository();

    @Override
    protected void extract(PrintWriter writer) {
        
        GeneTSVTranslator translator = new GeneTSVTranslator(writer);
        
        Result gene_res = repo.getAllGenes();
        
        translator.translateResult(gene_res);

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
