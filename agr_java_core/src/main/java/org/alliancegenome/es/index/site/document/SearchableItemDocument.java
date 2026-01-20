package org.alliancegenome.es.index.site.document;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alliancegenome.curation_api.model.document.es.ESDocument;
import org.alliancegenome.curation_api.view.CurationView;
import org.alliancegenome.es.index.site.doclet.CrossReferenceDoclet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SearchableItemDocument extends ESDocument {

	@JsonView({CurationView.VariantIndexerView.class})
	String alterationType;
	String automatedGeneSynopsis;
	String branch;
	@JsonView({CurationView.VariantIndexerView.class})
	String category;
	@JsonView({CurationView.VariantIndexerView.class})
	String chromosome;
	String dataProvider;
	String description;
	String definition;
	String geneLiteratureUrl;
	String geneSynopsis;
	String geneSynopsisUrl;
	@JsonView({CurationView.VariantIndexerView.class})
	String globalId;
	String href; //GO terms use this rather than modCrossRefCompleteUrl
	String localId;
	@JsonView({CurationView.VariantIndexerView.class})
	String name;
	@JsonView({CurationView.VariantIndexerView.class})
	@JsonProperty("name_key")
	String nameKey;
	String nameText;
	String modCrossRefCompleteUrl;
	@JsonView({CurationView.VariantIndexerView.class})
	String modLocalId;
	Double popularity;
	@JsonView({CurationView.VariantIndexerView.class})
	String primaryKey;
	String soTermId;
	@JsonView({CurationView.VariantIndexerView.class})
	String soTermName;
	@JsonView({CurationView.VariantIndexerView.class})
	String species;
	String summary;
	@JsonView({CurationView.VariantIndexerView.class})
	String symbol;
	@JsonView({CurationView.VariantIndexerView.class})
	String symbolText;
	@JsonView({CurationView.VariantIndexerView.class})
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
	@JsonView({CurationView.VariantIndexerView.class})
	Set<String> chromosomes;
	@JsonView({CurationView.VariantIndexerView.class})
	Set<String> constructs;
	@JsonView({CurationView.VariantIndexerView.class})
	Set<String> constructExpressedComponent;
	@JsonView({CurationView.VariantIndexerView.class})
	Set<String> constructKnockdownComponent;
	@JsonView({CurationView.VariantIndexerView.class})
	Set<String> constructRegulatoryRegion;
	Map<String, List<CrossReferenceDoclet>> crossReferencesMap;
	List<CrossReferenceDoclet> crossReferenceList;
	@JsonView({CurationView.VariantIndexerView.class})
	Set<String> crossReferences;
	@JsonView({CurationView.VariantIndexerView.class})
	Set<String> diseases;
	@JsonView({CurationView.VariantIndexerView.class})
	Set<String> diseasesAgrSlim;
	Set<String> diseaseGroup;
	@JsonView({CurationView.VariantIndexerView.class})
	Set<String> diseasesWithParents;
	Set<String> expressionStages;
	@JsonView({CurationView.VariantIndexerView.class})
	Set<String> alleles;
	@JsonView({CurationView.VariantIndexerView.class})
	Set<String> genes;
	@JsonView({CurationView.VariantIndexerView.class})
	Set<String> geneIds;
	@JsonView({CurationView.VariantIndexerView.class})
	Set<String> geneSynonyms;
	@JsonView({CurationView.VariantIndexerView.class})
	Set<String> geneCrossReferences;
	//Set<String> go_genes;
	//Set<String> go_species;
	Set<String> models;
	@JsonView({CurationView.VariantIndexerView.class})
	Set<String> molecularConsequence;
	Set<String> molecularFunctionAgrSlim;
	Set<String> molecularFunctionWithParents;
	@JsonView({CurationView.VariantIndexerView.class})
	Set<String> parentDiseaseNames;
	@JsonView({CurationView.VariantIndexerView.class})
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
