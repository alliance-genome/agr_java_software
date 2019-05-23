package org.alliancegenome.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alliancegenome.api.entity.DiseaseRibbonSection;
import org.alliancegenome.api.entity.DiseaseRibbonSummary;
import org.alliancegenome.api.entity.DiseaseSectionSlim;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.enterprise.context.RequestScoped;
import java.io.IOException;
import java.util.*;

import static org.alliancegenome.api.entity.DiseaseRibbonSummary.DOID_ALL_ANNOTATIONS;
import static org.alliancegenome.api.entity.DiseaseRibbonSummary.DOID_OTHER;

@RequestScoped
public class DiseaseRibbonService {

    private Log log = LogFactory.getLog(getClass());
    private DiseaseRepository diseaseRepository = new DiseaseRepository();

    private static DiseaseRibbonSummary diseaseRibbonSummary;

    public DiseaseRibbonSummary getDiseaseRibbonSectionInfo() {
        // get a deep clone of a template object
        // by serialization and deserialization (JSON)
        ObjectMapper objectMapper = new ObjectMapper();
        DiseaseRibbonSummary deepCopy = null;
        try {
            deepCopy = objectMapper.readValue(objectMapper.writeValueAsString(getDiseaseRibbonSections()), DiseaseRibbonSummary.class);
        } catch (IOException e) {
            log.error(e);
        }

        return deepCopy;
    }

    public static Map<String, String> slimParentTermIdMap = new LinkedHashMap<>();

    static {
        slimParentTermIdMap.put("DOID:0050117", "Infection");
        slimParentTermIdMap.put("DOID:7", "Disease of Anatomy");
        slimParentTermIdMap.put("DOID:14566", "Neoplasm");
        slimParentTermIdMap.put("DOID:630", "Genetic Disease");
    }

    private DiseaseRibbonSummary getDiseaseRibbonSections() {
        if (diseaseRibbonSummary != null) {
            return diseaseRibbonSummary;
        }

        diseaseRibbonSummary = new DiseaseRibbonSummary();

        slimParentTermIdMap.forEach((id, name) -> {
            DiseaseRibbonSection section = new DiseaseRibbonSection();
            section.setLabel(name);
            section.setId(id);
            diseaseRibbonSummary.addRibbonSection(section);
        });

        DiseaseRibbonSection section5 = new DiseaseRibbonSection();
        section5.setLabel("Other Disease");
        section5.setId(DOID_OTHER);
        diseaseRibbonSummary.addRibbonSection(section5);

        diseaseRibbonSummary.getDiseaseRibbonSections().stream()
                .filter(diseaseRibbonSection -> diseaseRibbonSection.getId() != null)
                .filter(diseaseRibbonSection -> !diseaseRibbonSection.getId().equals(DOID_ALL_ANNOTATIONS))
                .filter(diseaseRibbonSection -> !diseaseRibbonSection.getId().equals(DOID_OTHER))
                .forEach(diseaseRibbonSection -> {
                    DOTerm term = diseaseRepository.getShallowDiseaseTerm(diseaseRibbonSection.getId());
                    diseaseRibbonSection.setDescription(term.getDefinition());
                });

        Map<String, Set<String>> closureMapping = diseaseRepository.getClosureChildMapping();

        List<DOTerm> doList = diseaseRepository.getAgrDoSlim();
        doList.forEach(doTerm -> {
            List<String> slimFoundList = new ArrayList<>();
            diseaseRibbonSummary.getDiseaseRibbonSections().forEach(diseaseRibbonSection -> {
                if (closureMapping.get(doTerm.getPrimaryKey()).contains(diseaseRibbonSection.getId())) {
                    DiseaseSectionSlim slim = new DiseaseSectionSlim();
                    slim.setId(doTerm.getPrimaryKey());
                    slim.setLabel(doTerm.getName());
                    slim.setDescription(doTerm.getDefinition());
                    diseaseRibbonSection.addDiseaseSlim(slim);
                    slimFoundList.add(doTerm.getPrimaryKey());
                }
            });
            if (slimFoundList.isEmpty()) {
                DiseaseSectionSlim slim = new DiseaseSectionSlim();
                slim.setId(doTerm.getPrimaryKey());
                slim.setLabel(doTerm.getName());
                diseaseRibbonSummary.getOtherSection().addDiseaseSlim(slim);
            }
        });

        return diseaseRibbonSummary;
    }

    public Set<String> getSlimId(String doID) {
        Map<String, Set<String>> closureMapping = diseaseRepository.getClosureChildMapping();
        List<DOTerm> doList = diseaseRepository.getAgrDoSlim();
        Set<String> partOfSlimList = new HashSet<>(3);
        List<String> slimFoundList = new ArrayList<>();
        doList.forEach(doTerm -> {
            final String slimDoID = doTerm.getPrimaryKey();
            if (closureMapping.get(doID).contains(slimDoID)) {
                partOfSlimList.add(slimDoID);
                slimFoundList.add(slimDoID);
            }
        });
        slimParentTermIdMap.keySet().forEach(id -> {
            if (closureMapping.get(doID).contains(id)) {
                partOfSlimList.add(id);
                slimFoundList.add(id);
            }
        });
        if (slimFoundList.isEmpty()) {
            partOfSlimList.add(getDiseaseRibbonSectionInfo().getOtherSection().getId());
        }
        return partOfSlimList;
    }

}

