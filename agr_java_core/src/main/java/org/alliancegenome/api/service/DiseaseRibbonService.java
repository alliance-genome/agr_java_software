package org.alliancegenome.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alliancegenome.api.entity.DiseaseRibbonSection;
import org.alliancegenome.api.entity.DiseaseRibbonSummary;
import org.alliancegenome.api.entity.SectionSlim;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.enterprise.context.RequestScoped;
import java.io.IOException;
import java.util.*;

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
                DOTerm term = diseaseRepository.getShallowDiseaseTerm(id);
                section.setDescription(term.getDefinition());
                allSlimElement.setDescription(term.getDefinition());
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
                    DOTerm term = diseaseRepository.getShallowDiseaseTerm(diseaseRibbonSection.getId());
                    diseaseRibbonSection.setDescription(term.getDefinition());
                });

//        Map<String, Set<String>> closureMapping = diseaseRepository.getClosureChildToParentsMapping();

        List<DOTerm> doList = diseaseRepository.getAgrDoSlim();
        doList.forEach(doTerm -> {
            List<String> slimFoundList = new ArrayList<>();
            diseaseRibbonSummary.getDiseaseRibbonSections().forEach(diseaseRibbonSection -> {
                if (diseaseRepository.getChildren(doTerm.getPrimaryKey()).contains(diseaseRibbonSection.getId())) {
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

    public Set<String> getSlimId(String doID) {
        //Map<String, Set<String>> closureMapping = diseaseRepository.getClosureChildToParentsMapping();
        List<DOTerm> doList = diseaseRepository.getAgrDoSlim();
        Set<String> partOfSlimList = new HashSet<>(3);
        List<String> slimFoundList = new ArrayList<>();
        doList.forEach(doTerm -> {
            final String slimDoID = doTerm.getPrimaryKey();
            if (diseaseRepository.getChildren(doID).contains(slimDoID)) {
                partOfSlimList.add(slimDoID);
                slimFoundList.add(slimDoID);
            }
        });

        boolean parentFound = slimParentTermIdMap.keySet().stream()
                .filter(id -> diseaseRepository.getChildren(doID).contains(id))
                .peek(partOfSlimList::add)
                .anyMatch(Objects::nonNull);
        if (!parentFound) {
            partOfSlimList.add(getDiseaseRibbonSectionInfo().getOtherSection().getId());
        }
        return partOfSlimList;
    }

}

