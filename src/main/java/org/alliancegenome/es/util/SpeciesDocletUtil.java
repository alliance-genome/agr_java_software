package org.alliancegenome.es.util;

import org.alliancegenome.es.index.site.doclet.SpeciesDoclet;
import org.alliancegenome.neo4j.entity.SpeciesType;

public class SpeciesDocletUtil {

	public static SpeciesDoclet getSpeciesDoclet(SpeciesType type) {
		SpeciesDoclet doclet = new SpeciesDoclet();
		doclet.setTaxonID(type.getTaxonID());
		doclet.setName(type.getName());
		doclet.setDisplayName(type.getDisplayName());
		return doclet;
	}
}
