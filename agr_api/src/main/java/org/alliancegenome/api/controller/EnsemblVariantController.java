package org.alliancegenome.api.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.api.rest.interfaces.EnsemblVariantRESTInterface;
import org.alliancegenome.api.service.ensembl.EnsemblVariantService;
import org.alliancegenome.api.service.ensembl.model.EnsemblVariant;
import org.alliancegenome.api.service.ensembl.model.EnsemblVariantConsequence;
import org.alliancegenome.api.service.ensembl.model.VariantListForm;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.neo4j.entity.node.CrossReference;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneRepository;

@RequestScoped
public class EnsemblVariantController implements EnsemblVariantRESTInterface {

    @Inject
    private EnsemblVariantService service;

    private GeneRepository geneRepo = new GeneRepository();

    //  public EnsemblVariantFeature getVariantFeatures(String ensemblVariantId, String species) {
    //      return service.getVariantFeatures(ensemblVariantId, species, 1);
    //  }
    //  
    //  public List<EnsemblVariantConsequence> getVariantConsequence(String ensemblVariantId, String species) {
    //      return service.getVariantConsequence(ensemblVariantId, species);
    //  }
    //
    //  public List<EnsemblVariant> getVariants(String ensemblGeneId, String feature) {
    //      return service.getVariants(ensemblGeneId, "variation");
    //  }

    @Override
    public JsonResultResponse<EnsemblVariant> getEnsemblVariants(String id) {
        Gene g = geneRepo.getOneGene(id);

        String primaryKey = null;
        for(CrossReference cr: g.getCrossReferences()) {
            if(cr.getPrefix().equals("ENSEMBL")) {
                primaryKey = cr.getLocalId();
            }
        }

        List<EnsemblVariant> variants = service.getVariants(primaryKey, "variation");

        JsonResultResponse<EnsemblVariant> json = new JsonResultResponse<>();
        json.setResults(variants);
        return json;
    }

    @Override
    public JsonResultResponse<EnsemblVariantConsequence> getEnsemblVariantsVEP(String id) {
        
        System.out.println("");
        Gene g = geneRepo.getOneGene(id);

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

        
        //System.out.println(consequences);

        JsonResultResponse<EnsemblVariantConsequence> json = new JsonResultResponse<>();
        json.setResults(results);
        return json;

    }

}
