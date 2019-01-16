package org.alliancegenome.indexer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.translators.document.AlleleTranslator;
import org.alliancegenome.core.translators.document.DiseaseTranslator;
import org.alliancegenome.es.index.site.document.AlleleDocument;
import org.alliancegenome.es.index.site.document.DiseaseAnnotationDocument;
import org.alliancegenome.es.index.site.document.DiseaseDocument;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.entity.node.DiseaseEntityJoin;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.Publication;
import org.alliancegenome.neo4j.repository.AlleleRepository;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.repository.Neo4jRepository;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class DiseaseTest {

    private AlleleRepository alleleRepository;
    private DiseaseRepository diseaseRepository;
    private GeneRepository geneRepository;

    private AlleleTranslator alleleTranslator;
    private DiseaseTranslator diseaseTranslator;

    @Before
    public void before() {
        Configurator.setRootLevel(Level.WARN);
        //Configurator.setLevel("org.neo4j",Level.DEBUG);
        Logger log = LogManager.getLogger(DiseaseTest.class);
        log.info("Hallo");
        ConfigHelper.init();

        alleleRepository = new AlleleRepository();
        diseaseRepository = new DiseaseRepository();
        geneRepository = new GeneRepository();

        alleleTranslator = new AlleleTranslator();
        diseaseTranslator = new DiseaseTranslator();
    }

    public static void main(String[] args) throws Exception {
        Configurator.setRootLevel(Level.WARN);
        //Configurator.setLevel("org.neo4j",Level.DEBUG);
        Logger log = LogManager.getLogger(DiseaseTest.class);
        log.info("Hallo");

        DiseaseRepository diseaseRepository = new DiseaseRepository();
        AlleleRepository alleleRepository = new AlleleRepository();
        GeneRepository geneRepository = new GeneRepository();
/*
        Iterable<DOTerm> disease_entities = neo4jService.getPage(0, 1000, 3);

        disease_entities.forEach(entity -> {
            if (entity.getGeneMap() != null)
                System.out.println(entity);
        });
        Collection<DOTerm> entityt = neo4jService.getEntity("primaryKey", "DOID:9281");
*/

        DiseaseTest test = new DiseaseTest();
//        test.testReferencesForAllele();
        test.testReferencesForAlleleDiseaseAnnotations();
        Neo4jRepository<DOTerm> neo4jService = new Neo4jRepository<>(DOTerm.class);

        DiseaseTranslator translator = new DiseaseTranslator();
        AlleleTranslator alleleTranslator = new AlleleTranslator();
        //Allele allele = alleleRepository.getAllele("ZFIN:ZDB-ALT-980203-985");

        //Gene gene = geneRepository.getOneGene("MGI:94909");
        //Gene gene = geneRepository.getOneGene("MGI:1202717");
        Gene gene = geneRepository.getOneGene("MGI:97747");
        Allele allele = alleleRepository.getAllele("MGI:3029164");
        AlleleDocument alleleDoc = alleleTranslator.translate(allele);
        Map<DOTerm, List<DiseaseEntityJoin>> map = gene.getDiseaseEntityJoins().stream()
                .collect(Collectors.groupingBy(o -> o.getDisease()));

        Map<Publication, List<DiseaseEntityJoin>> mapPub = gene.getDiseaseEntityJoins().stream()
                .collect(Collectors.groupingBy(o -> o.getPublication()));

        DOTerm diseaseTerm = diseaseRepository.getDiseaseTerm("DOID:0050700");
        DOTerm diseaseTerm1 = diseaseRepository.getDiseaseTermWithAnnotations("DOID:0050700");
        DiseaseDocument doc = translator.translate(diseaseTerm, 1);
        List<DOTerm> doList = new ArrayList<>(Collections.singletonList(diseaseTerm));
        Iterable<DiseaseAnnotationDocument> annotDoc = translator.translateAnnotationEntities(doList, 1);

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(doc);
        System.out.println(json);

        //List<DOTerm> geneDiseaseList = diseaseRepository.getAllDiseaseTerms(0, 10);
/*
        List<DOTerm> geneDiseaseList = diseaseRepository.getAllTerms();
        System.out.println("Number of all terms: " + geneDiseaseList.size());
*/
        //List<DOTerm>  geneDiseaseList1 = diseaseRepository.getDiseaseTermsWithAnnotations();

        //System.out.println("Number of Diseases with Genes: " + geneDiseaseList1.size());


        String cypher = "match (n:Gene)-[*]->(d:DOTerm) return n, d";
        //geneDiseaseList = (List<DOTerm>) neo4jService.query(cypher);


        //List<DOTerm> fullTerms = geneDiseaseList1.stream()
        //        .filter(doTerm -> !(doTerm.getPrimaryKey().contains("!")))
        //        .collect(Collectors.toList());
        //System.out.println("Number of Diseases with Genes Info: " + geneDiseaseList1.size());

    }

    public void testReferencesForAllele() {
        Allele allele = alleleRepository.getAllele("MGI:2156738");
        AlleleDocument alleleDoc = alleleTranslator.translate(allele);

        assertNotNull(allele);
    }

    @Test
    @Ignore
    public void getGeneAlleleAnnotationMap() {
        // Peters anomaly
        DOTerm disease = diseaseRepository.getDiseaseTerm("DOID:0060673");
        // Pax6
        Gene gene = geneRepository.getOneGene("MGI:97490");
        Map<Gene, Map<Optional<Allele>, List<DiseaseEntityJoin>>> map = diseaseTranslator.getGeneAlleleAnnotationMap(disease, gene);

        assertNotNull(map);
        assert (map.keySet().size() == 1);
        Map<Optional<Allele>, List<DiseaseEntityJoin>> diseaseMap = map.get(gene);
        assert (diseaseMap.keySet().size() == 6);
        diseaseMap.forEach((allele, diseaseEntityJoinList) -> {
            if (!allele.isPresent()) {
                assertThat("There should be 3 publications for the alleleless record", diseaseEntityJoinList.size(), equalTo(3));
            }
            if (allele.isPresent() && allele.get().getSymbol().contains("7Neu")) {
                assertThat("There should be 1 publication for Pax6<7Neu> record", diseaseEntityJoinList.size(), equalTo(1));
                assertThat(diseaseEntityJoinList.get(0).getPublication().getPubMedId(), equalTo("PMID:11779807"));

            }
        });
    }

    @Test
    @Ignore
    public void testReferencesForAlleleDiseaseAnnotations() {
        DOTerm disease = diseaseRepository.getDiseaseTerm("DOID:0060673");
        List<DOTerm> list = new ArrayList<>();
        list.add(disease);
        Gene gene = geneRepository.getOneGene("MGI:97490");
        Set<DiseaseAnnotationDocument> set = (Set) diseaseTranslator.translateAnnotationEntities(list, 1);

        assertNotNull(set);
        assertThat(set.size(), equalTo(9));
    }


}
