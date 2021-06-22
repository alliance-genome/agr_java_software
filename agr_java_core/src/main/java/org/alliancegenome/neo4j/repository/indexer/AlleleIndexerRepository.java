package org.alliancegenome.neo4j.repository.indexer;

import java.util.*;
import java.util.concurrent.*;

import org.alliancegenome.es.index.site.cache.AlleleDocumentCache;
import org.alliancegenome.es.util.CollectionHelper;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.repository.AlleleRepository;
import org.alliancegenome.neo4j.repository.Neo4jRepository;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.*;

public class AlleleIndexerRepository extends AlleleRepository {

    private final Logger log = LogManager.getLogger(getClass());

    private AlleleDocumentCache cache = new AlleleDocumentCache();

    public AlleleDocumentCache getAlleleDocumentCache() {
        log.info("Building AlleleDocumentCache");

        ExecutorService executor = Executors.newFixedThreadPool(20); // Run all at once
        
        executor.execute(new GetAlleleMapThread());
        executor.execute(new GetAlleleVariantsMapThread());
        executor.execute(new GetCrossReferencesThread());
        executor.execute(new GetConstructsThread());
        executor.execute(new GetConstructExpressedComponentsThread());
        executor.execute(new GetConstructKnockdownComponent());
        executor.execute(new GetConstructRegulatoryRegions());
        executor.execute(new GetDiseaseMapThread());
        executor.execute(new GetDiseasesAgrSlimMapThread());
        executor.execute(new GetDiseaseWithParentsThread());
        executor.execute(new GetGenesMapThread());
        executor.execute(new GetGeneSynonymsThreadThread());
        executor.execute(new GetGeneCrossReferencesThread());
        executor.execute(new GetModelsThread());
        executor.execute(new GetPhenotypeStatementsMapThread());
        executor.execute(new GetVariantsThread());
        executor.execute(new GetVariantSynonymsThread());
        executor.execute(new GetVariantTypeMapThread());
        executor.execute(new GetMolecularConsequence());
        
        executor.shutdown();
        while (!executor.isTerminated()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        log.info("Finished Building AlleleDocumentCache");

        return cache;

    }
    private class GetAlleleVariantsMapThread implements Runnable {

        @Override
        public void run() {
            log.info("Fetching alleles objects");
            cache.setAlleleVariantMap(getAllAlleleVariants());
            log.info("Finished Fetching alleles objects");
        }
    }
    private class GetAlleleMapThread implements Runnable {

        @Override
        public void run() {
            log.info("Fetching alleles");
            String query = "MATCH p1=(species:Species)-[:FROM_SPECIES]-(feature:Allele) ";
            query += " OPTIONAL MATCH pSyn=(feature:Feature)-[:ALSO_KNOWN_AS]-(synonym:Synonym) ";
            query += " RETURN p1, pSyn ";

            Iterable<Allele> alleles = null;

            alleles = query(query);

            Map<String, Allele> alleleMap = new HashMap<>();
            for (Allele allele : alleles) {
                alleleMap.put(allele.getPrimaryKey(),allele);
            }
            cache.setAlleleMap(alleleMap);
            log.info("Finished Fetching alleles");
        }
    }
    
    private class GetCrossReferencesThread implements Runnable {

        @Override
        public void run() {
            log.info("Building allele -> crossReference map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CROSS_REFERENCE]-(cr:CrossReference) ";
            query += " RETURN allele.primaryKey as id, cr.name as value";

            Map<String, Set<String>> names = getMapSetForQuery(query);

            query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CROSS_REFERENCE]-(cr:CrossReference) ";
            query += " RETURN allele.primaryKey as id, cr.localId as value";

            Map<String, Set<String>> localIds = getMapSetForQuery(query);

            cache.setCrossReferences(CollectionHelper.merge(names, localIds));
            log.info("Finished Building allele -> crossReference map");
        }
    }

    private class GetConstructsThread implements Runnable {

        @Override
        public void run() {
            log.info("Fetching allele -> constructs map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct) ";
            query += " RETURN allele.primaryKey as id, [construct.nameText, construct.primaryKey] as value";

            Map<String, Set<String>> constructs = getMapSetForQuery(query);

            query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:ALSO_KNOWN_AS]-(synonym:Synonym) ";

            query += "RETURN allele.primaryKey as id, synonym.name as value ";

            constructs = CollectionHelper.merge(constructs, getMapSetForQuery(query));

            query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:CROSS_REFERENCE]-(crossReference:CrossReference) ";
            query += "RETURN allele.primaryKey as id, crossReference.name as value ";

            constructs = CollectionHelper.merge(constructs, getMapSetForQuery(query));

            query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:ALSO_KNOWN_AS]-(secondaryId:SecondaryId) ";

            query += " RETURN allele.primaryKey as id, secondaryId.name as value";

            constructs = CollectionHelper.merge(constructs, getMapSetForQuery(query));

            query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:EXPRESSES|TARGETS|IS_REGULATED_BY]-(gene:Gene) ";
            query += " RETURN allele.primaryKey as id, gene.primaryKey as value; ";

