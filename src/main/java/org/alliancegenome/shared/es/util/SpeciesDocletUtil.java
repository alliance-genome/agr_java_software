package org.alliancegenome.shared.es.util;

import org.alliancegenome.shared.es.document.site_index.SpeciesDoclet;
import org.alliancegenome.shared.neo4j.entity.SpeciesType;

public class SpeciesDocletUtil {

	public static SpeciesDoclet getSpeciesDoclet(SpeciesType type) {
		SpeciesDoclet doclet = new SpeciesDoclet();
		doclet.setTaxonID(type.getTaxonID());
		doclet.setName(type.getName());
		doclet.setDisplayName(type.getDisplayName());
		return doclet;
	}
}
