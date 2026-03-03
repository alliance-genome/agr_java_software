package org.alliancegenome.indexer;

import java.util.List;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.variant.converters.AlleleSequenceSummaryConverter;
import org.alliancegenome.curation_api.interfaces.document.AlleleDocumentInterface;
import org.alliancegenome.curation_api.model.document.es.AlleleSummaryDocument;
import org.alliancegenome.curation_api.model.document.es.SequenceSummaryDocument;
import org.alliancegenome.curation_api.model.entities.PredictedVariantConsequence;
import org.alliancegenome.curation_api.model.entities.Transcript;
import org.alliancegenome.curation_api.model.entities.Variant;
import org.alliancegenome.curation_api.model.entities.associations.CuratedVariantGenomicLocationAssociation;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.curation_api.view.CurationView;
import org.alliancegenome.es.rest.RestConfig;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import si.mazi.rescu.RestProxyFactory;

public class TestSynonyms {

	public static void main(String[] args) throws Exception {
		long alleleId = args.length > 0 ? Long.parseLong(args[0]) : 593L;

		AlleleDocumentInterface alleleApi = RestProxyFactory.createProxy(AlleleDocumentInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
		AlleleSequenceSummaryConverter sequenceSummaryConverter = new AlleleSequenceSummaryConverter();

		System.out.println("=== API URL: " + ConfigHelper.getCurationApiUrl() + " ===");
		System.out.println("=== Allele ID: " + alleleId + " ===\n");

		SearchResponse<AlleleSummaryDocument> response = alleleApi.findSummaryByIds(List.of(alleleId));

		System.out.println("=== Step 1: AlleleSummaryDocument from API ===");
		for (AlleleSummaryDocument doc : response.getResults()) {
			System.out.println("Allele symbol: " + (doc.getAllele() != null && doc.getAllele().getAlleleSymbol() != null ? doc.getAllele().getAlleleSymbol().getDisplayText() : "null"));
			System.out.println("Variants: " + (doc.getVariants() != null ? doc.getVariants().size() : "null"));
			if (doc.getVariants() != null) {
				for (Variant v : doc.getVariants()) {
					System.out.println("  Variant type: " + (v.getVariantType() != null ? v.getVariantType().getName() : "null"));
					if (v.getCuratedVariantGenomicLocations() != null) {
						for (CuratedVariantGenomicLocationAssociation loc : v.getCuratedVariantGenomicLocations()) {
							System.out.println("  HGVS: " + loc.getHgvs());
							System.out.println("  Consequences: " + (loc.getPredictedVariantConsequences() != null ? loc.getPredictedVariantConsequences().size() : "null"));
							if (loc.getPredictedVariantConsequences() != null) {
								for (int i = 0; i < Math.min(5, loc.getPredictedVariantConsequences().size()); i++) {
									PredictedVariantConsequence pvc = loc.getPredictedVariantConsequences().get(i);
									Transcript t = pvc.getVariantTranscript();
									System.out.println("	[" + i + "] transcript curie: " + (t != null ? t.getCurie() : "null"));
									System.out.println("		transcript name: " + (t != null ? t.getName() : "null"));
									System.out.println("		transcript primaryExternalId: " + (t != null ? t.getPrimaryExternalId() : "null"));
									System.out.println("		transcriptType: " + (t != null && t.getTranscriptType() != null ? t.getTranscriptType().getName() : "null"));
									System.out.println("		vepImpact: " + (pvc.getVepImpact() != null ? pvc.getVepImpact().getName() : "null"));
									System.out.println("		introns: " + pvc.getIntrons());
									System.out.println("		exons: " + pvc.getExons());
									System.out.println("		intronExonLocation: " + pvc.getIntronExonLocation());
									System.out.println("		geneLevelConsequence: " + pvc.getGeneLevelConsequence());
								}
								if (loc.getPredictedVariantConsequences().size() > 5) {
									System.out.println("	... and " + (loc.getPredictedVariantConsequences().size() - 5) + " more");
								}
							}
						}
					}
				}
			}
		}

		System.out.println("\n=== Step 2: Convert to SequenceSummaryDocuments ===");
		List<SequenceSummaryDocument> ssds = sequenceSummaryConverter.convert(response.getResults());
		System.out.println("SSD count: " + ssds.size());

		System.out.println("\n=== Step 3: Serialize first SSD with SequenceSummaryDocument view ===");
		ObjectMapper mapper = RestConfig.config.getJacksonObjectMapperFactory().createObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		ObjectMapper viewMapper = mapper.copy();
		viewMapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);

		if (!ssds.isEmpty()) {
			String json = viewMapper.writerWithView(CurationView.SequenceSummaryDocument.class).writeValueAsString(ssds.get(0));
			System.out.println(json);
			if (ssds.size() > 1) {
				System.out.println("\n(" + (ssds.size() - 1) + " more SSDs not shown)");
			}
		}
	}

}
