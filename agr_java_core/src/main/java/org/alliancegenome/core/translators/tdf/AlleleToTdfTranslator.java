package org.alliancegenome.core.translators.tdf;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.api.entity.GeneTransgenicAlleleSummaryDocument;
import org.alliancegenome.api.entity.TransgenicAlleleSummaryDocument;
import org.alliancegenome.curation_api.model.document.es.AVSParentDocument;
import org.alliancegenome.curation_api.model.document.es.AlleleSummaryDocument;
import org.alliancegenome.curation_api.model.document.es.ESDocument;
import org.alliancegenome.curation_api.model.document.es.VariantSummaryDocument;
import org.alliancegenome.curation_api.model.entities.Allele;
import org.alliancegenome.curation_api.model.entities.CrossReference;
import org.alliancegenome.curation_api.model.entities.Note;
import org.alliancegenome.curation_api.model.entities.Reference;
import org.alliancegenome.curation_api.model.entities.TransgenicAlleleConstruct;
import org.alliancegenome.curation_api.model.entities.Variant;
import org.alliancegenome.curation_api.model.entities.PredictedVariantConsequence;
import org.alliancegenome.curation_api.model.entities.associations.CuratedVariantGenomicLocationAssociation;
import org.alliancegenome.curation_api.model.entities.ontology.SOTerm;
import org.apache.commons.collections.CollectionUtils;

public class AlleleToTdfTranslator {

	public String getAllRows(List<ESDocument> annotations) {

		List<AlleleDownloadRow> list = getAlleleDownloadRowsForGenes(annotations);
		List<DownloadHeader> headers = List.of(
			new DownloadHeader<>("Allele ID", AlleleDownloadRow::getAlleleID),
			new DownloadHeader<>("Allele Symbol", AlleleDownloadRow::getAlleleSymbol),
			new DownloadHeader<>("Allele Synonyms", AlleleDownloadRow::getAlleleSynonyms),
			new DownloadHeader<>("Category", AlleleDownloadRow::getVariantCategory),
			new DownloadHeader<>("Variant Symbol", AlleleDownloadRow::getVariantSymbol),
			new DownloadHeader<>("Variant Type", AlleleDownloadRow::getVariantType),
			new DownloadHeader<>("Variant Consequence", AlleleDownloadRow::getVariantConsequence),
			new DownloadHeader<>("Has Disease", AlleleDownloadRow::getHasDisease),
			new DownloadHeader<>("Has Phenotype", AlleleDownloadRow::getHasPhenotype)
		);

		return DownloadHeader.getDownloadOutput(list, headers);
	}

	public List<AlleleDownloadRow> getAlleleDownloadRowsForGenes(List<ESDocument> annotations) {
		return annotations.stream()
			.map(annotation -> {
				AVSParentDocument doc = (AVSParentDocument) annotation;
				List<Variant> variants = getVariants(doc);
				if (CollectionUtils.isNotEmpty(variants)) {
					return variants.stream()
						.map(var -> getBaseDownloadRow(doc, var))
						.collect(Collectors.toList());
				} else {
					return List.of(getBaseDownloadRow(doc, null));
				}
			})
			.flatMap(Collection::stream)
			.collect(Collectors.toList());
	}

	private List<Variant> getVariants(AVSParentDocument doc) {
		return switch (doc) {
			case AlleleSummaryDocument asd -> asd.getVariants();
			case VariantSummaryDocument vsd -> vsd.getVariants();
			default -> null;
		};
	}

	public List<AlleleVariantSequenceDownloadRow> alleleVariantSequenceDownloadRow(List<AlleleVariantSequence> annotations) {
		return annotations.stream()
			.map(this::getBaseAlleleVariantDownloadRow)
			.collect(Collectors.toList());
	}


