package org.alliancegenome.api.service;

import static java.util.stream.Collectors.toList;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.alliancegenome.cache.repository.helper.AlleleFiltering;
import org.alliancegenome.cache.repository.helper.AlleleSorting;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.core.api.service.FilterService;
import org.alliancegenome.es.index.site.dao.VariantESDAO;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.SOTerm;
import org.alliancegenome.neo4j.entity.node.Transcript;
import org.alliancegenome.neo4j.entity.node.Variant;
import org.alliancegenome.neo4j.repository.VariantRepository;
import org.apache.commons.lang3.StringUtils;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class VariantService {

	private static VariantRepository variantRepo = new VariantRepository();
	private static VariantESDAO variantDAO = new VariantESDAO();

	public JsonResultResponse<Transcript> getTranscriptsByVariant(String variantID, Pagination pagination) {
		Variant variant = getVariantById(variantID);

		JsonResultResponse<Transcript> response = new JsonResultResponse<>();
		if (variant == null || variant.getTranscriptLevelConsequence()==null)
			return response;

		List<Transcript> transcriptList = variant.getTranscriptLevelConsequence().stream()
				.map(consequence -> {
					Transcript transcript = new Transcript();
					transcript.setPrimaryKey(consequence.getTranscript().getPrimaryKey());
					// don't get an independent name from the VCF
					if (StringUtils.isNotEmpty(consequence.getTranscript().getName())) {
						transcript.setName(consequence.getTranscript().getName());
					} else {
						transcript.setName(consequence.getTranscript().getPrimaryKey());
					}
					transcript.setConsequences(List.of(consequence));
					SOTerm tType = new SOTerm();
					tType.setName(consequence.getSequenceFeatureType());
					transcript.setType(tType);
					transcript.setGene(consequence.getAssociatedGene());
					transcript.setIntronExonLocation(consequence.getLocation());
					return transcript;
				})
				.collect(Collectors.toList());
		response.setTotal(transcriptList.size());

		// populate location
///		   transcriptList.forEach(transcript -> VariantServiceHelper.populateIntronExonLocation(variant, transcript));

		// sorting
///		   Comparator<Transcript> comparatorGene = Comparator.comparing(transcript -> transcript.getGene().getSymbol());
///		   Comparator<Transcript> comparatorGeneSequence = comparatorGene.thenComparing(Transcript::getName);
///		   transcriptList.sort(comparatorGeneSequence);

		// pagination
		response.setResults(transcriptList.stream()
				.skip(pagination.getStart())
				.limit(pagination.getLimit())
				.collect(toList()));
		return response;
	}

	public JsonResultResponse<Variant> getVariants(String id, Pagination pagination) {
		LocalDateTime startDate = LocalDateTime.now();

		List<Variant> variants = variantRepo.getVariantsOfAllele(id);

		JsonResultResponse<Variant> result = new JsonResultResponse<>();

		FilterService<Variant> filterService = new FilterService<>(null);
		result.setTotal(variants.size());
		result.setResults(filterService.getPaginatedAnnotations(pagination, variants));
		result.calculateRequestDuration(startDate);
		return result;
	}

	public JsonResultResponse<Allele> getAllelesByVariant(String variantID, Pagination pagination) {
		Variant variant = variantRepo.getVariant(variantID);

		JsonResultResponse<Allele> response = new JsonResultResponse<>();
		if (variant == null)
			return response;

		List<Allele> alleles = variantRepo.getAllelesOfVariant(variantID);
		response.setTotal(alleles.size());

		// sorting
		FilterService<Allele> service = new FilterService<>(new AlleleFiltering());
		response.setResults(service.getSortedAndPaginatedAnnotations(pagination, alleles, new AlleleSorting()));
		return response;
	}

	public Variant getVariantById(String id) {
		Variant variant = variantRepo.getVariant(id);
		// if not found in Neo then try in ES
		if (variant == null) {
			variant = variantDAO.getVariant(id);
		}
		if (variant != null && variant.getSymbol() == null) {
			variant.setSymbol(variant.getPrimaryKey());
		}
		return variant;
	}
}
