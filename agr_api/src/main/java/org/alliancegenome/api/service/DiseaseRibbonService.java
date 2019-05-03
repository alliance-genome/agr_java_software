package org.alliancegenome.api.service;

import org.alliancegenome.api.entity.DiseaseRibbonSection;
import org.alliancegenome.api.entity.DiseaseSectionSlim;
import org.alliancegenome.api.service.helper.DiseaseRibbonSummary;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.enterprise.context.RequestScoped;
import java.util.*;

@RequestScoped
public class DiseaseRibbonService {

    private Log log = LogFactory.getLog(getClass());
    private DiseaseRepository diseaseRepository = new DiseaseRepository();
    private DiseaseRibbonSummary summary;

    public DiseaseRibbonSummary getDiseaseRibbonSectionInfo() {
        if (summary != null)
            return summary;

        summary = new DiseaseRibbonSummary();

        DiseaseRibbonSection section0 = new DiseaseRibbonSection();
        summary.addRibbonSection(section0);

        DiseaseRibbonSection section1 = new DiseaseRibbonSection();
        section1.setLabel("Infection");
        section1.setId("DOID:0050117");
        summary.addRibbonSection(section1);

        DiseaseRibbonSection section2 = new DiseaseRibbonSection();
        section2.setLabel("Disease of Anatomy");
        section2.setId("DOID:7");
        summary.addRibbonSection(section2);

        DiseaseRibbonSection section3 = new DiseaseRibbonSection();
        section3.setLabel("Neoplasm");
        section3.setId("DOID:14566");
        summary.addRibbonSection(section3);


        DiseaseRibbonSection section4 = new DiseaseRibbonSection();
        section4.setLabel("Genetic Disease");
        section4.setId("DOID:630");
        summary.addRibbonSection(section4);

        DiseaseRibbonSection section5 = new DiseaseRibbonSection();
        section5.setLabel("Other Disease");
        section5.setId("DOID:000");
        summary.addRibbonSection(section5);

        Map<String, Set<String>> closureMapping = diseaseRepository.getClosureChildMapping();

        List<DOTerm> doList = diseaseRepository.getAgrDoSlim();
        doList.forEach(doTerm -> {
            List<String> slimFoundList = new ArrayList<>();
            summary.getDiseaseRibbonSections().forEach(diseaseRibbonSection -> {
                if (closureMapping.get(doTerm.getPrimaryKey()).contains(diseaseRibbonSection.getId())) {
                    DiseaseSectionSlim slim = new DiseaseSectionSlim();
                    slim.setId(doTerm.getPrimaryKey());
                    slim.setLabel(doTerm.getName());
                    diseaseRibbonSection.addDiseaseSlim(slim);
                    slimFoundList.add(doTerm.getPrimaryKey());
                }
            });
            if (slimFoundList.isEmpty()) {
                DiseaseSectionSlim slim = new DiseaseSectionSlim();
                slim.setId(doTerm.getPrimaryKey());
                slim.setLabel(doTerm.getName());
                summary.getOtherSection().addDiseaseSlim(slim);
            }
        });

        return summary;
    }

    public Set<String> getSlimId(String doID) {
        Map<String, Set<String>> closureMapping = diseaseRepository.getClosureChildMapping();
        List<DOTerm> doList = diseaseRepository.getAgrDoSlim();
        Set<String> partOfSlimList = new HashSet<>(3);
        List<String> slimFoundList = new ArrayList<>();
        doList.forEach(doTerm -> {
            if (closureMapping.get(doID).contains(doTerm.getPrimaryKey())) {
                partOfSlimList.add(doTerm.getPrimaryKey());
                slimFoundList.add(doTerm.getPrimaryKey());
            }
        });
        if (slimFoundList.isEmpty()) {
            partOfSlimList.add(getDiseaseRibbonSectionInfo().getOtherSection().getId());
        }
        return partOfSlimList;
    }

}