	private AlleleDownloadRow getBaseDownloadRow(AVSParentDocument annotation, Variant variant) {
		AlleleDownloadRow row = new AlleleDownloadRow();
		Allele allele = annotation.getAllele();

		row.setAlleleID(allele != null ? allele.getIdentifier() : "");
		row.setAlleleSymbol(allele != null && allele.getAlleleSymbol() != null ? allele.getAlleleSymbol().getDisplayText() : "");
		if (allele != null && CollectionUtils.isNotEmpty(allele.getAlleleSynonyms())) {
			row.setAlleleSynonyms(allele.getAlleleSynonyms().stream()
				.map(syn -> syn.getDisplayText())
				.collect(Collectors.joining(",")));
		}
		row.setVariantCategory(annotation.getAlterationType());

		if (variant != null) {
			List<CuratedVariantGenomicLocationAssociation> locations = variant.getCuratedVariantGenomicLocations();
			if (CollectionUtils.isNotEmpty(locations)) {
				CuratedVariantGenomicLocationAssociation location = locations.get(0);

				if (annotation instanceof VariantSummaryDocument) {
					row.setAlleleID(location.getHgvs());
					row.setAlleleSymbol(location.getHgvs());
				}

				row.setVariantSymbol(location.getHgvs());

				List<PredictedVariantConsequence> pvcs = location.getPredictedVariantConsequences();
				if (CollectionUtils.isNotEmpty(pvcs) && CollectionUtils.isNotEmpty(pvcs.get(0).getVepConsequences())) {
					row.setVariantConsequence(pvcs.get(0).getVepConsequences().stream()
						.filter(Objects::nonNull)
						.map(SOTerm::getName)
						.distinct()
						.collect(Collectors.joining("|")));
				}
			}
			if (variant.getVariantType() != null) {
				row.setVariantType(variant.getVariantType().getName());
			}
		}

		row.setHasPhenotype(annotation.getHasPhenotype() != null ? annotation.getHasPhenotype().toString() : "false");
		row.setHasDisease(annotation.getHasDisease() != null ? annotation.getHasDisease().toString() : "false");
		return row;
	}

	private AlleleVariantSequenceDownloadRow getBaseAlleleVariantDownloadRow(AlleleVariantSequence annotation) {
		AlleleVariantSequenceDownloadRow row = new AlleleVariantSequenceDownloadRow();

		row.setAlleleID(annotation.getAllele().getPrimaryKey());
		row.setAlleleSymbol(annotation.getAllele().getSymbol());
		String synonyms = "";
		if (CollectionUtils.isNotEmpty(annotation.getAllele().getSynonyms())) {
			StringJoiner synonymJoiner = new StringJoiner(",");
			annotation.getAllele().getSynonyms().forEach(synonym -> synonymJoiner.add(synonym.getName()));
			synonyms = synonymJoiner.toString();
		}
		row.setAlleleSynonyms(synonyms);
		row.setVariantCategory(annotation.getAllele().getCategory());
		row.setHasDisease(annotation.getAllele().hasDisease().toString());
		row.setHasPhenotype(annotation.getAllele().hasPhenotype().toString());
		if (annotation.getVariant() != null) {
			row.setHgvsgName(annotation.getVariant().getHgvsNomenclature());
			row.setVariantType(annotation.getVariant().getVariantType().getName());
		}
		if (annotation.getConsequence() != null) {
			row.setMolecularConsequences(annotation.getConsequence().getMolecularConsequences());
			row.setSequenceFeature(annotation.getConsequence().getTranscript().getName());
			row.setSequenceFeatureType(annotation.getConsequence().getSequenceFeatureType());
			row.setLocation(annotation.getConsequence().getLocation());
			row.setVepImpact(annotation.getConsequence().getImpact());
			row.setSiftPrediction(annotation.getConsequence().getSiftPrediction());
			row.setSiftScore(annotation.getConsequence().getSiftScore());
			row.setPolyphenPrediction(annotation.getConsequence().getPolyphenPrediction());
			row.setPolyphenScore(annotation.getConsequence().getPolyphenScore());
			if (annotation.getConsequence().getAssociatedGene() != null) {
				row.setSequenceFeatureAssociatedGene(annotation.getConsequence().getAssociatedGene().getSymbol());
				row.setSequenceFeatureAssociatedGeneID(annotation.getConsequence().getAssociatedGene().getPrimaryKey());
			}
		}
		return row;
	}

