package org.alliancegenome.core.api.service;

import static org.alliancegenome.api.entity.DiseaseRibbonSummary.DOID_OTHER;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.alliancegenome.api.entity.DiseaseRibbonSection;
import org.alliancegenome.api.entity.DiseaseRibbonSummary;
import org.alliancegenome.api.entity.SectionSlim;
import org.alliancegenome.curation_api.model.document.es.DiseaseSummaryDocument;
import org.alliancegenome.es.index.site.dao.DiseaseESDAO;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.repository.DiseaseRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class DiseaseRibbonService {

	private final DiseaseRepository diseaseRepository;
	private final DiseaseESDAO diseaseESDAO;

	private static DiseaseRibbonSummary diseaseRibbonSummary;

	public DiseaseRibbonService(DiseaseRepository diseaseRepository, DiseaseESDAO diseaseESDAO) {
		this.diseaseRepository = diseaseRepository;
		this.diseaseESDAO = diseaseESDAO;
	}

	public DiseaseRibbonSummary getDiseaseRibbonSectionInfo() {
		// get a deep clone of a template object
		// by serialization and deserialization (JSON)
		ObjectMapper objectMapper = new ObjectMapper();
		DiseaseRibbonSummary deepCopy = null;
		try {
			deepCopy = objectMapper.readValue(objectMapper.writeValueAsString(getDiseaseRibbonSections()), DiseaseRibbonSummary.class);
		} catch (IOException e) {
			Log.error(e.toString());
		}

		return deepCopy;
	}

	public static Map<String, List<String>> slimParentTermIdMap = new LinkedHashMap<>();

	static {
		createAllEntryCategory("Infection", "All disease by infectious agent", "DOID:0050117");
		createAllEntryCategory("Disease of Anatomy", "All disease of anatomical entity", "DOID:7");
		createAllEntryCategory("Neoplasm", "All disease of cellular proliferation", "DOID:14566");
		createAllEntryCategory("Genetic Disease", "All genetic disease", "DOID:630");
		createAllEntryCategory("Other Disease", "All other disease", DOID_OTHER);
	}

	private static void createAllEntryCategory(String label, String displayName, String doID) {
		List<String> term = new ArrayList<>();
		term.add(label);
		term.add(displayName);
		slimParentTermIdMap.put(doID, term);
	}

	private DiseaseRibbonSummary getDiseaseRibbonSections() {
		if (diseaseRibbonSummary != null) {
			return diseaseRibbonSummary;
		}

		diseaseRibbonSummary = new DiseaseRibbonSummary();

		slimParentTermIdMap.forEach((id, names) -> {
			DiseaseRibbonSection section = new DiseaseRibbonSection();
			section.setLabel(names.get(0));
			section.setId(id);
			SectionSlim allSlimElement = new SectionSlim();
			allSlimElement.setId(id);
			allSlimElement.setLabel(names.get(1));
			allSlimElement.setTypeAll();
			if (!id.equals(DOID_OTHER)) {
				DiseaseSummaryDocument doc = diseaseESDAO.getById(id);
				String definition = doc != null && doc.getDoTerm() != null ? doc.getDoTerm().getDefinition() : null;
				section.setDescription(definition);
				allSlimElement.setDescription(definition);
			} else {
				final String description = "Terms that do not fall into any other group";
				section.setDescription(description);
				allSlimElement.setDescription(description);
			}
			section.addDiseaseSlim(allSlimElement);
			diseaseRibbonSummary.addDiseaseRibbonSection(section);
		});

		diseaseRibbonSummary.getDiseaseRibbonSections().stream()
			.filter(diseaseRibbonSection -> diseaseRibbonSection.getId() != null)
			.filter(diseaseRibbonSection -> !diseaseRibbonSection.getId().equals(DiseaseRibbonSummary.DOID_ALL_ANNOTATIONS))
			.filter(diseaseRibbonSection -> !diseaseRibbonSection.getId().equals(DOID_OTHER))
			.forEach(diseaseRibbonSection -> {
				DiseaseSummaryDocument doc = diseaseESDAO.getById(diseaseRibbonSection.getId());
				if (doc != null && doc.getDoTerm() != null) {
					diseaseRibbonSection.setDescription(doc.getDoTerm().getDefinition());
				}
			});

//		  Map<String, Set<String>> closureMapping = diseaseRepository.getClosureChildToParentsMapping();

		List<DOTerm> doList = diseaseRepository.getAgrDoSlim();
		doList.forEach(doTerm -> {
			List<String> slimFoundList = new ArrayList<>();
			diseaseRibbonSummary.getDiseaseRibbonSections().forEach(diseaseRibbonSection -> {
				if (diseaseRepository.getDOParentTermIDs(doTerm.getPrimaryKey()).contains(diseaseRibbonSection.getId())) {
					SectionSlim slim = new SectionSlim();
					slim.setId(doTerm.getPrimaryKey());
					slim.setLabel(doTerm.getName());
					slim.setDescription(doTerm.getDefinition());
					diseaseRibbonSection.addDiseaseSlim(slim);
					slimFoundList.add(doTerm.getPrimaryKey());
				}
			});
			if (slimFoundList.isEmpty()) {
				SectionSlim slim = new SectionSlim();
				slim.setId(doTerm.getPrimaryKey());
				slim.setLabel(doTerm.getName());
				slim.setDescription(doTerm.getDefinition());
				diseaseRibbonSummary.getOtherSection().addDiseaseSlim(slim);
			}
		});

		return diseaseRibbonSummary;
	}

}
