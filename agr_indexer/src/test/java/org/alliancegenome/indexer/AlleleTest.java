package org.alliancegenome.indexer;

import org.alliancegenome.core.translators.document.AlleleTranslator;
import org.alliancegenome.es.index.site.document.AlleleDocument;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.repository.AlleleRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AlleleTest {

    public static void main(String[] args) throws Exception {
        AlleleRepository repo = new AlleleRepository();
        AlleleTranslator trans = new AlleleTranslator();

        Allele allele = repo.getAllele("MGI:2148259");

        AlleleDocument alleleDocument = trans.translate(allele);

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(alleleDocument);
        System.out.println(json);
    }
}
