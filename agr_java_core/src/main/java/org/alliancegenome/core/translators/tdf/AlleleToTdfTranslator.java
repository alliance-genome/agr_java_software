package org.alliancegenome.core.translators.tdf;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.Construct;
import org.alliancegenome.neo4j.entity.node.Publication;
import org.alliancegenome.neo4j.entity.node.TranscriptLevelConsequence;
import org.alliancegenome.neo4j.entity.node.Variant;
import org.apache.commons.collections.CollectionUtils;

public class AlleleToTdfTranslator {

	public String getAllRows(List<Allele> annotations) {

		List<AlleleDownloadRow> list = getAlleleDownloadRowsForGenes(annotations);
		List<DownloadHeader> headers = List.of(
				new DownloadHeader<>("Allele ID", (AlleleDownloadRow::getAlleleID)),
				new DownloadHeader<>("Allele Symbol", (AlleleDownloadRow::getAlleleSymbol)),
				new DownloadHeader<>("Allele Synonyms", (AlleleDownloadRow::getAlleleSynonyms)),
				new DownloadHeader<>("Category", (AlleleDownloadRow::getVariantCategory)),
				new DownloadHeader<>("Variant Symbol", (AlleleDownloadRow::getVariantSymbol)),
				new DownloadHeader<>("Variant Type", (AlleleDownloadRow::getVariantType)),
				new DownloadHeader<>("Variant Consequence", (AlleleDownloadRow::getVariantConsequence)),
				new DownloadHeader<>("Has Phenotype", (AlleleDownloadRow::getHasPhenotype)),
				new DownloadHeader<>("Has Disease", (AlleleDownloadRow::getHasDisease)),
				new DownloadHeader<>("Variant Information Reference", (AlleleDownloadRow::getReference))
		);

		return DownloadHeader.getDownloadOutput(list, headers);
	}

	public List<AlleleDownloadRow> getAlleleDownloadRowsForGenes(List<Allele> annotations) {

		return annotations.stream()
				.map(annotation -> {
					if (CollectionUtils.isNotEmpty(annotation.getVariants()))
						return annotation.getVariants().stream()
								.map(join -> {
									if (CollectionUtils.isNotEmpty(join.getPublications()))
										return join.getPublications().stream()
												.map(pub -> getBaseDownloadRow(annotation, join, pub))
												.collect(Collectors.toList());

									else
										return annotation.getVariants().stream()
												.map(var -> getBaseDownloadRow(annotation, var, null))
												.collect(Collectors.toList());

								}).flatMap(Collection::stream)
								.collect(Collectors.toList());


					else
						return List.of(getBaseDownloadRow(annotation, null, null));
				})
				.flatMap(Collection::stream)
				.collect(Collectors.toList());
	}

	public List<AlleleVariantSequenceDownloadRow> AlleleVariantSequenceDownloadRow(List<AlleleVariantSequence> annotations) {
		return annotations.stream()
				.map(this::getBaseAlleleVariantDownloadRow)
				.collect(Collectors.toList());
	}


