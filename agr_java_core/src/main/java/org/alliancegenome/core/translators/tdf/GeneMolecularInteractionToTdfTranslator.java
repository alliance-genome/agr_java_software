package org.alliancegenome.core.translators.tdf;

import java.util.List;
import java.util.StringJoiner;

import org.alliancegenome.api.entity.GeneMolecularInteractionDocument;
import org.alliancegenome.api.entity.JoinTypeValue;
import org.alliancegenome.core.config.ConfigHelper;
import org.apache.commons.collections.CollectionUtils;

public class GeneMolecularInteractionToTdfTranslator {

	public String getAllRows(List<GeneMolecularInteractionDocument> interactions) {
		StringBuilder builder = new StringBuilder();
		StringJoiner headerJoiner = new StringJoiner("\t");
		
		headerJoiner.add("Focus gene molecule type ID");
		headerJoiner.add("Focus gene molecule type");
		headerJoiner.add("Focus gene experimental role ID");
		headerJoiner.add("Focus gene experimental role");

		headerJoiner.add("Interactor gene ID");
		headerJoiner.add("Interactor gene");
		headerJoiner.add("Interactor species ID");
		headerJoiner.add("Interactor species");

		headerJoiner.add("Interactor molecule type ID");
		headerJoiner.add("Interactor molecule type");
		headerJoiner.add("Interactor experimental role ID");
		headerJoiner.add("Interactor experimental role");

		headerJoiner.add("Interaction type ID");
		headerJoiner.add("Interaction type");

		headerJoiner.add("Detection method IDs");
		headerJoiner.add("Detection methods");

		headerJoiner.add("Source ID");
		headerJoiner.add("Source DB ID");
		headerJoiner.add("Source DB");
		headerJoiner.add("Aggregation DB ID");
		headerJoiner.add("Aggregation DB");
		headerJoiner.add("Reference");

		builder.append(headerJoiner.toString());
		builder.append(ConfigHelper.getJavaLineSeparator());

		interactions.forEach(interaction -> {
			StringJoiner joiner = new StringJoiner("\t");
			joiner.add(interaction.getGeneMolecularInteraction().getInteractorAType().getCurie());
			joiner.add(interaction.getGeneMolecularInteraction().getInteractorAType().getName());
			joiner.add(interaction.getGeneMolecularInteraction().getInteractorARole().getCurie());
			joiner.add(interaction.getGeneMolecularInteraction().getInteractorARole().getName());

			joiner.add(interaction.getGeneMolecularInteraction().getGeneGeneAssociationObject().getIdentifier());
			joiner.add(interaction.getGeneMolecularInteraction().getGeneGeneAssociationObject().getGeneSymbol().getDisplayText());
			joiner.add(interaction.getGeneMolecularInteraction().getGeneGeneAssociationObject().getTaxon().getCurie());
			joiner.add(interaction.getGeneMolecularInteraction().getGeneGeneAssociationObject().getTaxon().getName());
			
			joiner.add(interaction.getGeneMolecularInteraction().getInteractorBType().getCurie());
			joiner.add(interaction.getGeneMolecularInteraction().getInteractorBType().getName());
			joiner.add(interaction.getGeneMolecularInteraction().getInteractorBRole().getCurie());
			joiner.add(interaction.getGeneMolecularInteraction().getInteractorBRole().getName());

			joiner.add(interaction.getGeneMolecularInteraction().getInteractionType().getCurie());
			joiner.add(interaction.getGeneMolecularInteraction().getInteractionType().getName());

			String detectionMethodCurie = "";
			String detectionMethodName = "";
			if (interaction.getGeneMolecularInteraction().getDetectionMethod() != null) {
				detectionMethodCurie = interaction.getGeneMolecularInteraction().getDetectionMethod().getCurie();
				detectionMethodName = interaction.getGeneMolecularInteraction().getDetectionMethod().getName();
			}
			joiner.add(detectionMethodCurie);
			joiner.add(detectionMethodName);
			
			
			String interactionId = "";
			String interactionSourceCurie = "";
			String interactionSourceName = "";
			if (interaction.getGeneMolecularInteraction().getInteractionId() != null) {
				interactionId = interaction.getGeneMolecularInteraction().getInteractionId();
			}
			if (interaction.getGeneMolecularInteraction().getInteractionSource() != null) {
				interactionSourceCurie = interaction.getGeneMolecularInteraction().getInteractionSource().getCurie();
				interactionSourceName = interaction.getGeneMolecularInteraction().getInteractionSource().getName();
			}
			joiner.add(interactionId);
			joiner.add(interactionSourceCurie);
			joiner.add(interactionSourceName);
			
			String aggregationDatabaseCurie = "";
			String aggregationDatabaseName = "";
			if (interaction.getGeneMolecularInteraction().getAggregationDatabase() != null) {
				aggregationDatabaseCurie = interaction.getGeneMolecularInteraction().getAggregationDatabase().getCurie();
				aggregationDatabaseName = interaction.getGeneMolecularInteraction().getAggregationDatabase().getName();
			}
			joiner.add(aggregationDatabaseCurie);
			joiner.add(aggregationDatabaseName);
			
			String referenceCuries = "";
			if (CollectionUtils.isNotEmpty(interaction.getGeneMolecularInteraction().getEvidence())) {
				StringJoiner referenceJoiner = new StringJoiner(",");
				interaction.getGeneMolecularInteraction().getEvidence().forEach(reference -> referenceJoiner.add(reference.getCurie()));
				referenceCuries = referenceJoiner.toString();
			}
			joiner.add(referenceCuries);
			
			builder.append(joiner.toString());
			builder.append(ConfigHelper.getJavaLineSeparator());
		});

		return builder.toString();
	}
}
