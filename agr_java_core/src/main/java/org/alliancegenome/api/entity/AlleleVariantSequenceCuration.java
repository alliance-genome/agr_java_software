package org.alliancegenome.api.entity;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.curation_api.model.entities.Allele;
import org.alliancegenome.curation_api.model.entities.PredictedVariantConsequence;
import org.alliancegenome.curation_api.model.entities.Variant;
import org.alliancegenome.curation_api.model.entities.associations.CuratedVariantGenomicLocationAssociation;
import org.alliancegenome.neo4j.view.View;

/**
 * A flattened version of curation API entities for presentational purposes.
 * Uses curation_api.model.entities classes instead of neo4j.entity.node classes.
 * Allele -> multiple Variants -> multiple PredictedVariantConsequence
 */
@Setter
@Getter
public class AlleleVariantSequenceCuration {

	@JsonView({View.Default.class, View.AlleleVariantSequenceCurationForES.class})
	private Allele allele;

	@JsonView({View.Default.class, View.AlleleVariantSequenceCurationForES.class})
	private Variant variant;

	@JsonView({View.Default.class, View.AlleleVariantSequenceCurationForES.class})
	private CuratedVariantGenomicLocationAssociation variantLocation;

	@JsonView({View.Default.class})
	private PredictedVariantConsequence consequence;

	@JsonView({View.Default.class, View.AlleleVariantSequenceCurationForES.class})
	private Boolean searchable = true;

	// Used only for deserialization purposes
	public AlleleVariantSequenceCuration() {
	}

	public AlleleVariantSequenceCuration(Allele allele, Variant variant, PredictedVariantConsequence consequence) {
		this.allele = allele;
		this.variant = variant;
		this.consequence = consequence;
	}

	public AlleleVariantSequenceCuration(Allele allele, Variant variant,
										 CuratedVariantGenomicLocationAssociation variantLocation,
										 PredictedVariantConsequence consequence) {
		this.allele = allele;
		this.variant = variant;
		this.variantLocation = variantLocation;
		this.consequence = consequence;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (allele != null && allele.getAlleleSymbol() != null) {
			builder.append(allele.getAlleleSymbol().getDisplayText());
		}
		if (variant != null && variant.getCurie() != null) {
			builder.append(" : ");
			builder.append(variant.getCurie());
		}
		if (consequence != null && consequence.getVepConsequences() != null && !consequence.getVepConsequences().isEmpty()) {
			builder.append(" : ");
			builder.append(consequence.getVepConsequences().get(0).getName());
		}
		return builder.toString();
	}
}
