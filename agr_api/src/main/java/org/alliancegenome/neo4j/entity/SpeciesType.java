package org.alliancegenome.neo4j.entity;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@AllArgsConstructor
/**
 * This class is being replaced by curation and should no longer be used.
 * This class goes away with neo4j.
 * The functionality has been moved to {@link org.alliancegenome.curation_api.model.entities.Species}.
 *
 * @deprecated and replaced by {@link org.alliancegenome.curation_api.model.entities.Species}
 */
@Deprecated
public enum SpeciesType {
	HUMAN("Homo sapiens", "HUMAN", "NCBITaxon:9606", "Hsa", "HUMAN", "Human", "9606", 1, "GRCh38"), RAT("Rattus norvegicus", "RGD", "NCBITaxon:10116", "Rno", "RGD", "Rat Genome Database", "10116", 2, "mRatBN7.2"),
	MOUSE("Mus musculus", "MGI", "NCBITaxon:10090", "Mmu", "MGD", "Mouse Genome Database", "10090", 3, "GRCm39"), AFRCLAWFROG("Xenopus laevis", "XBXL", "NCBITaxon:8355", "Xla", "XB", "Xenbase", "8355", 4, "XL10.1"),
	WESTCLAWFROG("Xenopus tropicalis", "XBXT", "NCBITaxon:8364", "Xtr", "XB", "Xenbase", "8364", 5, "XT10.0"), ZEBRAFISH("Danio rerio", "ZFIN", "NCBITaxon:7955", "Dre", "ZFIN", "Zebrafish Information Network", "7955", 6, "GRCz11"),
	FLY("Drosophila melanogaster", "FB", "NCBITaxon:7227", "Dme", "FB", "Fly Base", "7227", 7, "R6"), WORM("Caenorhabditis elegans", "WB", "NCBITaxon:6239", "Cel", "WB", "Worm Base", "6239", 8, "WBcel235"),
	YEAST("Saccharomyces cerevisiae", "SGD", "NCBITaxon:559292", "Sce", "SGD", "Saccharomyces Genome Database", "559292", 9, "R64-2-1"), COVID("SARS-CoV-2", // Must be the same as the DB due to lookup from the database
		"Severe acute respiratory syndrome coronavirus 2", // not sure where display Name is used?
		"NCBITaxon:2697049", "SARS-CoV-2", // Up for change?
		"COVID", // Up for change?
		"Severe acute respiratory syndrome coronavirus 2", // Mod name?
		"2697049", 10, null);

	private String name;
	private String displayName;
	private String taxonID;
	private String abbreviation;
	private String modName;
	private String databaseName;
	private String taxonIDPart;
	private int orderID;
	private String assembly;

	@Deprecated
	public static final String NCBITAXON = "NCBITaxon:";

	@Deprecated
	public static SpeciesType getTypeByNameField(String name) {
		for (SpeciesType type : values()) {
			if (type.getName().equals(name)) {
				return type;
			}
		}
		log.warn("SpeciesType Name not found: " + name);
		return null;
	}

	@Deprecated
	public static SpeciesType getTypeByID(String id) {
		for (SpeciesType type : values()) {
			if (type.taxonID.equals(id)) {
				return type;
			}
		}
		log.info("Species could not be found: " + id);
		return null;
	}

	@Deprecated
	private static SpeciesType getTypeByPartialName(String name) {
		List<SpeciesType> species = Arrays.stream(values()).filter(type -> type.name.toLowerCase().contains(name.toLowerCase())).collect(Collectors.toList());
		return species != null && species.size() == 1 ? species.get(0) : null;
	}



	@Deprecated
	public static String getAllTaxonIDs() {
		return List.of(values()).stream().map(SpeciesType::getTaxonID).collect(Collectors.joining(","));
	}

	@Deprecated
	public static String getTaxonId(String species) {
		if (species == null) {
			return null;
		}
		// return name if it already is the full taxon ID
		if (species.startsWith(NCBITAXON)) {
			return species;
		}
		// if only a number is provided then prefix it with taxon...
		if (StringUtils.isNumeric(species)) {
			return NCBITAXON + species;
		}
		SpeciesType typeByName = getTypeByNameField(species);
		if (typeByName != null) {
			return typeByName.getTaxonID();
		}
		typeByName = getTypeByPartialName(species);
		if (typeByName != null) {
			return typeByName.getTaxonID();
		}
		return species;
	}

}
