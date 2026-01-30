package org.alliancegenome.es.index.site.document;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alliancegenome.curation_api.model.document.es.ESDocument;
import org.alliancegenome.es.index.site.doclet.CrossReferenceDoclet;
import org.alliancegenome.neo4j.view.PublicView;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SearchableItemDocument extends ESDocument {

	@JsonView({PublicView.AlleleVariantSequenceConverterForES.class})
	String alterationType;
	String automatedGeneSynopsis;
	String branch;
	@JsonView({PublicView.AlleleVariantSequenceConverterForES.class})
	String category;
	@JsonView({PublicView.AlleleVariantSequenceConverterForES.class})
	String chromosome;
	String dataProvider;
	String description;
	String definition;
	String geneLiteratureUrl;
	String geneSynopsis;
	String geneSynopsisUrl;
	@JsonView({PublicView.AlleleVariantSequenceConverterForES.class})
	String globalId;
	String href; //GO terms use this rather than modCrossRefCompleteUrl
	String localId;
	@JsonView({PublicView.AlleleVariantSequenceConverterForES.class})
	String name;
	@JsonView({PublicView.AlleleVariantSequenceConverterForES.class})
	@JsonProperty("name_key")
	String nameKey;
	String nameText;
	String modCrossRefCompleteUrl;
	@JsonView({PublicView.AlleleVariantSequenceConverterForES.class})
	String modLocalId;
	Double popularity;
	@JsonView({PublicView.AlleleVariantSequenceConverterForES.class})
	String primaryKey;
	String soTermId;
	@JsonView({PublicView.AlleleVariantSequenceConverterForES.class})
	String soTermName;
	@JsonView({PublicView.AlleleVariantSequenceConverterForES.class})
	String species;
	String summary;
	@JsonView({PublicView.AlleleVariantSequenceConverterForES.class})
	String symbol;
	@JsonView({PublicView.AlleleVariantSequenceConverterForES.class})
	String symbolText;
	@JsonView({PublicView.AlleleVariantSequenceConverterForES.class})
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
	@JsonView({PublicView.AlleleVariantSequenceConverterForES.class})
	Set<String> chromosomes;
	@JsonView({PublicView.AlleleVariantSequenceConverterForES.class})
	Set<String> constructs;
	@JsonView({PublicView.AlleleVariantSequenceConverterForES.class})
	Set<String> constructExpressedComponent;
	@JsonView({PublicView.AlleleVariantSequenceConverterForES.class})
	Set<String> constructKnockdownComponent;
	@JsonView({PublicView.AlleleVariantSequenceConverterForES.class})
	Set<String> constructRegulatoryRegion;
	Map<String, List<CrossReferenceDoclet>> crossReferencesMap;
	List<CrossReferenceDoclet> crossReferenceList;
	@JsonView({PublicView.AlleleVariantSequenceConverterForES.class})
	Set<String> crossReferences;
	@JsonView({PublicView.AlleleVariantSequenceConverterForES.class})
	Set<String> diseases;
	@JsonView({PublicView.AlleleVariantSequenceConverterForES.class})
	Set<String> diseasesAgrSlim;
	Set<String> diseaseGroup;
	@JsonView({PublicView.AlleleVariantSequenceConverterForES.class})
	Set<String> diseasesWithParents;
	Set<String> expressionStages;
	@JsonView({PublicView.AlleleVariantSequenceConverterForES.class})
	Set<String> alleles;
	@JsonView({PublicView.AlleleVariantSequenceConverterForES.class})
	Set<String> genes;
	@JsonView({PublicView.AlleleVariantSequenceConverterForES.class})
	Set<String> geneIds;
	@JsonView({PublicView.AlleleVariantSequenceConverterForES.class})
	Set<String> geneSynonyms;
	@JsonView({PublicView.AlleleVariantSequenceConverterForES.class})
	Set<String> geneCrossReferences;
	//Set<String> go_genes;
	//Set<String> go_species;
	Set<String> models;
	@JsonView({PublicView.AlleleVariantSequenceConverterForES.class})
	Set<String> molecularConsequence;
	Set<String> molecularFunctionAgrSlim;
	Set<String> molecularFunctionWithParents;
	@JsonView({PublicView.AlleleVariantSequenceConverterForES.class})
	Set<String> parentDiseaseNames;
	@JsonView({PublicView.AlleleVariantSequenceConverterForES.class})
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
	@JsonView({PublicView.AlleleVariantSequenceConverterForES.class})
	Set<String> variants;
	@JsonView({PublicView.AlleleVariantSequenceConverterForES.class})
	Set<String> variantSynonyms;
	@JsonView({PublicView.AlleleVariantSequenceConverterForES.class})
	Set<String> variantType;
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
