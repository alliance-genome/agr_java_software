package org.alliancegenome.indexer;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.alliancegenome.core.service.OrthologyService;
import org.alliancegenome.core.translators.document.GeneTranslator;
import org.alliancegenome.es.index.site.cache.GeneDocumentCache;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.repository.GoRepository;
import org.alliancegenome.neo4j.view.OrthologyModule;
import org.alliancegenome.neo4j.view.View;
import org.alliancegenome.es.index.site.document.GeneDocument;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GeneTest {

    public static void main(String[] args) throws Exception {

        GeneRepository repo = new GeneRepository();
        GoRepository goRepo = new GoRepository();
        GeneTranslator trans = new GeneTranslator();

        Gene gene = null;
        gene = repo.getOneGene("MGI:104798");
        GeneDocument geneDocument = trans.translate(gene);

        repo.addExpressionListsToGeneDocumentCache(new GeneDocumentCache(), null);

        List<BioEntityGeneExpressionJoin> expressionJoinList = repo.getExpressionAnnotations(gene);

        List<String> uberonTermNames = new ArrayList<>();
        uberonTermNames.addAll(
                gene.getEntityGeneExpressionJoins().stream()
                        .map(BioEntityGeneExpressionJoin::getEntity)
                        .map(ExpressionBioEntity::getAoTermList)
                        .flatMap(List::stream)
                        .map(UBERONTerm::getName)
                        .distinct()
                        .collect(Collectors.toList())
        );

        //gene = repo.getOrthologyGene("ZFIN:ZDB-GENE-990415-72");
        gene = repo.getOrthologyGene("MGI:109583");
        Set<Gene> genes = repo.getOrthologyByTwoSpecies("7955", "10090");

        //for(GOTerm go: gene.getGoParentTerms()) {
        //  System.out.println(go.getName());
        //}

        GOTerm term = goRepo.getOneGoTerm("GO:0005488");

        Date start = new Date();
//        gene = repo.getOneGene("MGI:97490");
        Date end = new Date();

        System.out.println("Time: " + (end.getTime() - start.getTime()));


        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        mapper.registerModule(new OrthologyModule());
//        mapper.writerWithView(View.OrthologyView.class).writeValue(System.out, gene.getOrthoGenes());
        mapper.writerWithView(View.OrthologyView.class).writeValue(System.out, OrthologyService.getOrthologViewList(gene));
        //mapper.writeValue(System.out, OrthologyService.getOrthologyDoclets(gene));
/*
        String json = mapper
                .writerWithView(View.OrthologyView.class)
                .writeValueAsString(gene.getOrthoGenes());
*/
        //String jsona = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(trans.translate(gene));
        //String json = mapper.writeValueAsString(gene);
        //System.out.println(json);

        //String jsonList = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(gene.getOrthoGenes());
        //System.out.println(json);

/*
        gene.getOrthoGenes().forEach(ortholog -> {
            System.out.println(ortholog.);
        });
*/
//        System.out.println(json);


    }

}