	public String getAllTransgenicAlleleRows(List<GeneTransgenicAlleleSummaryDocument> annotations) {

		List<TransgenicAlleleDownloadRow> list = getTransgenicAlleleDownloadRowsForGenes(annotations);
		List<DownloadHeader> headers;
		headers = List.of(
			new DownloadHeader<>("Species", TransgenicAlleleDownloadRow::getSpecies),
			new DownloadHeader<>("Allele ID", TransgenicAlleleDownloadRow::getAlleleID),
			new DownloadHeader<>("Allele Symbol", TransgenicAlleleDownloadRow::getAlleleSymbol),
			new DownloadHeader<>("Transgenic Construct ID", TransgenicAlleleDownloadRow::getTgConstructID),
			new DownloadHeader<>("Transgenic Construct", TransgenicAlleleDownloadRow::getTransgenicConstruct),
			new DownloadHeader<>("Expressed Gene ID", TransgenicAlleleDownloadRow::getExpGeneID),
			new DownloadHeader<>("Expressed Gene", TransgenicAlleleDownloadRow::getExpressedGene),
			new DownloadHeader<>("Knockdown Target ID", TransgenicAlleleDownloadRow::getTargetID),
			new DownloadHeader<>("Knockdown Target", TransgenicAlleleDownloadRow::getKnockdownTarget),
			new DownloadHeader<>("Regulatory Region ID", TransgenicAlleleDownloadRow::getRegulatoryRegionID),
			new DownloadHeader<>("Regulatory Region", TransgenicAlleleDownloadRow::getRegulatoryRegion),
			new DownloadHeader<>("Has Phenotype", TransgenicAlleleDownloadRow::getHasPhenotype),
			new DownloadHeader<>("Has Disease", TransgenicAlleleDownloadRow::getHasDisease)
		);

		return DownloadHeader.getDownloadOutput(list, headers);
	}

	public List<TransgenicAlleleDownloadRow> getTransgenicAlleleDownloadRowsForGenes(List<GeneTransgenicAlleleSummaryDocument> annotations) {

		return annotations.stream()
			.map(annotation -> {
				List<TransgenicAlleleConstruct> transgenicAlleleConstructs = annotation.getAlleleDocument().getTransgenicAlleleConstructs();
				if (CollectionUtils.isNotEmpty(transgenicAlleleConstructs)) {
					return transgenicAlleleConstructs.stream()
						.map(transgenicAlleleConstruct ->
							getBaseDownloadAlleleTransgenicRow(annotation.getAlleleDocument(), transgenicAlleleConstruct, null))
						.toList();
				} else {
					return List.of(getBaseDownloadAlleleTransgenicRow(annotation.getAlleleDocument(), null, null));
				}
			})
			.flatMap(Collection::stream)
			.collect(Collectors.toList());


	}