	private AlleleDownloadRow getBaseDownloadRow(Allele annotation, Variant join, Publication pub) {
		AlleleDownloadRow row = new AlleleDownloadRow();
		row.setAlleleID(annotation.getPrimaryKey());
		row.setAlleleSymbol(annotation.getSymbol());
		String synonyms = "";
		if (CollectionUtils.isNotEmpty(annotation.getSynonyms())) {
			StringJoiner synonymJoiner = new StringJoiner(",");
			annotation.getSynonyms().forEach(synonym -> synonymJoiner.add(synonym.getName()));
			synonyms = synonymJoiner.toString();
		}
		row.setAlleleSynonyms(synonyms);
		row.setVariantCategory(annotation.getCategory());
		if (join != null) {
			row.setVariantSymbol(join.getHgvsNomenclature());
			row.setVariantType(join.getVariantType().getName());
			String consequence = "";
			if(CollectionUtils.isNotEmpty(join.getTranscriptLevelConsequence())) {
				consequence = join.getTranscriptLevelConsequence().stream()
					.filter(Objects::nonNull)
					.map(TranscriptLevelConsequence::getMolecularConsequences)
					.filter(Objects::nonNull)
					.flatMap(List::stream).distinct()
					.collect(Collectors.joining("|"));
			}
			row.setVariantConsequence (consequence);
		}
		row.setHasPhenotype(annotation.hasPhenotype().toString());
		row.setHasDisease(annotation.hasDisease().toString());
		if (pub != null) {
			row.setReference(pub.getPubId());
		}
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

	public String getAllTransgenicAlleleRows(List<Allele> annotations) {

		List<TransgenicAlleleDownloadRow> list = getTransgenicAlleleDownloadRowsForGenes(annotations);
		List<DownloadHeader> headers = List.of(
				new DownloadHeader<>("Species", (TransgenicAlleleDownloadRow::getSpecies)),
				new DownloadHeader<>("Allele ID", (TransgenicAlleleDownloadRow::getAlleleID)),
				new DownloadHeader<>("Allele Symbol", (TransgenicAlleleDownloadRow::getAlleleSymbol)),
				new DownloadHeader<>("Transgenic Construct ID", (TransgenicAlleleDownloadRow::getTgConstructID)),
				new DownloadHeader<>("Transgenic Construct", (TransgenicAlleleDownloadRow::getTransgenicConstruct)),
				new DownloadHeader<>("Expressed Gene ID", (TransgenicAlleleDownloadRow::getExpGeneID)),
				new DownloadHeader<>("Expressed Gene", (TransgenicAlleleDownloadRow::getExpressedGene)),
				new DownloadHeader<>("Knockdown Target ID", (TransgenicAlleleDownloadRow::getTargetID)),
				new DownloadHeader<>("Knockdown Target", (TransgenicAlleleDownloadRow::getKnockdownTarget)),
				new DownloadHeader<>("Regulatory Region ID", (TransgenicAlleleDownloadRow::getRegulatoryRegionID)),
				new DownloadHeader<>("Regulatory Region", (TransgenicAlleleDownloadRow::getRegulatoryRegion)),
				new DownloadHeader<>("Has Phenotype", (TransgenicAlleleDownloadRow::getHasPhenotype)),
				new DownloadHeader<>("Has Disease", (TransgenicAlleleDownloadRow::getHasDisease))
		);

		return DownloadHeader.getDownloadOutput(list, headers);
	}

	public List<TransgenicAlleleDownloadRow> getTransgenicAlleleDownloadRowsForGenes(List<Allele> annotations) {

		return annotations.stream()
				.map(annotation -> {
					if (CollectionUtils.isNotEmpty(annotation.getConstructs()))
						return annotation.getConstructs().stream()
								.map(join -> {
									return annotation.getConstructs().stream()
											.map(var -> getBaseDownloadAlleleTransgenicRow(annotation, var, null))
											.collect(Collectors.toList());

								}).flatMap(Collection::stream)
								.collect(Collectors.toList());


					else
						return List.of(getBaseDownloadAlleleTransgenicRow(annotation, null, null));
				})
				.flatMap(Collection::stream)
				.collect(Collectors.toList());


	}

	private TransgenicAlleleDownloadRow getBaseDownloadAlleleTransgenicRow(Allele annotation, Construct join, Publication pub) {
		TransgenicAlleleDownloadRow row = new TransgenicAlleleDownloadRow();
		row.setSpecies(annotation.getSpecies().getName());
		row.setAlleleID(annotation.getPrimaryKey());
		row.setAlleleSymbol(annotation.getSymbol());
		row.setTgConstructID(join.getPrimaryKey());
		row.setTransgenicConstruct(join.getName());
		String expressedGene = "";
		String expGeneID = "";
		String targetGene = "";
		String tgtGeneID = "";
		String regGene = "";
		String regGeneID = "";
		if (join != null) {
			if (CollectionUtils.isNotEmpty(join.getExpressedGenes())) {
				StringJoiner expGeneJoiner = new StringJoiner(",");
				StringJoiner expGeneIDJoiner = new StringJoiner(",");
				join.getExpressedGenes().forEach(expressedGeneSymbol -> expGeneJoiner.add(expressedGeneSymbol.getSymbol()));
				join.getExpressedGenes().forEach(expressedGeneID -> expGeneIDJoiner.add(expressedGeneID.getPrimaryKey()));
				expressedGene = expGeneJoiner.toString();
				expGeneID = expGeneIDJoiner.toString();
			}
			if (CollectionUtils.isNotEmpty(join.getTargetGenes())) {
				StringJoiner tgtGeneJoiner = new StringJoiner(",");
				StringJoiner tgtGeneIDJoiner = new StringJoiner(",");
				join.getTargetGenes().forEach(targetGeneSymbol -> tgtGeneJoiner.add(targetGeneSymbol.getSymbol()));
				join.getTargetGenes().forEach(targetGeneID -> tgtGeneIDJoiner.add(targetGeneID.getPrimaryKey()));
				targetGene = tgtGeneJoiner.toString();
				tgtGeneID = tgtGeneIDJoiner.toString();
			}
			if (CollectionUtils.isNotEmpty(join.getRegulatedByGenes())) {
				StringJoiner regGeneJoiner = new StringJoiner(",");
				StringJoiner regGeneIDJoiner = new StringJoiner(",");
				join.getRegulatedByGenes().forEach(regGeneSymbol -> regGeneJoiner.add(regGeneSymbol.getSymbol()));
				join.getRegulatedByGenes().forEach(regulatoryGeneID -> regGeneIDJoiner.add(regulatoryGeneID.getPrimaryKey()));
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
		row.setHasPhenotype(annotation.hasPhenotype().toString());
		row.setHasDisease(annotation.hasDisease().toString());

		return row;
	}


	public String getAllVariantsRows(List<Variant> variants) {

		List<VariantDownloadRow> list = getVariantDownloadRowsForAlleles(variants);
		List<DownloadHeader> headers = List.of(
				new DownloadHeader<>("Symbol", (VariantDownloadRow::getSymbol)),
				new DownloadHeader<>("Variant Type", (VariantDownloadRow::getVariantType)),
				new DownloadHeader<>("Overlaps", (VariantDownloadRow::getOverlaps)),
				new DownloadHeader<>("Chromosome:Position", (VariantDownloadRow::getChrPosition)),
				new DownloadHeader<>("Nucleotide Change", (VariantDownloadRow::getChange)),
				new DownloadHeader<>("Most Severe Consequence", (VariantDownloadRow::getConsequence)),
				new DownloadHeader<>("HGVS.gName", (VariantDownloadRow::getHgvsG)),
				new DownloadHeader<>("HGVS.cName", (VariantDownloadRow::getHgvsC)),
				new DownloadHeader<>("HGVS.pName", (VariantDownloadRow::getHgvsP)),
				new DownloadHeader<>("Synonyms", (VariantDownloadRow::getVariantSynonyms)),
				new DownloadHeader<>("Notes", (VariantDownloadRow::getNotes)),
				new DownloadHeader<>("Cross References", (VariantDownloadRow::getCrossReference)),
				new DownloadHeader<>("References", (VariantDownloadRow::getReference))
		);

		return DownloadHeader.getDownloadOutput(list, headers);
	}


	public List<VariantDownloadRow> getVariantDownloadRowsForAlleles(List<Variant> annotations) {

		return annotations.stream()
				.map(this::getBaseDownloadVariantRow)
				.collect(Collectors.toList());
	}
	

	private VariantDownloadRow getBaseDownloadVariantRow(final Variant annotation) {
		VariantDownloadRow row = new VariantDownloadRow();
		row.setSymbol(annotation.getHgvsNomenclature());
		row.setVariantType(annotation.getVariantType().getName());
		row.setChrPosition(annotation.getLocation().getChromosomeAndPosition());
		row.setChange(annotation.getNucleotideChange());
		row.setConsequence(annotation.getConsequence());
		row.setOverlaps(annotation.getGene().getSymbol());
		String hgvsGs = "";
		String hgvsPs = "";
		String hgvsCs = "";
		String synonyms = "";
		String crossRefs = "";
		String notesDescs = "";
		String pubs="";

		if (CollectionUtils.isNotEmpty(annotation.getSynonyms())) {
			StringJoiner synonymJoiner = new StringJoiner(",");
			annotation.getSynonyms().forEach(synonym -> synonymJoiner.add(synonym.getName()));
			synonyms = synonymJoiner.toString();
		}

		if (CollectionUtils.isNotEmpty(annotation.getHgvsG())) {
			StringJoiner hgvsgJoiner = new StringJoiner(",");
			annotation.getHgvsG().forEach(hgvsg -> hgvsgJoiner.add(hgvsg));
			hgvsGs = hgvsgJoiner.toString();
		}
		if (CollectionUtils.isNotEmpty(annotation.getHgvsC())) {
			StringJoiner hgvscJoiner = new StringJoiner(",");
			annotation.getHgvsC().forEach(hgvsc -> hgvscJoiner.add(hgvsc));
			hgvsCs = hgvscJoiner.toString();
		}
		if (CollectionUtils.isNotEmpty(annotation.getHgvsP())) {
			StringJoiner hgvspJoiner = new StringJoiner(",");
			annotation.getHgvsP().forEach(hgvsp -> hgvspJoiner.add(hgvsp));
			hgvsPs = hgvspJoiner.toString();
		}
		if (CollectionUtils.isNotEmpty(annotation.getCrossReferences())) {
			StringJoiner crossRefJoiner = new StringJoiner(",");
			annotation.getCrossReferences().forEach(crossRef -> crossRefJoiner.add(crossRef.getDisplayName()));
			crossRefs = crossRefJoiner.toString();
		}
		if (CollectionUtils.isNotEmpty(annotation.getNotes())) {
			StringJoiner noteJoiner = new StringJoiner(",");
			annotation.getNotes().forEach(noteDesc -> noteJoiner.add(noteDesc.getNote()));
			notesDescs = noteJoiner.toString();
		}
		if (CollectionUtils.isNotEmpty(annotation.getPublications())) {
			StringJoiner pubJoiner = new StringJoiner(",");
			annotation.getPublications().forEach(pub -> pubJoiner.add(pub.getPubId()));
			pubs = pubJoiner.toString();
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

	public String getAllAlleleVariantDetailRows(List<AlleleVariantSequence> annotations) {

		List<AlleleVariantSequenceDownloadRow> list = AlleleVariantSequenceDownloadRow(annotations);
		List<DownloadHeader> headers = List.of(
				new DownloadHeader<>("Allele ID", (AlleleVariantSequenceDownloadRow::getAlleleID)),
				new DownloadHeader<>("Allele Symbol", (AlleleVariantSequenceDownloadRow::getAlleleSymbol)),
				new DownloadHeader<>("Allele Synonyms", (AlleleVariantSequenceDownloadRow::getAlleleSynonyms)),
				new DownloadHeader<>("Category", (AlleleVariantSequenceDownloadRow::getVariantCategory)),
				new DownloadHeader<>("Has Phenotype", (AlleleVariantSequenceDownloadRow::getHasPhenotype)),
				new DownloadHeader<>("Has Disease", (AlleleVariantSequenceDownloadRow::getHasDisease)),
				new DownloadHeader<>("Variant HGVS.g Name", (AlleleVariantSequenceDownloadRow::getHgvsgName)),
				new DownloadHeader<>("Variant Type", (AlleleVariantSequenceDownloadRow::getVariantType)),
				new DownloadHeader<>("Sequence Feature ", (AlleleVariantSequenceDownloadRow::getSequenceFeature)),
				new DownloadHeader<>("Sequence Feature Type", (AlleleVariantSequenceDownloadRow::getSequenceFeatureType)),
				new DownloadHeader<>("Sequence Feature associated Gene", (AlleleVariantSequenceDownloadRow::getSequenceFeatureAssociatedGene)),
				new DownloadHeader<>("Sequence Feature associated Gene ID", (AlleleVariantSequenceDownloadRow::getSequenceFeatureAssociatedGeneID)),
				new DownloadHeader<>("Molecular Consequences", (AlleleVariantSequenceDownloadRow::getMolecularConsequences)),
				new DownloadHeader<>("Variant Location", (AlleleVariantSequenceDownloadRow::getLocation)),
				new DownloadHeader<>("VEP Impact", (AlleleVariantSequenceDownloadRow::getVepImpact)),
				new DownloadHeader<>("Sift Prediction", (AlleleVariantSequenceDownloadRow::getSiftPrediction)),
				new DownloadHeader<>("Sift Score", (AlleleVariantSequenceDownloadRow::getSiftScore)),
				new DownloadHeader<>("PolyPhen Prediction", (AlleleVariantSequenceDownloadRow::getPolyphenPrediction)),
				new DownloadHeader<>("PolyPhen Score", (AlleleVariantSequenceDownloadRow::getPolyphenScore))
		);

		return DownloadHeader.getDownloadOutput(list, headers);
	}


}