            constructs = CollectionHelper.merge(constructs, getMapSetForQuery(query));
            
            cache.setConstructs(constructs);
            log.info("Finished Fetching allele -> constructs map");
        }
    }

    private class GetConstructExpressedComponentsThread implements Runnable {

        @Override
        public void run() {
            log.info("Fetching allele -> constructExpressedComponent map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:EXPRESSES]-(constructComponent:NonBGIConstructComponent) ";
            query += " RETURN allele.primaryKey as id, constructComponent.primaryKey as value";

            Map<String, Set<String>> result = getMapSetForQuery(query);

            query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:EXPRESSES]-(gene:Gene) ";
            query += " RETURN allele.primaryKey as id, gene.symbolWithSpecies as value; ";
            
            cache.setConstructExpressedComponents(CollectionHelper.merge(result, getMapSetForQuery(query)));
            log.info("Finished Fetching allele -> constructExpressedComponent map");
        }
    }

    private class GetConstructKnockdownComponent implements Runnable {

        @Override
        public void run() {
            log.info("Fetching allele -> constructKnockdownComponent map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:TARGETS]-(constructComponent:NonBGIConstructComponent) ";
            query += " RETURN allele.primaryKey as id, constructComponent.primaryKey as value";

            Map<String, Set<String>> result = getMapSetForQuery(query);

            query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:TARGETS]-(gene:Gene) ";
            query += " RETURN allele.primaryKey as id, gene.symbolWithSpecies as value; ";

            cache.setConstructKnockdownComponents(CollectionHelper.merge(result, getMapSetForQuery(query)));
            log.info("Finished Fetching allele -> constructKnockdownComponent map");
        }
    }
    
    private class GetConstructRegulatoryRegions implements Runnable {

        @Override
        public void run() {
            log.info("Fetching allele -> constructRegulatoryRegion map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:IS_REGULATED_BY]-(constructComponent:NonBGIConstructComponent) ";
            query += " RETURN allele.primaryKey as id, constructComponent.primaryKey as value";

            Map<String, Set<String>> result = getMapSetForQuery(query);

            query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:IS_REGULATED_BY]-(gene:Gene) ";
            query += " RETURN allele.primaryKey as id, gene.symbolWithSpecies as value; ";

            cache.setConstructRegulatoryRegions(CollectionHelper.merge(result, getMapSetForQuery(query)));
            log.info("Finished Fetching allele -> constructRegulatoryRegion map");
        }
    }

    private class GetDiseaseMapThread implements Runnable {

        @Override
        public void run() {
            log.info("Building allele -> diseases map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:IS_IMPLICATED_IN]-(disease:DOTerm) ";
            query += " RETURN a.primaryKey, disease.nameKey ";

            cache.setDiseases(getMapSetForQuery(query, "a.primaryKey", "disease.nameKey"));
            log.info("Finished Building allele -> diseases map");
        }
    }

    private class GetDiseasesAgrSlimMapThread implements Runnable {

        @Override
        public void run() {
            log.info("Building allele -> diseasesAgrSlim map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:IS_IMPLICATED_IN]-(:DOTerm)-[:IS_A_PART_OF_CLOSURE]->(disease:DOTerm)";
            query += " WHERE disease.subset =~ '.*DO_AGR_slim.*' ";
            query += " RETURN a.primaryKey, disease.nameKey ";

            cache.setDiseasesAgrSlim(getMapSetForQuery(query, "a.primaryKey", "disease.nameKey"));
            log.info("Finished Building allele -> diseasesAgrSlim map");
        }
    }
    
    private class GetDiseaseWithParentsThread implements Runnable {

        @Override
        public void run() {
            log.info("Building allele -> diseasesWithParents map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:IS_IMPLICATED_IN]-(:DOTerm)-[:IS_A_PART_OF_CLOSURE]->(disease:DOTerm)";

            query += " RETURN a.primaryKey, disease.nameKey ";

            cache.setDiseasesWithParents(getMapSetForQuery(query, "a.primaryKey", "disease.nameKey"));
            log.info("Finished Building allele -> diseasesWithParents map");
        }
    }

    private class GetGenesMapThread implements Runnable {

        @Override
        public void run() {
            log.info("Building allele -> genes & gene Ids map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)-[:IS_ALLELE_OF]-(a:Allele) ";
            query += "RETURN distinct a.primaryKey, gene.symbolWithSpecies, gene.primaryKey";

            cache.setGenes(getMapSetForQuery(query, "a.primaryKey", "gene.symbolWithSpecies"));
            cache.setGeneIds(getMapSetForQuery(query, "a.primaryKey", "gene.primaryKey"));

            log.info("Finished Building allele -> genes & gene Ids map");
        }
    }

    private class GetGeneSynonymsThreadThread implements Runnable {

        @Override
        public void run() {
            log.info("Building allele -> genes synonyms map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:IS_ALLELE_OF]-(gene:Gene)-[:ALSO_KNOWN_AS]-(synonym:Synonym) ";
            query += "RETURN a.primaryKey, synonym.name";

            cache.setGeneSynonyms(getMapSetForQuery(query, "a.primaryKey", "synonym.name"));
            log.info("Finished Building allele -> genes synonyms map");
        }
    }

    private class GetGeneCrossReferencesThread implements Runnable {

        @Override
        public void run() {
            log.info("Building allele -> gene crossreferences map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:IS_ALLELE_OF]-(gene:Gene)-[:CROSS_REFERENCE]-(cr:CrossReference) ";
            query += "RETURN a.primaryKey, cr.name";

            cache.setGeneCrossReferences(getMapSetForQuery(query, "a.primaryKey", "cr.name"));
            log.info("Finished Building allele -> gene crossreferences map");
        }
    }

    private class GetModelsThread implements Runnable {

        @Override
        public void run() {
            log.info("Building allele -> model map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(model:AffectedGenomicModel)-[:MODEL_COMPONENT]-(allele:Allele)";
            query += " RETURN allele.primaryKey as id, model.nameTextWithSpecies as value";

            cache.setModels(getMapSetForQuery(query));
            log.info("Finished Building allele -> model map");
        }
    }
    
    private class GetPhenotypeStatementsMapThread implements Runnable {

        @Override
        public void run() {
            log.info("Building allele -> phenotype statements map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:HAS_PHENOTYPE]-(phenotype:Phenotype) ";
            query += " RETURN distinct a.primaryKey, phenotype.phenotypeStatement ";

            cache.setPhenotypeStatements(getMapSetForQuery(query, "a.primaryKey", "phenotype.phenotypeStatement"));
            log.info("Finished Building allele -> phenotype statements map");
        }
    }
    
    private class GetVariantsThread implements Runnable {

        @Override
        public void run() {
            log.info("Building allele -> variant name map");
            String query = " MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:VARIATION]-(v:Variant) ";
            query += " RETURN distinct a.primaryKey as id, v.name as value";

            cache.setVariants(getMapSetForQuery(query));
            log.info("Finished Building allele -> variant name map");
        }
    }
    
    private class GetVariantSynonymsThread implements Runnable {

        @Override
        public void run() {
            log.info("Building allele -> variant synonyms map");
            String query = " MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:VARIATION]-(v:Variant)-[:ASSOCIATION]-(tlc:TranscriptLevelConsequence)  ";

            query += "RETURN distinct a.primaryKey as id, [v.hgvsNomenclature, tlc.hgvsVEPGeneNomenclature, tlc.hgvsProteinNomenclature, tlc.hgvsCodingNomenclature] as value  ";
            Map<String,Set<String>> tlcNames = getMapSetForQuery(query);

            query = "MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:VARIATION]-(v:Variant)-[:ALSO_KNOWN_AS]-(synonym:Synonym) ";

            query += " RETURN a.primaryKey as id, synonym.name as value ";
            Map<String,Set<String>> synonyms = getMapSetForQuery(query);

            cache.setVariantSynonyms(CollectionHelper.merge(tlcNames, synonyms));
            log.info("Finished Building allele -> variant synonyms map");
        }
    }
    
    private class GetVariantTypeMapThread implements Runnable {

        @Override
        public void run() {
            log.info("Building allele -> variant types map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:VARIATION]-(v:Variant)-[:VARIATION_TYPE]-(term:SOTerm) ";
            query += " RETURN distinct a.primaryKey,term.name ";

            cache.setVariantType(getMapSetForQuery(query, "a.primaryKey", "term.name"));
            log.info("Finished Building allele -> variant types map");
        }
    }
    
    private class GetMolecularConsequence implements Runnable {

        @Override
        public void run() {
            log.info("Building allele -> molecular consequence map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:VARIATION]-(v:Variant)-[:ASSOCIATION]-(consequence:GeneLevelConsequence) ";
            query += " RETURN a.primaryKey as id, consequence.geneLevelConsequence as value ";

            cache.setMolecularConsequenceMap(getMapSetForQuery(query));
            log.info("Finished Building allele -> molecular consequence map");
        }
    }

}
