package org.alliancegenome.api.tests.integration;

import java.util.List;

import org.alliancegenome.api.service.ensembl.EnsemblVariantService;
import org.alliancegenome.api.service.ensembl.model.EnsemblVariant;
import org.alliancegenome.neo4j.entity.node.CrossReference;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneRepository;

public class TestEnsembl {

    public static void main(String[] args) {

        EnsemblVariantService service = new EnsemblVariantService();
        
        //EnsemblVariantFeature feature = service.getVariantFeatures("rs56116432", "human", 1);
        //System.out.println(feature);
        
        //List<EnsemblVariantConsequence> cons = service.getVariantConsequence("rs56116432", "human");
        //System.out.println(cons);
        
        GeneRepository geneRepo = new GeneRepository();
        
        Gene g = geneRepo.getOneGene("MGI:97490");
        
        String primaryKey = null;
        for(CrossReference cr: g.getCrossReferences()) {
            if(cr.getPrefix().equals("ENSEMBL")) {
                primaryKey = cr.getLocalId();
            }
        }

        List<EnsemblVariant> variants = service.getVariants(primaryKey, "variation");
        System.out.println(variants);
        
    }

}