package org.alliancegenome.core.helpers;

import org.alliancegenome.curation_api.model.entities.BiologicalEntity;

public class DiseaseAnnotationHelper {

	private DiseaseAnnotationHelper() { }
	// convenience method to extract the entity name from a biological Entity, which
	// is either
	// gene
	// allele
	// model
	public static String getEntityName(BiologicalEntity entity) {
		if (entity instanceof org.alliancegenome.curation_api.model.entities.Gene gene) {
			return gene.getGeneSymbol().getFormatText();
		}
		if (entity instanceof org.alliancegenome.curation_api.model.entities.Allele allele) {
			return allele.getAlleleSymbol().getFormatText();
		}
		if (entity instanceof org.alliancegenome.curation_api.model.entities.AffectedGenomicModel model) {
			return model.getName();
		}
		return null;
	}

}
