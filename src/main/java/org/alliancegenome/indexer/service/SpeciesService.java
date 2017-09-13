package org.alliancegenome.indexer.service;

import org.alliancegenome.indexer.document.SpeciesDoclet;
import org.alliancegenome.indexer.entity.SpeciesType;

public class SpeciesService {

    public static SpeciesDoclet getSpeciesDoclet(SpeciesType type) {
        SpeciesDoclet doclet = new SpeciesDoclet();
        doclet.setTaxonID(type.getTaxonID());
        doclet.setName(type.getName());
        doclet.setDisplayName(type.getDisplayName());
        return doclet;
    }
}
