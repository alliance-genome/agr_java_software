package org.alliancegenome.api.tests.integration;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alliancegenome.cache.repository.AlleleCacheRepository;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.relationship.*;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.repository.InteractionRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.util.ProcessDisplayHelper;
//for 
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.entity.relationship.Orthologous;
import org.alliancegenome.neo4j.view.*;
import org.apache.logging.log4j.*;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.alliancegenome.neo4j.repository.Neo4jRepository;
import org.alliancegenome.neo4j.repository.Neo4jSessionFactory;

//log
import org.apache.logging.log4j.*;

public class Main {
     private final Logger log = LogManager.getLogger(getClass());
    
  @SuppressWarnings("unchecked")
public static void main (String[] args) {
      GeneRepository geneRepo = new GeneRepository();
      Gene g=geneRepo.getOneGene("MGI:97490");
      System.out.println(g.getSynonyms());
      
      
     // AlleleCacheRepository alleleCache = new AlleleCacheRepository();
     // Allele a=alleleCache.getAllelesByGene("MGI:97490", pagination)
     
      //here to test filer
    // Calling values() 
      FieldFilter arr[] = FieldFilter.values(); 

      // enum with loop 
      for (FieldFilter col : arr) 
      { 
          // Calling ordinal() to find index 
          // of color. 
          System.out.println(col + " at index "
                           + col.ordinal() + " getName "+ col.getFullName()); 
      }  
      System.out.println(FieldFilter.valueOf("GENE_NAME")); 
      // System.out.println(Color.valueOf("WHITE")); 
      FieldFilter ff =FieldFilter.ALLELE;
      System.out.println(" ff:" + ff + " ff name:" +ff.getFullName());
      
      java.util.List<String> names = 
              java.util.Arrays.asList("Geek","GeeksQuiz","g1","QA","Geek2"); 
      java.util.function.Predicate<String> p =(s)->s.startsWith("G");
      for (String s1:names){
          if (p.test(s1)){
              System.out.println(s1);
          }
      }
     // java.util.stream.Stream
      String[] names1= {"Abs","cdd","Sare" };
      Arrays.stream(names1).filter(x->x.startsWith("S")).sorted().forEach(System.out::println);
      
      //test Interaction object
      InteractionRepository repo = new InteractionRepository();
      List<InteractionGeneJoin> list = repo.getInteractions("MGI:103150"); // 135, 116
      list.forEach(interactionGeneJoin-> interactionGeneJoin.getDetectionsMethods().toString());
      Logger log = LogManager.getLogger(InteractionsIT.class);
      for (InteractionGeneJoin join : list) {
          log.info(join);
      }
      
      //test Orthologous
      String query = " MATCH p1=(g:Gene)-[ortho:ORTHOLOGOUS]->(gh:Gene), ";
      query += "p4=(g:Gene)-->(s:OrthologyGeneJoin)-->(gh:Gene) where g.primaryKey='MGI:109583' ";
       
     // if (filter.getStringency() != null) {
      //    if (filter.getStringency().equals(OrthologyFilter.Stringency.STRINGENT))
     //         query += "and ortho.strictFilter = true ";
     //     if (filter.getStringency().equals(OrthologyFilter.Stringency.MODERATE))
     //         query += "and ortho.moderateFilter = true ";
     // }
      query += "OPTIONAL MATCH p6=(s:OrthologyGeneJoin)-[:NOT_MATCHED]->(notMatched:OrthoAlgorithm) ";
      query += "OPTIONAL MATCH p7=(s:OrthologyGeneJoin)-[:NOT_CALLED]->(notCalled:OrthoAlgorithm) ";
      String recordQuery = query + "return distinct g, gh, collect(distinct ortho)  ";

      System.out.println(recordQuery);
      Result result = queryForResult(recordQuery);
      Set<OrthologView> orthologViews = new LinkedHashSet<>();
      result.forEach(objectMap -> {
          OrthologView view = new OrthologView();
          Gene gene = (Gene) objectMap.get("g");
          //gene.setSpeciesName(SpeciesType.fromTaxonId(taxonOne).getName());
          view.setGene(gene);
          
          Gene homologGene = (Gene) objectMap.get("gh");
          Orthologous o =(Orthologous)objectMap.get("orth"); 
          if (o !=null) {
               System.out.println("ORTHOLOGOUS bst" + o.getIsBestScore()) ;  
         }
          //view.setHomologGene(homologGene);
          //view.setBest(((List<Orthologous>) objectMap.get("collect(distinct ortho)")).get(0).getIsBestScore());
          //view.setBestReverse(((List<Orthologous>) objectMap.get("collect(distinct ortho)")).get(0).getIsBestRevScore());
          if (gene !=null) {
              log.info("in log:" + gene.getName());
              System.out.println("in println:" + gene.getName());
          }
          orthologViews.add(view);
      });
      
  }
  
 static Result loggedQuery(String cypherQuery, Map<String, ?> params) {
     Session neo4jSession = Neo4jSessionFactory.getInstance().getNeo4jSession();
      Date start = new Date();
      //log.debug("Running Query: " + cypherQuery);
      Result ret = neo4jSession.query(cypherQuery, params);
      Date end = new Date();
      //log.debug("Query took: " + ProcessDisplayHelper.getHumanReadableTimeDisplay(end.getTime() - start.getTime()) + " to run");
      return ret;
  }
  
  
 static Result queryForResult(String cypherQuery) {
      return loggedQuery(cypherQuery, Collections.EMPTY_MAP);
  }
}
