package org.alliancegenome.api.tests.integration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.alliancegenome.api.service.ensembl.EnsemblVariantService;
import org.alliancegenome.api.service.ensembl.model.EnsemblVariant;
import org.alliancegenome.api.service.ensembl.model.EnsemblVariantConsequence;
import org.alliancegenome.api.service.ensembl.model.VariantListForm;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.neo4j.entity.node.CrossReference;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneRepository;

import lombok.extern.log4j.Log4j2;

@Log4j2
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

        List<String> ids = new ArrayList<>();
        VariantListForm form = new VariantListForm();

        variants.forEach(variant -> form.getIds().add(variant.getId()));
        //System.out.println(form);

        AtomicInteger counter = new AtomicInteger();

        Collection<List<String>> splits = form.getIds().stream().collect(Collectors.groupingBy(it -> counter.getAndIncrement() / 200)).values();

        List<EnsemblVariantConsequence> results = new ArrayList<>();
        
        for(List<String> idList: splits) {
            VariantListForm fm = new VariantListForm();
            fm.getIds().addAll(idList);
            List<EnsemblVariantConsequence> consequences = service.getVariantConsequences(g.getSpecies().getName().toLowerCase().replace(" ", "_"), fm);
            results.addAll(consequences);
            System.out.println("Size: " + results.size());
        }

        
        System.out.println(results);


    }

}