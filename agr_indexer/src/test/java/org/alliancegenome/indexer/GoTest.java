package org.alliancegenome.indexer;

import org.alliancegenome.neo4j.entity.node.GOTerm;
import org.alliancegenome.neo4j.repository.GoRepository;

public class GoTest {

    public static void main(String[] args) {
        GoRepository repo = new GoRepository();

        GOTerm term1 = repo.getOneGoTerm("GO:0019901");
        GOTerm term2 = repo.getOneGoTerm("GO:0019903");
        GOTerm term3 = repo.getOneGoTerm("GO:0031625");
        GOTerm term4 = repo.getOneGoTerm("GO:0031623");


    }

}
