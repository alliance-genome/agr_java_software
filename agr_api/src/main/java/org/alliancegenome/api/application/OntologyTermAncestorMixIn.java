package org.alliancegenome.api.application;

import com.fasterxml.jackson.annotation.JsonView;

import org.alliancegenome.core.view.PublicView;

// MixIn applied globally to curation_api's OntologyTerm. DiseaseAncestor lets the
// disease-ancestors endpoint serialize just curie + name; Default (the base view all
// other PublicView views extend) keeps curie + name serializing on every other public
// endpoint, e.g. gene molecular/genetic interactions.
public interface OntologyTermAncestorMixIn {

	@JsonView({PublicView.Default.class, PublicView.DiseaseAncestor.class})
	String getCurie();

	@JsonView({PublicView.Default.class, PublicView.DiseaseAncestor.class})
	String getName();

}
