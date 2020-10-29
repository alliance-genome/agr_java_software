package org.alliancegenome.data_extractor.extractors;

import java.io.PrintWriter;
import java.util.List;

import org.alliancegenome.data_extractor.translators.AlleleTSVTranslator;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.repository.AlleleRepository;

public class AlleleExtractor extends DataExtractor {

    private AlleleRepository alleleRepo = new AlleleRepository();
    
    @Override
    protected void extract(PrintWriter writer) {

        AlleleTSVTranslator translator = new AlleleTSVTranslator(writer);
        
        List<String> alleleIds = alleleRepo.getAllAlleleKeys();
        
        startProcess("Starting Alleles: ", alleleIds.size());
        for(String id: alleleIds) {
            Allele a = alleleRepo.getAllele(id);
            translator.translateEntity(a);
            progressProcess();
        }
        finishProcess();
    }
    
    @Override
    protected String getFileName() {
        return "Allele.tsv";
    }

    @Override
    protected String getDirName() {
        return "alleles";
    }
}
