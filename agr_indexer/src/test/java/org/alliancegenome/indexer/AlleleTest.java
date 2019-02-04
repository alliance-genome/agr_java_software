package org.alliancegenome.indexer;

import java.util.List;

import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.repository.AlleleRepository;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AlleleTest {

    public static void main(String[] args) throws Exception {
        GeneRepository geneRepo = new GeneRepository();
        AlleleRepository repo = new AlleleRepository();

        Allele allele = repo.getAllele("MGI:1857937");


        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        
        String json = mapper.writerWithDefaultPrettyPrinter().withView(View.AlleleAPI.class).writeValueAsString(allele);
        System.out.println(json);
        
        List<Allele> alleles = geneRepo.getAlleles("MGI:109583");
        
        json = mapper.writerWithDefaultPrettyPrinter().withView(View.GeneAllelesAPI.class).writeValueAsString(alleles);
        System.out.println(json);
    }
}