	private TransgenicAlleleDownloadRow getBaseDownloadAlleleTransgenicRow(TransgenicAlleleSummaryDocument transgenicAlleleSummaryDocument, TransgenicAlleleConstruct transgenicAlleleConstruct, Reference pub) {
		TransgenicAlleleDownloadRow row = new TransgenicAlleleDownloadRow();
		Allele allele = transgenicAlleleSummaryDocument.getAllele();

		row.setSpecies(allele.getTaxon().getName());
		row.setAlleleID(allele.getPrimaryExternalId());
		row.setAlleleSymbol(allele.getAlleleSymbol().getFormatText());
		row.setTgConstructID(transgenicAlleleConstruct.getConstruct().getPrimaryExternalId());
		// do not print construct name if placeholder = true
		if (transgenicAlleleConstruct.getConstruct().getPlaceholder() == null || !transgenicAlleleConstruct.getConstruct().getPlaceholder()) {
			row.setTransgenicConstruct(transgenicAlleleConstruct.getConstruct().getConstructSymbol().getFormatText());
		}
		String expressedGene = "";
		String expGeneID = "";
		String targetGene = "";
		String tgtGeneID = "";
		String regGene = "";
		String regGeneID = "";
		if (transgenicAlleleConstruct != null) {
			if (CollectionUtils.isNotEmpty(transgenicAlleleConstruct.getExpressedGenes())) {
				StringJoiner expGeneJoiner = new StringJoiner(",");
				StringJoiner expGeneIDJoiner = new StringJoiner(",");
				transgenicAlleleConstruct.getExpressedGenes().forEach(expressedGeneSymbol -> expGeneJoiner.add(expressedGeneSymbol.getGeneSymbol().getFormatText()));
				transgenicAlleleConstruct.getExpressedGenes().stream().filter(gene -> gene.getPrimaryExternalId() != null)
					.forEach(expressedGeneID -> expGeneIDJoiner.add(expressedGeneID.getPrimaryExternalId()));
				expressedGene = expGeneJoiner.toString();
				expGeneID = expGeneIDJoiner.toString();
			}
			if (CollectionUtils.isNotEmpty(transgenicAlleleConstruct.getTargetedGenes())) {
				StringJoiner tgtGeneJoiner = new StringJoiner(",");
				StringJoiner tgtGeneIDJoiner = new StringJoiner(",");
				transgenicAlleleConstruct.getTargetedGenes().forEach(targetGeneSymbol -> tgtGeneJoiner.add(targetGeneSymbol.getGeneSymbol().getFormatText()));
				transgenicAlleleConstruct.getTargetedGenes().stream().filter(gene -> gene.getPrimaryExternalId() != null)
					.forEach(targetGeneID -> tgtGeneIDJoiner.add(targetGeneID.getPrimaryExternalId()));
				targetGene = tgtGeneJoiner.toString();
				tgtGeneID = tgtGeneIDJoiner.toString();
			}
			if (CollectionUtils.isNotEmpty(transgenicAlleleConstruct.getRegulatoryGenes())) {
				StringJoiner regGeneJoiner = new StringJoiner(",");
				StringJoiner regGeneIDJoiner = new StringJoiner(",");
				transgenicAlleleConstruct.getRegulatoryGenes().forEach(regGeneSymbol -> regGeneJoiner.add(regGeneSymbol.getGeneSymbol().getFormatText()));
				transgenicAlleleConstruct.getRegulatoryGenes().stream().filter(gene -> gene.getPrimaryExternalId() != null)
					.forEach(regulatoryGeneID -> regGeneIDJoiner.add(regulatoryGeneID.getPrimaryExternalId()));
				regGene = regGeneJoiner.toString();
				regGeneID = regGeneIDJoiner.toString();
			}
			row.setExpGeneID(expGeneID);
			row.setExpressedGene(expressedGene);
			row.setTargetID(tgtGeneID);
			row.setKnockdownTarget(targetGene);
			row.setRegulatoryRegionID(regGeneID);
			row.setRegulatoryRegion(regGene);
		}
		row.setHasPhenotype(transgenicAlleleSummaryDocument.getHasPhenotypeAnnotations().toString());
		row.setHasDisease(transgenicAlleleSummaryDocument.getHasDiseaseAnnotations().toString());

		return row;
	}


	public String getAllVariantsRows(List<VariantSummaryDocument> variants) {

		List<VariantDownloadRow> list = getVariantDownloadRowsForAlleles(variants);
		List<DownloadHeader> headers = List.of(
			new DownloadHeader<>("Symbol", VariantDownloadRow::getSymbol),
			new DownloadHeader<>("Variant Type", VariantDownloadRow::getVariantType),
			new DownloadHeader<>("Overlaps", VariantDownloadRow::getOverlaps),
			new DownloadHeader<>("Chromosome:Position", VariantDownloadRow::getChrPosition),
			new DownloadHeader<>("Nucleotide Change", VariantDownloadRow::getChange),
			new DownloadHeader<>("Most Severe Consequence", VariantDownloadRow::getConsequence),
			new DownloadHeader<>("HGVS.gName", VariantDownloadRow::getHgvsG),
			new DownloadHeader<>("HGVS.cName", VariantDownloadRow::getHgvsC),
			new DownloadHeader<>("HGVS.pName", VariantDownloadRow::getHgvsP),
			new DownloadHeader<>("Synonyms", VariantDownloadRow::getVariantSynonyms),
			new DownloadHeader<>("Notes", VariantDownloadRow::getNotes),
			new DownloadHeader<>("Cross References", VariantDownloadRow::getCrossReference),
			new DownloadHeader<>("References", VariantDownloadRow::getReference)
		);

		return DownloadHeader.getDownloadOutput(list, headers);
	}


