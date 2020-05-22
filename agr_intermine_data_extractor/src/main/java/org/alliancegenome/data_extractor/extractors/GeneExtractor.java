package org.alliancegenome.data_extractor.extractors;

import java.io.PrintWriter;
import java.util.List;

import org.alliancegenome.data_extractor.translators.GeneTSVTranslator;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneRepository;

public class GeneExtractor extends DataExtractor {

    private GeneRepository geneRepo = new GeneRepository();

    @Override
    protected void extract(PrintWriter writer) {
        
        GeneTSVTranslator translator = new GeneTSVTranslator(writer);
        
        List<String> geneIds = geneRepo.getAllGeneKeys();
        
        startProcess("Starting Genes: ", geneIds.size());
        for(String id: geneIds) {
            Gene g = geneRepo.getOneGene(id);
            translator.translateEntity(g);
            
            progressProcess();
        }
        finishProcess();
    }

    @Override
    protected String getFileName() {
        return "Gene.tsv";
    }

}
