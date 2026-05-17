package org.alliancegenome.core.helper;

import org.alliancegenome.curation_api.model.entities.AffectedGenomicModel;
import org.alliancegenome.curation_api.model.entities.Allele;
import org.alliancegenome.curation_api.model.entities.BiologicalEntity;
import org.alliancegenome.curation_api.model.entities.Gene;

public class DiseaseAnnotationHelper {

	private DiseaseAnnotationHelper() { }
	// convenience method to extract the entity name from a biological Entity, which
	// is either
	// gene
	// allele
	// model
	public static String getEntityName(BiologicalEntity entity) {
		if (entity instanceof Gene gene) {
			return gene.getGeneSymbol().getFormatText();
		}
		if (entity instanceof Allele allele) {
			return allele.getAlleleSymbol().getFormatText();
		}
		if (entity instanceof AffectedGenomicModel model) {
			return model.getAgmFullName().getDisplayText();
		}
		return null;
	}

}