	public List<VariantDownloadRow> getVariantDownloadRowsForAlleles(List<VariantSummaryDocument> annotations) {

		return annotations.stream()
			.map(this::getBaseDownloadVariantRow)
			.collect(Collectors.toList());
	}


	private VariantDownloadRow getBaseDownloadVariantRow(VariantSummaryDocument annotation) {
		VariantDownloadRow row = new VariantDownloadRow();
		CuratedVariantGenomicLocationAssociation variant = annotation.getVariants().get(0).getCuratedVariantGenomicLocations().get(0);
		if (variant == null) {
			return row;
		}
		row.setSymbol(variant.getHgvs());
		if (variant.getVariantAssociationSubject() != null && variant.getVariantAssociationSubject().getVariantType() != null) {
			row.setVariantType(variant.getVariantAssociationSubject().getVariantType().getName());
		}
		if (variant.getVariantGenomicLocationAssociationObject() != null) {
			String chrPos = variant.getVariantGenomicLocationAssociationObject().getName() + ":" + variant.getStart();
			if (!Objects.equals(variant.getStart(), variant.getEnd())) {
				chrPos += "-" + variant.getEnd();
			}
			row.setChrPosition(chrPos);
		}
		if (CollectionUtils.isNotEmpty(variant.getPredictedVariantConsequences())
			&& CollectionUtils.isNotEmpty(variant.getPredictedVariantConsequences().get(0).getVepConsequences())) {
			row.setConsequence(variant.getPredictedVariantConsequences().get(0).getVepConsequences().get(0).getName());
		}
		if (variant.getNucleotideChange() != null) {
			row.setChange(variant.getNucleotideChange());
		}
		if (variant.getPredictedVariantConsequences() != null) {
			String overlaps = variant.getPredictedVariantConsequences().stream()
				.filter(pvc -> pvc.getVariantTranscript() != null && pvc.getVariantTranscript().getTranscriptGeneAssociations() != null)
				.flatMap(pvc -> pvc.getVariantTranscript().getTranscriptGeneAssociations().stream())
				.map(tga -> tga.getTranscriptGeneAssociationObject())
				.filter(gene -> gene.getGeneSymbol() != null)
				.map(gene -> gene.getGeneSymbol().getDisplayText())
				.filter(Objects::nonNull)
				.distinct()
				.sorted()
				.collect(Collectors.joining(","));
			if (!overlaps.isEmpty()) {
				row.setOverlaps(overlaps);
			}
		}
		String hgvsGs = "";
		String hgvsPs = "";
		String hgvsCs = "";
		String synonyms = "";
		String crossRefs = "";
		String notesDescs = "";
		String pubs = "";

/*
		if (CollectionUtils.isNotEmpty(annotation.getSynonyms())) {
			StringJoiner synonymJoiner = new StringJoiner(",");
			annotation.getSynonyms().forEach(synonym -> synonymJoiner.add(synonym.getName()));
			synonyms = synonymJoiner.toString();
		}
		if (CollectionUtils.isNotEmpty(annotation.getHgvsG())) {
			StringJoiner hgvsgJoiner = new StringJoiner(",");
			annotation.getHgvsG().forEach(hgvsgJoiner::add);
			hgvsGs = hgvsgJoiner.toString();
		}
*/
		hgvsGs = variant.getHgvs();

		if (CollectionUtils.isNotEmpty(variant.getHgvsC())) {
			hgvsCs = getCommaDelimetedString(variant.getHgvsC());
		}
		if (CollectionUtils.isNotEmpty(variant.getHgvsP())) {
			hgvsPs = getCommaDelimetedString(variant.getHgvsP());
		}
		List<CrossReference> crossReferences = variant.getVariantAssociationSubject().getCrossReferences();
		if (CollectionUtils.isNotEmpty(crossReferences)) {
			crossRefs = getCommaDelimetedString(crossReferences.stream().map(CrossReference::getDisplayName).filter(Objects::nonNull).toList());
		}
		List<Note> relatedNotes = variant.getVariantAssociationSubject().getRelatedNotes();
		if (CollectionUtils.isNotEmpty(relatedNotes)) {
			notesDescs = getCommaDelimetedString(relatedNotes.stream().map(Note::getFreeText).toList());
		}
		List<Reference> references = variant.getVariantAssociationSubject().getReferences();
		if (CollectionUtils.isNotEmpty(references)) {
			pubs = getCommaDelimetedString(references.stream().map(Reference::getReferenceID).toList());
		}
		if (pubs.isEmpty() && CollectionUtils.isNotEmpty(variant.getEvidence())) {
			pubs = getCommaDelimetedString(variant.getEvidence().stream()
				.filter(Reference.class::isInstance)
				.map(Reference.class::cast)
				.map(Reference::getReferenceID)
				.filter(Objects::nonNull)
				.toList());
		}

		row.setVariantSynonyms(synonyms);
		row.setHgvsG(hgvsGs);
		row.setHgvsC(hgvsCs);
		row.setHgvsP(hgvsPs);
		row.setCrossReference(crossRefs);
		row.setNotes(notesDescs);
		row.setReference(pubs);

		return row;
	}

