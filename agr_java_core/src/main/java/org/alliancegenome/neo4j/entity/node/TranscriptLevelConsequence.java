package org.alliancegenome.neo4j.entity.node;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.alliancegenome.core.helpers.VariantServiceHelper;
import org.alliancegenome.curation_api.view.CurationView;
import org.alliancegenome.neo4j.entity.Neo4jEntity;
import org.alliancegenome.neo4j.view.PublicView;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NodeEntity(label = "TranscriptLevelConsequence")
@Getter
@Setter
@Schema(name = "TranscriptLevelConsequence", description = "POJO that represents Transcript Level Consequences")
public class TranscriptLevelConsequence extends Neo4jEntity {

	private static ConcurrentHashMap<String, Transcript> transcriptCache = new ConcurrentHashMap<>();

	@JsonView({ PublicView.API.class, PublicView.GeneAlleleVariantSequenceAPI.class, CurationView.VariantSummaryDocument.class }) private List<String> molecularConsequences;

	@JsonView({ PublicView.Default.class, CurationView.VariantSummaryDocument.class }) private String impact;

	@JsonView({ PublicView.API.class, CurationView.VariantSummaryDocument.class }) private String aminoAcidChange;

	@JsonView({ PublicView.API.class, CurationView.VariantSummaryDocument.class }) private String aminoAcidVariation;

	@JsonView({ PublicView.API.class, CurationView.VariantSummaryDocument.class }) private String aminoAcidReference;

	@JsonView({ PublicView.API.class, CurationView.VariantSummaryDocument.class }) private String codonChange;

	@JsonView({ PublicView.API.class, CurationView.VariantSummaryDocument.class }) private String codonReference;

	@JsonView({ PublicView.API.class, CurationView.VariantSummaryDocument.class }) private String codonVariation;

	@JsonView({ PublicView.API.class, CurationView.VariantSummaryDocument.class }) private String cdsStartPosition;

	@JsonView({ PublicView.API.class }) private String cdsEndPosition;

	@JsonView({ PublicView.API.class, CurationView.VariantSummaryDocument.class }) private String cdnaStartPosition;

	@JsonView({ PublicView.API.class }) private String cdnaEndPosition;

	@JsonView({ PublicView.API.class, CurationView.VariantSummaryDocument.class }) private String proteinStartPosition;

	@JsonView({ PublicView.API.class }) private String proteinEndPosition;

	@JsonView({ PublicView.API.class, CurationView.VariantSummaryDocument.class }) private String hgvsProteinNomenclature;

	@JsonView({ PublicView.API.class, CurationView.VariantSummaryDocument.class }) private String hgvsCodingNomenclature;

	@JsonView({ PublicView.API.class, CurationView.VariantSummaryDocument.class }) private String hgvsVEPGeneNomenclature;

	@JsonView({ PublicView.Default.class, CurationView.VariantSummaryDocument.class }) private String siftPrediction;

	@JsonView({ PublicView.Default.class, CurationView.VariantSummaryDocument.class }) private String polyphenPrediction;

	@JsonView({ PublicView.Default.class, CurationView.VariantSummaryDocument.class }) private String siftScore;

	@JsonView({ PublicView.Default.class, CurationView.VariantSummaryDocument.class }) private String polyphenScore;

	@JsonView({ PublicView.Default.class, CurationView.VariantSummaryDocument.class }) private String sequenceFeatureType;

	@Relationship(type = "ASSOCIATION", direction = Relationship.Direction.INCOMING) private Variant variant;

	@Relationship(type = "ASSOCIATION", direction = Relationship.Direction.INCOMING)

	@JsonView({ CurationView.VariantSummaryDocument.class, PublicView.GeneAlleleVariantSequenceAPI.class }) private Transcript transcript;

	@JsonView({ PublicView.Default.class, CurationView.VariantSummaryDocument.class }) private String location;

	@JsonView({ PublicView.GeneAlleleVariantSequenceAPI.class, CurationView.VariantSummaryDocument.class }) private Gene associatedGene;

	public void setLocation(String name) {
		location = name;
	}

	@JsonView({ PublicView.GeneAlleleVariantSequenceAPI.class, CurationView.VariantSummaryDocument.class })
	public String getLocation() {
		if (StringUtils.isNotEmpty(location)) {
			return location;
		}
		if (transcript == null || variant == null) {
			return "";
		}
		VariantServiceHelper.populateIntronExonLocation(variant, transcript);
		location = transcript.getIntronExonLocation();
		return location;
	}

	@JsonView({ PublicView.GeneAlleleVariantSequenceAPI.class, CurationView.VariantSummaryDocument.class })
	public String getSequenceFeatureType() {
		if (StringUtils.isNotEmpty(sequenceFeatureType)) {
			return sequenceFeatureType;
		}
		if (transcript == null) {
			return "";
		}
		if (transcript.getType() != null) {
			sequenceFeatureType = transcript.getType().getName();
		}
		return sequenceFeatureType;
	}

	public void setSequenceFeatureType(String sequenceFeatureType) {
		this.sequenceFeatureType = sequenceFeatureType;
	}

	public TranscriptLevelConsequence() {
	}

	@JsonView({ PublicView.API.class, CurationView.VariantSummaryDocument.class }) private String geneLevelConsequence;

}
