package org.alliancegenome.es.index.data.enums;

import org.alliancegenome.es.index.data.doclet.DataTypeDoclet;

import lombok.Getter;

@Getter
public enum DataType {
	// Default data type if index does not contain the data type then this will be used to inject a document
	BGI("Basic Gene Information", "json", true, true),
	DOA("Disease Ontology Annotations", "json", true, true),
	ORTHO("Orthology", "json", true, true),
	ALLELE("Allele Information", "json", true, true),
	GENOTYPE("Genotype Information", "json", true, true),

	// No schema required for these but will still stick them in the correct schema directory
	GOA("Gene Ontology Annotations", "gaf", true, false),
	GFF("Gene Features File", "gff", true, false),

	// No verification yet for these either
	DO("Disease Ontology", "obo", false, false),
	GO("Gene Ontology", "obo", false, false),
	SO("Sequence Ontology", "obo", false, false),
	;

	private boolean taxonIdRequired;
	private boolean validationRequired;
	private String fileExtension;
	private String description;

	private DataType(String description, String fileExtension, boolean taxonIdRequired, boolean validationRequired) {
		this.description = description;
		this.fileExtension = fileExtension;
		this.taxonIdRequired = taxonIdRequired;
		this.validationRequired = validationRequired;
	}

	public static DataTypeDoclet fromString(String string) {
		for(DataType dt: DataType.values()) {
			if(dt.name().toLowerCase().equals(string.toLowerCase())) {
				return getDoclet(dt);
			}
		}
		return null;
	}

	public static DataTypeDoclet getDoclet(DataType type) {
		DataTypeDoclet ret = new DataTypeDoclet();
		ret.setDescription(type.getDescription());
		ret.setFileExtension(type.getFileExtension());
		ret.setTaxonIdRequired(type.isTaxonIdRequired());
		ret.setName(type.name());
		ret.setValidationRequired(type.isValidationRequired());
		return ret;
	}

}
