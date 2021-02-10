package org.alliancegenome.data_extractor.extractors;

import java.io.PrintWriter;

import org.alliancegenome.data_extractor.translators.AlleleTSVTranslator;
import org.alliancegenome.neo4j.repository.DataExtractorRepository;
import org.neo4j.ogm.model.Result;


public class AlleleExtractor extends DataExtractor {

    private DataExtractorRepository dataRepo = new DataExtractorRepository();
    
    @Override
    protected void extract(PrintWriter writer) {

        //AlleleTSVTranslator translator = new AlleleTSVTranslator(writer);
        
        //Result allele_res = dataRepo.getAllalleles();
        
        //translator.translateResult(allele_res);
       
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


