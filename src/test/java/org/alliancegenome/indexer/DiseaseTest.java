package org.alliancegenome.indexer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alliancegenome.shared.config.ConfigHelper;
import org.alliancegenome.shared.es.document.site_index.DiseaseAnnotationDocument;
import org.alliancegenome.shared.es.document.site_index.DiseaseDocument;
import org.alliancegenome.shared.es.document.site_index.FeatureDocument;
import org.alliancegenome.shared.neo4j.entity.node.DOTerm;
import org.alliancegenome.shared.neo4j.repository.DiseaseRepository;
import org.alliancegenome.shared.neo4j.repository.Neo4jRepository;
import org.alliancegenome.shared.neo4j.repository.FeatureRepository;
import org.alliancegenome.shared.neo4j.repository.GeneRepository;
import org.alliancegenome.translators.DiseaseTranslator;
import org.alliancegenome.translators.FeatureTranslator;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNotNull;

public class DiseaseTest {

    private FeatureRepository featureRepository;
    private DiseaseRepository diseaseRepository;
    private GeneRepository geneRepository;

    private FeatureTranslator featureTranslator;
    private DiseaseTranslator diseaseTranslator;

    @Before
    public void before() {
        Configurator.setRootLevel(Level.WARN);
        //Configurator.setLevel("org.neo4j",Level.DEBUG);
        Logger log = LogManager.getLogger(DiseaseTest.class);
        log.info("Hallo");
        ConfigHelper.init();

        featureRepository = new FeatureRepository();
        diseaseRepository = new DiseaseRepository();
        geneRepository = new GeneRepository();

        featureTranslator = new FeatureTranslator();
        diseaseTranslator = new DiseaseTranslator();
    }

    public static void main(String[] args) throws Exception {
        Configurator.setRootLevel(Level.WARN);
        //Configurator.setLevel("org.neo4j",Level.DEBUG);
        Logger log = LogManager.getLogger(DiseaseTest.class);
        log.info("Hallo");
        ConfigHelper.init();

        DiseaseRepository diseaseRepository = new DiseaseRepository();
        FeatureRepository featureRepository = new FeatureRepository();
        GeneRepository geneRepository = new GeneRepository();
/*
        Iterable<DOTerm> disease_entities = neo4jService.getPage(0, 1000, 3);

        disease_entities.forEach(entity -> {
            if (entity.getGenes() != null)
                System.out.println(entity);
        });
        Collection<DOTerm> entityt = neo4jService.getEntity("primaryKey", "DOID:9281");
*/

        DiseaseTest test = new DiseaseTest();
//        test.testReferencesForAllele();
        test.testReferencesForAlleleDiseaseAnnotations();
        Neo4jRepository<DOTerm> neo4jService = new Neo4jRepository<>(DOTerm.class);

        DiseaseTranslator translator = new DiseaseTranslator();
        FeatureTranslator featureTranslator = new FeatureTranslator();
        //Feature feature = featureRepository.getFeature("ZFIN:ZDB-ALT-980203-985");

        //Gene gene = geneRepository.getOneGene("MGI:94909");
        //Gene gene = geneRepository.getOneGene("MGI:1202717");
        Gene gene = geneRepository.getOneGene("MGI:97747");
        Feature feature = featureRepository.getFeature("MGI:3029164");
        FeatureDocument featureDoc = featureTranslator.translate(feature);
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
        Feature feature = featureRepository.getFeature("MGI:2156738");
        FeatureDocument featureDoc = featureTranslator.translate(feature);

        assertNotNull(feature);
    }

    @Test
    @Ignore
    public void getGeneFeatureAnnotationMap() {
        // Peters anomaly
        DOTerm disease = diseaseRepository.getDiseaseTerm("DOID:0060673");
        // Pax6
        Gene gene = geneRepository.getOneGene("MGI:97490");
        Map<Gene, Map<Optional<Feature>, List<DiseaseEntityJoin>>> map = diseaseTranslator.getGeneFeatureAnnotationMap(disease, gene);

        assertNotNull(map);
        assert (map.keySet().size() == 1);
        Map<Optional<Feature>, List<DiseaseEntityJoin>> diseaseMap = map.get(gene);
        assert (diseaseMap.keySet().size() == 6);
        diseaseMap.forEach((feature, diseaseEntityJoinList) -> {
            if (!feature.isPresent()) {
                assertThat("There should be 3 publications for the featureless record", diseaseEntityJoinList.size(), equalTo(3));
            }
            if (feature.isPresent() && feature.get().getSymbol().contains("7Neu")) {
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
