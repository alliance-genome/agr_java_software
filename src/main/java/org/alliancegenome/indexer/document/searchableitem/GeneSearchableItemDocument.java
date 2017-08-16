package org.alliancegenome.indexer.document.searchableitem;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(includeFieldNames=true)
public class GeneSearchableItemDocument extends SearchableItemDocument {
	
	private List<String> gene_molecular_function;
	private String taxonId;
	private String symbol;
	private String species;
	private List<String> gene_biological_process;
	private List<String> synonyms;
	private String geneLiteratureUrl;
	private List<CrossReference> crossReferences;
	private List<String> external_ids;
	private String dataProvider;
	private Date dateProduced;
	private List<Disease> diseases;
	private String geneSynopsisUrl;
	private String primaryId;
	private List<GenomeLocation> genomeLocations;
	private String soTermId;
	private List<String> secondaryIds;
	private String soTermName;
	private String release;
	private String geneSynopsis;
	private List<String> gene_cellular_component;
	private List<Orthology> orthology;
	private String geneticEntityExternalUrl;

	@JsonIgnore
	public String getDocumentId() {
		return primaryId;
	}
	
	@Data
	@ToString(includeFieldNames=true)
	private class CrossReference {
		private String crossrefCompleteUrl;
		private String localId;
		private String id;
		private String globalCrossrefId;
	}
	
	@Data
	@ToString(includeFieldNames=true)
	private class Disease {
		private String do_id;
		private String associationType;
		private String dataProvider;
		private String do_name;
		private String qualifier;
		//private List<?> experimentalConditions;
		private Date dataAssigned;
		private String diseaseObjectType;
		private String diseaseObjectName;
		private DoIdDisplay doIdDisplay;
		private List<Evidence> evidence;
		private String taxonId;
		private String geneticSex;
		private String release;
		//private ? modifier;
		private String with;
	}
	
	@Data
	@ToString(includeFieldNames=true)
	private class DoIdDisplay {
		private String displayId;
		private String url;
		private String prefix;
	}
	
	@Data
	@ToString(includeFieldNames=true)
	private class Evidence {
		private List<Publication> pubs;
		private String evidenceCode;
	}
	
	@Data
	@ToString(includeFieldNames=true)
	private class Publication {
		private String pubMedUrl;
		private String pubMedId;
	}
	
	@Data
	@ToString(includeFieldNames=true)
	private class GenomeLocation {
		private String assembly;
		private int start;
		private int end;
		private String strand;
		private String chromosome;
	}
	
	@Data
	@ToString(includeFieldNames=true)
	private class Orthology {
		private int gene2Species;
		private List<String> predictionMethodsNotCalled;
		private boolean isBestScore;
		private boolean isBestRevScore;
		private String gene2Symbol;
		private String gene2SpeciesName;
		private String gene2AgrPrimaryId;
		private String confidence;
		private String gene1SpeciesName;
		private List<String> predictionMethodsMatched;
		private List<String> predictionMethodsNotMatched;
		private int gene1Species;
	}

	
}
