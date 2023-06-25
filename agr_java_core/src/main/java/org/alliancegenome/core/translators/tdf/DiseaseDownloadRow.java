package org.alliancegenome.core.translators.tdf;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DiseaseDownloadRow {

	private String mainEntityID;
	private String mainEntitySymbol;
	private String entityType;

	private String additionalImplicatedGeneIds;
	private String additionalImplicatedGeneSymbols;
	private String geneticEntityID;
	private String geneticEntityName;
	private String geneticEntityType;
	private String speciesID;
	private String speciesName;
	private String association;
	private String assertedGeneID;
	private String assertedGeneName;
	private String diseaseQualifier;
	private String diseaseGeneticModifierID;
	private String diseaseGeneticModifierName;
	private String diseaseGeneticModifierRelation;
	private String experimentalCondition;
	private String diseaseID;
	private String strainBackgroundID;
	private String strainBackgroundName;
	private String diseaseName;
	private String basedOnID;
	private String geneticSex;
	private String note;
	private String annotationType;
	private String basedOnName;
	private String evidenceCode;
	private String evidenceAbbreviation;
	private String evidenceCodeName;
	private String source;
	private String reference;
	private String dateAssigned;
}
