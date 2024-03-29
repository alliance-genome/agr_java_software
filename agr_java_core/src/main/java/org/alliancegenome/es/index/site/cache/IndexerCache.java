package org.alliancegenome.es.index.site.cache;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.Variant;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class IndexerCache {

	protected Map<String, Variant> variantMap = new HashMap<>();
	protected Map<String, Allele> alleleMap = new HashMap<>();
	protected Map<String, Set<String>> assays = new HashMap<>();
	protected Map<String, Set<String>> chromosomes = new HashMap<>();
	protected Map<String, Set<String>> constructs = new HashMap<>();
	protected Map<String, Set<String>> crossReferences = new HashMap<>();
	protected Map<String, Set<String>> diseases = new HashMap<>();
	protected Map<String, Set<String>> diseasesAgrSlim = new HashMap<>();
	protected Map<String, Set<String>> diseasesWithParents = new HashMap<>();
	protected Map<String, Set<String>> expressionStages = new HashMap<>();
	protected Map<String, Set<String>> alleles = new HashMap<>();
	protected Map<String, Set<String>> genes = new HashMap<>();
	protected Map<String, Set<String>> geneIds = new HashMap<>();
	protected Map<String, Set<String>> geneSynonyms = new HashMap<>();
	protected Map<String, Set<String>> geneCrossReferences = new HashMap<>();
	protected Map<String, Set<String>> models = new HashMap<>();
	protected Map<String, Set<String>> molecularConsequenceMap = new HashMap<>();
	protected Map<String, Set<String>> phenotypeStatements = new HashMap<>();
	protected Map<String, Double> popularity = new HashMap<>();
	protected Map<String, Set<String>> sex = new HashMap<>();
	protected Map<String, Set<String>> sampleIds = new HashMap<>();
	protected Map<String, Set<String>> secondaryIds = new HashMap<>();
	protected Map<String, Set<String>> species = new HashMap<>();
	protected Map<String, Set<String>> synonyms = new HashMap<>();
//	  protected Map<String, Set<String>> stage = new HashMap<>();
	protected Map<String, Set<String>> tags = new HashMap<>();
	protected Map<String, Set<String>> variants = new HashMap<>();
	protected Map<String, Set<String>> variantSynonyms = new HashMap<>();
	protected Map<String, Set<String>> variantType = new HashMap<>();

	//expression fields
	private Map<String,Set<String>> whereExpressed = new HashMap<>();
	private Map<String,Set<String>> anatomicalExpression = new HashMap<>();			//uberon slim
	private Map<String,Set<String>> anatomicalExpressionWithParents = new HashMap<>();
	private Map<String,Set<String>> subcellularExpressionWithParents = new HashMap<>();
	private Map<String,Set<String>> subcellularExpressionAgrSlim = new HashMap<>();

	protected abstract <D extends SearchableItemDocument> void addExtraCachedFields(D document);
	
	public <D extends SearchableItemDocument> void addCachedFields(Iterable<D> documents) {
		for (D document : documents) {
			addCachedFields(document);
			addExtraCachedFields(document);
		}
	}

	protected <D extends SearchableItemDocument> void addCachedFields(D document) {
		String id = document.getPrimaryKey();

		document.setAlleles(alleles.get(id));
		document.setAssays(assays.get(id));
		//addAll vs setter is because some fields may be set by a translator before this step
		if (crossReferences.get(id) != null) {
			if(document.getCrossReferences() == null) {
				document.setCrossReferences(new HashSet<String>());
			}
			document.getCrossReferences().addAll(crossReferences.get(id));
		}
		if (chromosomes.get(id) != null) {
			if(document.getChromosomes() == null) {
				document.setChromosomes(new HashSet<String>());
			}
			document.getChromosomes().addAll(chromosomes.get(id));
		}
		document.setConstructs(constructs.get(id));
		document.setDiseases(diseases.get(id));
		document.setDiseasesAgrSlim(diseasesAgrSlim.get(id));
		document.setDiseasesWithParents(diseasesWithParents.get(id));

		if (variantType.get(id) == null) {
			Set<String> defaultValue = new HashSet<>();
			defaultValue.add("unreported");
			document.setVariantType(defaultValue);
		} else {
			document.setVariantType(variantType.get(id));
		}

		document.setExpressionStages(expressionStages.get(id));
		document.setGenes(genes.get(id));
		document.setGeneIds(geneIds.get(id));
		document.setGeneSynonyms(geneSynonyms.get(id));
		document.setGeneCrossReferences(geneCrossReferences.get(id));
		document.setModels(models.get(id));

		if (molecularConsequenceMap.get(id) != null) {
			document.setMolecularConsequence(new HashSet<>());
			for (String consequence : molecularConsequenceMap.get(id)) {
				if(document.getMolecularConsequence() == null) {
					document.setMolecularConsequence(new HashSet<String>());
				}
				document.getMolecularConsequence().addAll(Arrays.asList(consequence.split(",")));
			}
		}

		document.setPhenotypeStatements(phenotypeStatements.get(id));
		document.setPopularity(popularity.get(id) == null ? 0D : popularity.get(id));
		document.setSampleIds(sampleIds.get(id));
		document.setSex(sex.get(id));
//		  document.setStage(stage.get(id));
		document.setVariants(variants.get(id));
		document.setVariantSynonyms(variantSynonyms.get(id));
		if (secondaryIds.get(id) != null) {
			if(document.getSecondaryIds() == null) {
				document.setSecondaryIds(new HashSet<String>());
			}
			document.getSecondaryIds().addAll(secondaryIds.get(id));
		}

		//awkwardly collapsing to a single value, multi-valued species should
		//be captured in the associatedSpecies field
		if (species.get(id) != null) {
			Set<String> speciesSet = species.get(id);
			String speciesName = speciesSet.stream().findFirst().get();
			if (speciesName != null) {
				document.setSpecies(speciesName);
			}
		}

		if (synonyms.get(id) != null) {
			if(document.getSynonyms() == null) {
				document.setSynonyms(new HashSet<String>());
			}
			document.getSynonyms().addAll(synonyms.get(id));
		}

		document.setTags(tags.get(id));

		//populate expression fields
		document.setWhereExpressed(whereExpressed.get(id));
		document.setAnatomicalExpression(anatomicalExpression.get(id));
		document.setAnatomicalExpressionWithParents(anatomicalExpressionWithParents.get(id));
		document.setSubcellularExpressionWithParents(subcellularExpressionWithParents.get(id));
		document.setSubcellularExpressionAgrSlim(subcellularExpressionAgrSlim.get(id));

	}


}
