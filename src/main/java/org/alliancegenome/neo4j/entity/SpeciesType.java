package org.alliancegenome.neo4j.entity;

public enum SpeciesType {
	HUMAN("Homo sapiens", "HUMAN","NCBITaxon:9606","Hsa"),
	RAT("Rattus norvegicus","RGD", "NCBITaxon:10116","Rno"),
	MOUSE("Mus musculus", "MGI","NCBITaxon:10090","Mmu"),
	ZEBRAFISH("Danio rerio", "ZFIN","NCBITaxon:7955","Dre"),
	FLY("Drosophila melanogaster","FB", "NCBITaxon:7227","Dme"),
	WORM("Caenorhabditis elegans", "WB","NCBITaxon:6239","Cel"),
	YEAST("Saccharomyces cerevisiae", "SGD","NCBITaxon:4932","Sce");

	String name;
	String taxonID;
	String displayName;
	String abbreviation;

	public String getName() {
		return name;
	}

	public String getTaxonID() {
		return taxonID;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getAbbreviation() { return abbreviation; }

	SpeciesType(String name, String displayName, String taxinID, String abbreviation) {
		this.name = name;
		this.displayName = displayName;
		this.taxonID = taxinID;
		this.abbreviation = abbreviation;
	}

	public static SpeciesType getTypeByName(String name) {
		for (SpeciesType type : values())
			if (type.name.equals(name))
				return type;
		return null;
	}
}
