package org.alliancegenome.core.translators.doclet;

import org.alliancegenome.core.translators.EntityDocletTranslator;
import org.alliancegenome.es.index.site.doclet.CrossReferenceDoclet;
import org.alliancegenome.neo4j.entity.node.CrossReference;

public class CrossReferenceDocletTranslator extends EntityDocletTranslator<CrossReference, CrossReferenceDoclet> {

	@Override
	protected CrossReferenceDoclet entityToDocument(CrossReference entity, int translationDepth) {
		CrossReferenceDoclet crd = new CrossReferenceDoclet();
		crd.setCrossRefCompleteUrl(entity.getCrossRefCompleteUrl());
		crd.setName(entity.getName());
		crd.setGlobalCrossRefId(entity.getGlobalCrossRefId());
		crd.setLocalId(entity.getLocalId());
		crd.setPrefix(entity.getPrefix());
		crd.setType(entity.getCrossRefType());
		return crd;
	}

	@Override
	protected CrossReference documentToEntity(CrossReferenceDoclet doument, int translationDepth) {
		
		return null;
	}

}
