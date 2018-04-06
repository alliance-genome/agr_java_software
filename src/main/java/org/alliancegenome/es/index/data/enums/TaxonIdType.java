package org.alliancegenome.es.index.data.enums;

import org.alliancegenome.es.index.data.document.TaxonIdDoclet;

import lombok.Getter;

@Getter
public enum TaxonIdType {
	// Default mod if index does not contain the mods then this will be used to inject a document
	FB("Fly Base", "7227"),
	Human("Human", "9606"),
	MGD("Mouse Genome Database", "10090"),
	RGD("Rat Genome Database", "10116"),
	SGD("Saccharomyces Genome Database", "4932"),
	WB("Worm Base", "6239"),
	ZFIN("Zebrafish Information Network", "7955"),
	;

	private String description;
	private String taxonId;

	private TaxonIdType(String description, String taxonId) {
		this.description = description;
		this.taxonId = taxonId;
	}
	
	public static TaxonIdDoclet fromModName(String modName) {
		for(TaxonIdType taxon: TaxonIdType.values()) {
			if(taxon.name().equals(modName)) {
				return getDoclet(taxon);
			}
		}
		return null;
	}

	public static TaxonIdDoclet fromTaxonId(String taxonId) {
		for(TaxonIdType taxon: TaxonIdType.values()) {
			if(taxon.taxonId.equals(taxonId)) {
				return getDoclet(taxon);
			}
		}
		return null;
	}

	public static TaxonIdDoclet getDoclet(TaxonIdType type) {
		TaxonIdDoclet ret = new TaxonIdDoclet();
		ret.setModName(type.name());
		ret.setDescription(type.getDescription());
		ret.setTaxonId(type.getTaxonId());
		return ret;
	}
}
