package org.alliancegenome.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;

import org.alliancegenome.api.dto.RibbonSection;
import org.alliancegenome.api.dto.RibbonSummary;
import org.alliancegenome.api.entity.SectionSlim;
import org.alliancegenome.cache.repository.ExpressionCacheRepository;
import org.alliancegenome.neo4j.entity.node.GOTerm;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequestScoped
public class ExpressionRibbonService {

	public static final String UNDEFINED = "undefined";
	private static DiseaseRepository diseaseRepository = new DiseaseRepository();
	private static GeneRepository geneRepository = new GeneRepository();

	private static RibbonSummary ribbonSummary;

	public RibbonSummary getRibbonSectionInfo() {
		// get a deep clone of a template object
		// by serialization and deserialization (JSON)
		ObjectMapper objectMapper = new ObjectMapper();
		RibbonSummary deepCopy = null;
		try {
			deepCopy = objectMapper.readValue(objectMapper.writeValueAsString(getRibbonSections()), RibbonSummary.class);
		} catch (IOException e) {
			e.printStackTrace();
			log.error("Error: " + e);
		}

		return deepCopy;
	}

	public static Map<String, List<String>> slimParentTermIdMap = new LinkedHashMap<>();

	public static final String LOC_ALL = "LOC:ALL";

	public static final String STAGE_ALL = "STAGE:ALL";

	static {
		List<String> infection = new ArrayList<>();
		infection.add("Expression grouped by Locations");
		infection.add("All anatomical structures");
		slimParentTermIdMap.put(ExpressionCacheRepository.UBERON_ANATOMY_ROOT, infection);

		List<String> anatomy = new ArrayList<>();
		anatomy.add("Expression grouped by Stages");
		anatomy.add("All stages");
		slimParentTermIdMap.put(ExpressionCacheRepository.UBERON_STAGE_ROOT, anatomy);

		List<String> goTerms = new ArrayList<>();
		goTerms.add("Expression grouped by GO CC terms");
		goTerms.add("All cellular components");
		slimParentTermIdMap.put(ExpressionCacheRepository.GO_CC_ROOT, goTerms);

	}

	public RibbonSummary getRibbonSections() {
		if (ribbonSummary != null) {
			return ribbonSummary;
		}

		ribbonSummary = new RibbonSummary();

		slimParentTermIdMap.forEach((id, names) -> {
			RibbonSection section = new RibbonSection();
			section.setLabel(names.get(0));
			section.setId(id);


			String definition = "";
			if (StringUtils.isNotEmpty(id))
				definition = diseaseRepository.getTermDefinition(id);
			section.setDescription(definition);
			SectionSlim allSlimElement = new SectionSlim();
			allSlimElement.setId(id);
			allSlimElement.setLabel(names.get(1));
			allSlimElement.setTypeAll();
			allSlimElement.setDescription(definition);
			section.addDiseaseSlim(allSlimElement);
			ribbonSummary.addRibbonSection(section);
			List<GOTerm> goSlimList = geneRepository.getFullGoTermList();
			if (id.equals(ExpressionCacheRepository.GO_CC_ROOT)) {
				goSlimList.forEach(term -> {
					SectionSlim slim = getSectionSlim(term.getPrimaryKey(), term.getName(), term.getDefinition());
					section.addDiseaseSlim(slim);
				});
			}
			if (id.equals(ExpressionCacheRepository.UBERON_ANATOMY_ROOT)) {
				geneRepository.getFullAoTermList().forEach(term -> {
					SectionSlim slim = getSectionSlim(term.getPrimaryKey(), term.getName(), term.getDefinition());
					section.addDiseaseSlim(slim);
				});
			}
			if (id.equals(ExpressionCacheRepository.UBERON_STAGE_ROOT)) {
				geneRepository.getStageTermList().forEach(term -> {
					SectionSlim slim = getSectionSlim(term.getPrimaryKey(), term.getName(), term.getDefinition());
					section.addDiseaseSlim(slim);
				});
			}
		});

		return ribbonSummary;
	}

	private SectionSlim getSectionSlim(String primaryKey, String name, String def) {
		SectionSlim slim = new SectionSlim();
		slim.setId(primaryKey);
		slim.setLabel(name);
		if(def == null)
			def = UNDEFINED;
		slim.setDescription(def);
		return slim;
	}

}

