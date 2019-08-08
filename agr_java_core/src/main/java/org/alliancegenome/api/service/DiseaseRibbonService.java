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

    public static Map<String, List<String>> slimParentTermIdMap = new LinkedHashMap<>();

    static {
        List<String> infection = new ArrayList<>();
        infection.add("Infection");
        infection.add("All disease by infectious agent");
        slimParentTermIdMap.put("DOID:0050117", infection);

        List<String> anatomy = new ArrayList<>();
        anatomy.add("Disease of Anatomy");
        anatomy.add("All disease of anatomical entity");
        slimParentTermIdMap.put("DOID:7", anatomy);

        List<String> neoplasm = new ArrayList<>();
        neoplasm.add("Neoplasm");
        neoplasm.add("All disease of cellular proliferation");
        slimParentTermIdMap.put("DOID:14566", neoplasm);

        List<String> disease = new ArrayList<>();
        disease.add("Genetic Disease");
        disease.add("All genetic disease");
        slimParentTermIdMap.put("DOID:630", disease);
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
            DOTerm term = diseaseRepository.getShallowDiseaseTerm(id);
            section.setDescription(term.getDefinition());
            DiseaseSectionSlim allSlimElement = new DiseaseSectionSlim();
            allSlimElement.setId(id);
            allSlimElement.setLabel(names.get(1));
            allSlimElement.setTypeAll();
            allSlimElement.setDescription(term.getDefinition());
            section.addDiseaseSlim(allSlimElement);
            diseaseRibbonSummary.addDiseaseRibbonSection(section);
        });

        diseaseRibbonSummary.getDiseaseRibbonSections().stream()
                .filter(diseaseRibbonSection -> diseaseRibbonSection.getId() != null)
                .filter(diseaseRibbonSection -> !diseaseRibbonSection.getId().equals(DiseaseRibbonSummary.DOID_ALL_ANNOTATIONS))
                .filter(diseaseRibbonSection -> !diseaseRibbonSection.getId().equals(DiseaseRibbonSummary.DOID_OTHER))
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
                slim.setDescription(doTerm.getDefinition());
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

