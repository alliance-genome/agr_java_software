package org.alliancegenome.api.service;

import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.neo4j.repository.AlleleRepository;

public class AlleleVariantService {

    public static AlleleRepository repo = new AlleleRepository();
    
    public static boolean allelicVariantExists(AlleleVariantSequence sequence) {
        return repo.getAllAllelicHgvsGNameCache().contains(sequence.getVariant().getHgvsNomenclature());
    }

}