	private static String getCommaDelimetedString(List<String> names) {
		String hgvsCs;
		StringJoiner hgvscJoiner = new StringJoiner(",");
		names.forEach(hgvscJoiner::add);
		hgvsCs = hgvscJoiner.toString();
		return hgvsCs;
	}

	public String getAllAlleleVariantDetailRows(List<AlleleVariantSequence> annotations) {

		List<AlleleVariantSequenceDownloadRow> list = alleleVariantSequenceDownloadRow(annotations);
		List<DownloadHeader> headers = List.of(
			new DownloadHeader<>("Allele ID", AlleleVariantSequenceDownloadRow::getAlleleID),
			new DownloadHeader<>("Allele Symbol", AlleleVariantSequenceDownloadRow::getAlleleSymbol),
			new DownloadHeader<>("Allele Synonyms", AlleleVariantSequenceDownloadRow::getAlleleSynonyms),
			new DownloadHeader<>("Category", AlleleVariantSequenceDownloadRow::getVariantCategory),
			new DownloadHeader<>("Has Phenotype", AlleleVariantSequenceDownloadRow::getHasPhenotype),
			new DownloadHeader<>("Has Disease", AlleleVariantSequenceDownloadRow::getHasDisease),
			new DownloadHeader<>("Variant HGVS.g Name", AlleleVariantSequenceDownloadRow::getHgvsgName),
			new DownloadHeader<>("Variant Type", AlleleVariantSequenceDownloadRow::getVariantType),
			new DownloadHeader<>("Sequence Feature ", AlleleVariantSequenceDownloadRow::getSequenceFeature),
			new DownloadHeader<>("Sequence Feature Type", AlleleVariantSequenceDownloadRow::getSequenceFeatureType),
			new DownloadHeader<>("Sequence Feature associated Gene", AlleleVariantSequenceDownloadRow::getSequenceFeatureAssociatedGene),
			new DownloadHeader<>("Sequence Feature associated Gene ID", AlleleVariantSequenceDownloadRow::getSequenceFeatureAssociatedGeneID),
			new DownloadHeader<>("Molecular Consequences", AlleleVariantSequenceDownloadRow::getMolecularConsequences),
			new DownloadHeader<>("Variant Location", AlleleVariantSequenceDownloadRow::getLocation),
			new DownloadHeader<>("VEP Impact", AlleleVariantSequenceDownloadRow::getVepImpact),
			new DownloadHeader<>("Sift Prediction", AlleleVariantSequenceDownloadRow::getSiftPrediction),
			new DownloadHeader<>("Sift Score", AlleleVariantSequenceDownloadRow::getSiftScore),
			new DownloadHeader<>("PolyPhen Prediction", AlleleVariantSequenceDownloadRow::getPolyphenPrediction),
			new DownloadHeader<>("PolyPhen Score", AlleleVariantSequenceDownloadRow::getPolyphenScore)
		);

		return DownloadHeader.getDownloadOutput(list, headers);
	}


}



