package org.alliancegenome.api.application;

import com.fasterxml.jackson.annotation.JsonView;

import org.alliancegenome.core.view.PublicView;

// MixIn that adds @JsonView(PublicView.DiseaseAncestor.class) to the curie + name
// accessors on curation_api's OntologyTerm so the disease-ancestors endpoint can
// serialize just those two fields without owning the source class.
public interface OntologyTermAncestorMixIn {

	@JsonView(PublicView.DiseaseAncestor.class)
	String getCurie();

	@JsonView(PublicView.DiseaseAncestor.class)
	String getName();

}
