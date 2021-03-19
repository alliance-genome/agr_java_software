package org.alliancegenome.api.service;

import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.neo4j.repository.AlleleRepository;

public class AlleleVariantService {

    public static boolean allelicVariantExists(AlleleVariantSequence sequence) {
        return new AlleleRepository().getAllAllelicHgvsGNameCache().contains(sequence.getVariant().getHgvsNomenclature());
    }

}
