package org.alliancegenome.api.translators.tdf;

import org.alliancegenome.curation_api.model.entities.ExternalDatabaseReference;
import org.alliancegenome.curation_api.model.entities.InformationContentEntity;
import org.alliancegenome.curation_api.model.entities.Reference;
import org.jetbrains.annotations.Nullable;

public class BaseToTdfTranslator {

	protected BaseToTdfTranslator() {
	}

	@Nullable
	protected static String getReferenceString(InformationContentEntity evidenceItem) {
		String ret = null;
		if (evidenceItem instanceof Reference reference) {
			ret = reference.getReferenceID();
		} else if (evidenceItem instanceof ExternalDatabaseReference externalReference) {
			ret = externalReference.getCurie();
		}
		return ret;
	}

}
