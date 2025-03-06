package org.alliancegenome.core.translators.tdf;

import java.util.List;
import java.util.StringJoiner;

import org.alliancegenome.api.entity.GeneGeneticInteractionDocument;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.InformationContentEntity;
import org.alliancegenome.curation_api.model.entities.Reference;
import org.apache.commons.collections.CollectionUtils;

public class GeneGeneticInteractionToTdfTranslator {

	public String getAllRows(List<GeneGeneticInteractionDocument> interactions) {
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

		headerJoiner.add("Source ID");
		headerJoiner.add("Source DB ID");
		headerJoiner.add("Source DB");
		
		headerJoiner.add("Reference");
		
		headerJoiner.add("Genetic perturbation A");
		headerJoiner.add("Genetic perturbation B");
		headerJoiner.add("Phenotype or trait");

		builder.append(headerJoiner.toString());
		builder.append(ConfigHelper.getJavaLineSeparator());

		interactions.forEach(interaction -> {
			StringJoiner joiner = new StringJoiner("\t");
			joiner.add(interaction.getGeneGeneticInteraction().getInteractorAType().getCurie());
			joiner.add(interaction.getGeneGeneticInteraction().getInteractorAType().getName());
			joiner.add(interaction.getGeneGeneticInteraction().getInteractorARole().getCurie());
			joiner.add(interaction.getGeneGeneticInteraction().getInteractorARole().getName());

			joiner.add(interaction.getGeneGeneticInteraction().getGeneGeneAssociationObject().getIdentifier());
			joiner.add(interaction.getGeneGeneticInteraction().getGeneGeneAssociationObject().getGeneSymbol().getDisplayText());
			joiner.add(interaction.getGeneGeneticInteraction().getGeneGeneAssociationObject().getTaxon().getCurie());
			joiner.add(interaction.getGeneGeneticInteraction().getGeneGeneAssociationObject().getTaxon().getName());
			
			joiner.add(interaction.getGeneGeneticInteraction().getInteractorBType().getCurie());
			joiner.add(interaction.getGeneGeneticInteraction().getInteractorBType().getName());
			joiner.add(interaction.getGeneGeneticInteraction().getInteractorBRole().getCurie());
			joiner.add(interaction.getGeneGeneticInteraction().getInteractorBRole().getName());

			joiner.add(interaction.getGeneGeneticInteraction().getInteractionType().getCurie());
			joiner.add(interaction.getGeneGeneticInteraction().getInteractionType().getName());

			String interactionId = "";
			String interactionSourceCurie = "";
			String interactionSourceName = "";
			if (interaction.getGeneGeneticInteraction().getInteractionId() != null) {
				interactionId = interaction.getGeneGeneticInteraction().getInteractionId();
			}
			if (interaction.getGeneGeneticInteraction().getInteractionSource() != null) {
				interactionSourceCurie = interaction.getGeneGeneticInteraction().getInteractionSource().getCurie();
				interactionSourceName = interaction.getGeneGeneticInteraction().getInteractionSource().getName();
			}
			joiner.add(interactionId);
			joiner.add(interactionSourceCurie);
			joiner.add(interactionSourceName);
			
			String referenceCuries = "";
			if (CollectionUtils.isNotEmpty(interaction.getGeneGeneticInteraction().getEvidence())) {
				StringJoiner referenceJoiner = new StringJoiner(",");
				for (InformationContentEntity ice : interaction.getGeneGeneticInteraction().getEvidence()) {
					Reference reference = (Reference) ice;
					referenceJoiner.add(reference.getReferenceID());
				}
				referenceCuries = referenceJoiner.toString();
			}
			joiner.add(referenceCuries);
	
			String geneticPerturbationASymbol = "";
			String geneticPerturbationBSymbol = "";
			if (interaction.getGeneGeneticInteraction().getInteractorAGeneticPerturbation() != null) {
				geneticPerturbationASymbol = interaction.getGeneGeneticInteraction().getInteractorAGeneticPerturbation().getAlleleSymbol().getDisplayText();
			}
			if (interaction.getGeneGeneticInteraction().getInteractorBGeneticPerturbation() != null) {
				geneticPerturbationBSymbol = interaction.getGeneGeneticInteraction().getInteractorBGeneticPerturbation().getAlleleSymbol().getDisplayText();
			}
			joiner.add(geneticPerturbationASymbol);
			joiner.add(geneticPerturbationBSymbol);
			
			String phenotypesOrTraits = "";
			if (CollectionUtils.isNotEmpty(interaction.getGeneGeneticInteraction().getPhenotypesOrTraits())) {
				StringJoiner phenotypeJoiner = new StringJoiner(",");
				interaction.getGeneGeneticInteraction().getPhenotypesOrTraits().forEach(phenotype -> phenotypeJoiner.add(phenotype));
				phenotypesOrTraits = phenotypeJoiner.toString();
			}
			joiner.add(phenotypesOrTraits);
	
			builder.append(joiner.toString());
			builder.append(ConfigHelper.getJavaLineSeparator());
		});

		return builder.toString();
	}
}
