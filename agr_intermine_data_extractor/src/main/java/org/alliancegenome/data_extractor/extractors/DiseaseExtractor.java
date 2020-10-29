package org.alliancegenome.data_extractor.extractors;

import java.io.PrintWriter;
import java.util.Set;

import org.alliancegenome.data_extractor.translators.DiseaseTSVTranslator;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.repository.DiseaseRepository;

public class DiseaseExtractor extends DataExtractor {

    DiseaseRepository repo = new DiseaseRepository();
    
    @Override
    protected void extract(PrintWriter writer) {
        
        Set<String> ids = repo.getAllDiseaseWithAnnotationsKeys();
        DiseaseTSVTranslator translator = new DiseaseTSVTranslator(writer);
        
        startProcess("Starting Diseases: ", ids.size());
        for(String id: ids) {
            DOTerm doTerm = repo.getDiseaseTerm(id);
            translator.translateEntity(doTerm);
            progressProcess();
        }
        finishProcess();
    }

    @Override
    protected String getFileName() {
        return "Diseases.tsv";
    }
    
    @Override
    protected String getDirName() {
        return "diseases";
    }

}

