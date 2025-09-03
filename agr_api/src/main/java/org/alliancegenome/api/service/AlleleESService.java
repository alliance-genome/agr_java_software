package org.alliancegenome.api.service;

import jakarta.enterprise.context.RequestScoped;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.curation_api.model.document.es.TransgenicAlleleDocument;
import org.alliancegenome.es.model.query.Pagination;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;


@RequestScoped
public class AlleleESService extends ESService {

	public JsonResultResponse<TransgenicAlleleDocument> getTransgenicAlleles(String alleleId) {
		BoolQueryBuilder bool = boolQuery();
		BoolQueryBuilder bool2 = boolQuery();
		bool.must(bool2);
		// ToDo: Change this class such that the category is public
		// TransgenicAlleleDocument.category
		bool.filter(new TermQueryBuilder("category", "transgenic_allele_annotation"));
		bool2.should(new MatchQueryBuilder("allele.primaryExternalId.keyword", alleleId));

		JsonResultResponse<TransgenicAlleleDocument> ret = new JsonResultResponse<>();

		SearchResponse searchResponse = getSearchResponse(bool, new Pagination(), null, false);
		ret.setTotal((int) searchResponse.getHits().getTotalHits().value);
		List<TransgenicAlleleDocument> list = new ArrayList<>();
		String json = """
			{"results":[{"type":"TransgenicAlleleDocument","gene":{"type":"Gene","dateCreated":"2018-03-05T08:00:00Z","internal":false,"obsolete":false,"primaryExternalId":"ZFIN:ZDB-LINCRNAG-180305-1","taxon":{"internal":false,"obsolete":false,"curie":"NCBITaxon:7955","name":"Danio rerio"},"geneSymbol":{"internal":false,"obsolete":false,"formatText":"linc-tr","displayText":"linc-tr"},"geneFullName":{"internal":false,"obsolete":false,"formatText":"linc-tr","displayText":"linc-tr"}},"allele":{"type":"Allele","dateCreated":"2023-05-17T07:00:00Z","internal":false,"obsolete":false,"primaryExternalId":"ZFIN:ZDB-ALT-230517-8","taxon":{"internal":false,"obsolete":false,"curie":"NCBITaxon:7955","name":"Danio rerio"},"alleleSymbol":{"internal":false,"obsolete":false,"formatText":"zf3577Tg","displayText":"zf3577Tg"}},"transgenicAlleleConstructs":[{"construct":{"type":"Construct","dateCreated":"2023-05-17T07:00:00Z","internal":false,"obsolete":false,"primaryExternalId":"ZFIN:ZDB-TGCONSTRCT-230517-1","constructSymbol":{"internal":false,"obsolete":false,"formatText":"tg(drl:linc-tr,myl7:egfp)","displayText":"tg(drl:linc-tr,myl7:egfp)"}},"expressedGenes":[{"type":"Gene","dateCreated":"2018-03-05T08:00:00Z","internal":false,"obsolete":false,"primaryExternalId":"ZFIN:ZDB-LINCRNAG-180305-1","taxon":{"internal":false,"obsolete":false,"curie":"NCBITaxon:7955","name":"Danio rerio"},"geneSymbol":{"internal":false,"obsolete":false,"formatText":"linc-tr","displayText":"linc-tr"},"geneFullName":{"internal":false,"obsolete":false,"formatText":"linc-tr","displayText":"linc-tr"}}],"regulatoryGenes":[{"type":"Gene","dateCreated":"1999-10-19T07:00:00Z","internal":false,"obsolete":false,"primaryExternalId":"ZFIN:ZDB-GENE-991019-3","taxon":{"internal":false,"obsolete":false,"curie":"NCBITaxon:7955","name":"Danio rerio"},"geneSymbol":{"internal":false,"obsolete":false,"formatText":"myl7","displayText":"myl7"},"geneFullName":{"internal":false,"obsolete":false,"formatText":"myl7","displayText":"myl7"}},{"type":"Gene","dateCreated":"1999-12-13T08:00:00Z","internal":false,"obsolete":false,"primaryExternalId":"ZFIN:ZDB-GENE-991213-3","taxon":{"internal":false,"obsolete":false,"curie":"NCBITaxon:7955","name":"Danio rerio"},"geneSymbol":{"internal":false,"obsolete":false,"formatText":"drl","displayText":"drl"},"geneFullName":{"internal":false,"obsolete":false,"formatText":"drl","displayText":"drl"}}],"nonBgiComponents":[{"type":"Gene","internal":false,"obsolete":false,"geneSymbol":{"internal":false,"obsolete":false,"formatText":"EGFP","displayText":"EGFP"}}]}],"hasDiseaseAnnotations":false,"hasPhenotypeAnnotations":false,"regulatoryGenes":[{"type":"Gene","dateCreated":"1999-10-19T07:00:00Z","internal":false,"obsolete":false,"primaryExternalId":"ZFIN:ZDB-GENE-991019-3","taxon":{"internal":false,"obsolete":false,"curie":"NCBITaxon:7955","name":"Danio rerio"},"geneSymbol":{"internal":false,"obsolete":false,"formatText":"myl7","displayText":"myl7"},"geneFullName":{"internal":false,"obsolete":false,"formatText":"myl7","displayText":"myl7"}},{"type":"Gene","dateCreated":"1999-12-13T08:00:00Z","internal":false,"obsolete":false,"primaryExternalId":"ZFIN:ZDB-GENE-991213-3","taxon":{"internal":false,"obsolete":false,"curie":"NCBITaxon:7955","name":"Danio rerio"},"geneSymbol":{"internal":false,"obsolete":false,"formatText":"drl","displayText":"drl"},"geneFullName":{"internal":false,"obsolete":false,"formatText":"drl","displayText":"drl"}}],"expressedGenes":[{"type":"Gene","dateCreated":"2018-03-05T08:00:00Z","internal":false,"obsolete":false,"primaryExternalId":"ZFIN:ZDB-LINCRNAG-180305-1","taxon":{"internal":false,"obsolete":false,"curie":"NCBITaxon:7955","name":"Danio rerio"},"geneSymbol":{"internal":false,"obsolete":false,"formatText":"linc-tr","displayText":"linc-tr"},"geneFullName":{"internal":false,"obsolete":false,"formatText":"linc-tr","displayText":"linc-tr"}},{"type":"Gene","internal":false,"obsolete":false,"geneSymbol":{"internal":false,"obsolete":false,"formatText":"EGFP","displayText":"EGFP"}}],"constructList":[{"type":"Construct","dateCreated":"2023-05-17T07:00:00Z","internal":false,"obsolete":false,"primaryExternalId":"ZFIN:ZDB-TGCONSTRCT-230517-1","constructSymbol":{"internal":false,"obsolete":false,"formatText":"tg(drl:linc-tr,myl7:egfp)","displayText":"tg(drl:linc-tr,myl7:egfp)"}}]},{"type":"TransgenicAlleleDocument","gene":{"type":"Gene","dateCreated":"2002-05-07T07:00:00Z","internal":false,"obsolete":false,"primaryExternalId":"ZFIN:ZDB-GENE-020507-3","taxon":{"internal":false,"obsolete":false,"curie":"NCBITaxon:7955","name":"Danio rerio"},"geneSymbol":{"internal":false,"obsolete":false,"formatText":"vangl2","displayText":"vangl2"},"geneFullName":{"internal":false,"obsolete":false,"formatText":"vangl2","displayText":"vangl2"}},"allele":{"type":"Allele","dateCreated":"2014-08-14T17:18:37Z","internal":false,"obsolete":false,"primaryExternalId":"ZFIN:ZDB-ALT-140814-9","taxon":{"internal":false,"obsolete":false,"curie":"NCBITaxon:7955","name":"Danio rerio"},"alleleSymbol":{"internal":false,"obsolete":false,"formatText":"zou011Tg","displayText":"zou011Tg"}},"transgenicAlleleConstructs":[{"construct":{"type":"Construct","dateCreated":"2014-08-14T07:00:00Z","internal":false,"obsolete":false,"primaryExternalId":"ZFIN:ZDB-TGCONSTRCT-140814-10","constructSymbol":{"internal":false,"obsolete":false,"formatText":"Tg(UAS:MYC-vangl2-Rno.P2rx2,NLS-RFP,myl7:EGFP)","displayText":"Tg(UAS:MYC-vangl2-Rno.P2rx2,NLS-RFP,myl7:EGFP)"}},"expressedGenes":[{"type":"Gene","dateCreated":"2002-05-07T07:00:00Z","internal":false,"obsolete":false,"primaryExternalId":"ZFIN:ZDB-GENE-020507-3","taxon":{"internal":false,"obsolete":false,"curie":"NCBITaxon:7955","name":"Danio rerio"},"geneSymbol":{"internal":false,"obsolete":false,"formatText":"vangl2","displayText":"vangl2"},"geneFullName":{"internal":false,"obsolete":false,"formatText":"vangl2","displayText":"vangl2"}}],"regulatoryGenes":[{"type":"Gene","dateCreated":"1999-10-19T07:00:00Z","internal":false,"obsolete":false,"primaryExternalId":"ZFIN:ZDB-GENE-991019-3","taxon":{"internal":false,"obsolete":false,"curie":"NCBITaxon:7955","name":"Danio rerio"},"geneSymbol":{"internal":false,"obsolete":false,"formatText":"myl7","displayText":"myl7"},"geneFullName":{"internal":false,"obsolete":false,"formatText":"myl7","displayText":"myl7"}}],"nonBgiComponents":[{"type":"Gene","internal":false,"obsolete":false,"geneSymbol":{"internal":false,"obsolete":false,"formatText":"RFP","displayText":"RFP"}},{"type":"Gene","internal":false,"obsolete":false,"geneSymbol":{"internal":false,"obsolete":false,"formatText":"MYC","displayText":"MYC"}},{"type":"Gene","internal":false,"obsolete":false,"geneSymbol":{"internal":false,"obsolete":false,"formatText":"UAS","displayText":"UAS"}},{"type":"Gene","internal":false,"obsolete":false,"geneSymbol":{"internal":false,"obsolete":false,"formatText":"EGFP","displayText":"EGFP"}}]}],"hasDiseaseAnnotations":false,"hasPhenotypeAnnotations":false,"regulatoryGenes":[{"type":"Gene","dateCreated":"1999-10-19T07:00:00Z","internal":false,"obsolete":false,"primaryExternalId":"ZFIN:ZDB-GENE-991019-3","taxon":{"internal":false,"obsolete":false,"curie":"NCBITaxon:7955","name":"Danio rerio"},"geneSymbol":{"internal":false,"obsolete":false,"formatText":"myl7","displayText":"myl7"},"geneFullName":{"internal":false,"obsolete":false,"formatText":"myl7","displayText":"myl7"}}],"expressedGenes":[{"type":"Gene","dateCreated":"2002-05-07T07:00:00Z","internal":false,"obsolete":false,"primaryExternalId":"ZFIN:ZDB-GENE-020507-3","taxon":{"internal":false,"obsolete":false,"curie":"NCBITaxon:7955","name":"Danio rerio"},"geneSymbol":{"internal":false,"obsolete":false,"formatText":"vangl2","displayText":"vangl2"},"geneFullName":{"internal":false,"obsolete":false,"formatText":"vangl2","displayText":"vangl2"}},{"type":"Gene","internal":false,"obsolete":false,"geneSymbol":{"internal":false,"obsolete":false,"formatText":"RFP","displayText":"RFP"}},{"type":"Gene","internal":false,"obsolete":false,"geneSymbol":{"internal":false,"obsolete":false,"formatText":"MYC","displayText":"MYC"}},{"type":"Gene","internal":false,"obsolete":false,"geneSymbol":{"internal":false,"obsolete":false,"formatText":"UAS","displayText":"UAS"}},{"type":"Gene","internal":false,"obsolete":false,"geneSymbol":{"internal":false,"obsolete":false,"formatText":"EGFP","displayText":"EGFP"}}],"constructList":[{"type":"Construct","dateCreated":"2014-08-14T07:00:00Z","internal":false,"obsolete":false,"primaryExternalId":"ZFIN:ZDB-TGCONSTRCT-140814-10","constructSymbol":{"internal":false,"obsolete":false,"formatText":"Tg(UAS:MYC-vangl2-Rno.P2rx2,NLS-RFP,myl7:EGFP)","displayText":"Tg(UAS:MYC-vangl2-Rno.P2rx2,NLS-RFP,myl7:EGFP)"}}]}],"returnedRecords":2}
			""";


		Arrays.stream(searchResponse.getHits().getHits())
			.forEach(searchHit -> {
				try {
					TransgenicAlleleDocument object = mapper.readValue(searchHit.getSourceAsString(), TransgenicAlleleDocument.class);
					list.add(object);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		ret.setResults(list);
		return ret;
	}


}
