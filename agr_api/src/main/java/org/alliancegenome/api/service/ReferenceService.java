package org.alliancegenome.api.service;

import org.alliancegenome.api.enums.CrossReferencePrefix;
import org.alliancegenome.curation_api.model.entities.CrossReference;
import org.alliancegenome.curation_api.model.entities.Reference;

import java.util.Optional;

public class ReferenceService {

	public static String getReferenceID(Reference reference) {
		Optional<CrossReference> opt = reference.getCrossReferences().stream().filter(ref -> ref.getCurie().startsWith("PMID:")).findFirst();
		// if no PUBMED ID try MOD ID
		if (opt.isEmpty()) {
			opt = reference.getCrossReferences().stream().filter(ref -> CrossReferencePrefix.valueOf(ref.getPrefix()) != null).findFirst();
		}
		return opt.map(CrossReference::getCurie).orElse(null);
	}

}
