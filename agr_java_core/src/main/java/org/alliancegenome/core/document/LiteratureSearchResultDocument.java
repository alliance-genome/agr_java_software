package org.alliancegenome.core.document;

import java.util.Set;

import org.alliancegenome.curation_api.model.document.es.ESDocument;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LiteratureSearchResultDocument extends ESDocument {
	{
		category = "literature_search_result";
		searchable = true;
	}

	private String name;
	private String nameKey;
	private String primaryKey;
	private String abstractText;
	private String citation;
	private String shortCitation;
	private String publicationYear;

	private Set<String> authors;

	private Set<String> alleles;
	private Set<String> genes;
	private Set<String> geneSynonyms;
	private Set<String> geneCrossReferences;
	private Set<String> secondaryIds;
	private Set<String> crossReferences;
	
	private Set<String> models;
	
	private Set<String> variants;
	private Set<String> constructs;

	private Set<String> synonyms;

	private Set<String> diseases;
	private Set<String> diseasesAgrSlim;
	private Set<String> diseasesWithParents;

}

	
