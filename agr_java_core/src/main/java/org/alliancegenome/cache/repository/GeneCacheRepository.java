package org.alliancegenome.cache.repository;

import java.util.*;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.cache.*;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;
import org.alliancegenome.neo4j.view.OrthologView;

import lombok.extern.log4j.Log4j2;

@Log4j2
@RequestScoped
public class GeneCacheRepository {

    @Inject
    private CacheService cacheService;

    public List<OrthologView> getAllOrthologyGenes(List<String> geneIDs) {
        List<OrthologView> fullOrthologyList = new ArrayList<>();
        geneIDs.forEach(id -> {
            final List<OrthologView> orthology = cacheService.getCacheEntries(id, CacheAlliance.GENE_ORTHOLOGY);
            if (orthology != null)
                fullOrthologyList.addAll(orthology);
        });

        return fullOrthologyList;
    }

    public List<OrthologView> getOrthologyBySpeciesSpecies(String taxonOne, String taxonTwo) {
        List<OrthologView> fullOrthologyList = new ArrayList<>();
        final List<OrthologView> orthology = cacheService.getCacheEntries(taxonOne + ":" + taxonTwo, CacheAlliance.SPECIES_SPECIES_ORTHOLOGY);
        if (orthology != null)
            fullOrthologyList.addAll(orthology);

        return fullOrthologyList;
    }

    public List<OrthologView> getOrthologyBySpecies(List<String> taxonIDs) {

        List<OrthologView> fullOrthologyList = new ArrayList<>();
        taxonIDs.forEach(id -> {
            final List<OrthologView> orthology = cacheService.getCacheEntries(id, CacheAlliance.SPECIES_ORTHOLOGY);
            if (orthology != null)
                fullOrthologyList.addAll(orthology);
        });

        return fullOrthologyList;
    }
    
    /**
     * retrieve InteractionGeneJoin list from cache directly by gene primary key, could be use to get DistinctFieldValueSupplementalData
     * @param geneID
     * @return list of InteractionGeneJoin
     */
    public List<InteractionGeneJoin> getInteractions(String geneID){
        List<InteractionGeneJoin> interactionAnnotationList = cacheService.getCacheEntries(geneID, CacheAlliance.GENE_INTERACTION);
        return interactionAnnotationList ;
    }
}
