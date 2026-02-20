package org.alliancegenome.es.index.site.document;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alliancegenome.curation_api.model.document.es.ESDocument;
import org.alliancegenome.es.index.site.doclet.CrossReferenceDoclet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SearchableItemDocument extends ESDocument {


	String alterationType;
	String automatedGeneSynopsis;
	String branch;
	String category;
	String chromosome;
	String dataProvider;
	String description;
	String definition;
	String geneLiteratureUrl;
	String geneSynopsis;
	String geneSynopsisUrl;
	String globalId;
	String href; //GO terms use this rather than modCrossRefCompleteUrl
	String localId;
	String name;
	@JsonProperty("name_key")
	String nameKey;
	String nameText;
	String modCrossRefCompleteUrl;
	String modLocalId;
	Double popularity;
	String primaryKey;
	String soTermId;
	String soTermName;
	String species;
	String summary;
	String symbol;
	String symbolText;
	String variantName;
	Set<String> age;
	Set<String> anatomicalExpression;
	Set<String> anatomicalExpressionWithParents;
	Set<String> assays;
	Set<String> associatedSpecies;
	Set<String> biologicalProcessAgrSlim;
	Set<String> biologicalProcessWithParents;
	Set<String> biotype0;
	Set<String> biotype1;
	Set<String> biotype2;
	Set<String> biotypes;
	Set<String> cellularComponentAgrSlim;
	Set<String> cellularComponentWithParents;
	Set<String> chromosomes;
	Set<String> constructs;
	Set<String> constructExpressedComponent;
	Set<String> constructKnockdownComponent;
	Set<String> constructRegulatoryRegion;
	Map<String, List<CrossReferenceDoclet>> crossReferencesMap;
	List<CrossReferenceDoclet> crossReferenceList;
	Set<String> crossReferences;
	Set<String> diseases;
	Set<String> diseasesAgrSlim;
	Set<String> diseaseGroup;
	Set<String> diseasesWithParents;
	Set<String> expressionStages;
	Set<String> alleles;
	Set<String> genes;
	Set<String> geneIds;
	Set<String> geneSynonyms;
	Set<String> geneCrossReferences;
	//Set<String> go_genes;
	//Set<String> go_species;
	Set<String> models;
	Set<String> molecularConsequence;
	Set<String> molecularFunctionAgrSlim;
	Set<String> molecularFunctionWithParents;
	Set<String> parentDiseaseNames;
	Set<String> phenotypeStatements;
	Set<String> sampleIds;
	Set<String> secondaryIds;
	Set<String> sex;
	Set<String> strictOrthologySymbols;
	Set<String> soTermNameWithParents;
//	  Set<String> stage;
	Set<String> subcellularExpressionWithParents;
	Set<String> subcellularExpressionAgrSlim;
	Set<String> synonyms;
	Set<String> tags;

	//@JsonView({CurationView.VariantIndexerView.class})
	//Set<String> variants;
	//@JsonView({CurationView.VariantIndexerView.class})
	//Set<String> variantSynonyms;
	//@JsonView({CurationView.VariantIndexerView.class})
	//Set<String> variantType;
	
	Set<String> whereExpressed;

	public void setNameKeyWithSpecies(String nameKey, String species) {
		this.nameKey = nameKey;
		if (species != null) {
			this.nameKey += " (" + species + ")";
		}
	}

	@Override
	public String toString() {
		return primaryKey;
	}

}
