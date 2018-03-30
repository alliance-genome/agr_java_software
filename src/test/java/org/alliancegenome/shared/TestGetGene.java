package org.alliancegenome.shared;

import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.index.site.dao.GeneDAO;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Before;
import org.junit.Test;

import jdk.nashorn.internal.ir.annotations.Ignore;

public class TestGetGene {

    private GeneDAO geneService;

    public void before() {
        Configurator.setRootLevel(Level.WARN);
        ConfigHelper.init();
        geneService = new GeneDAO();
    }

    public static void main(String[] args) {
        GeneRepository repo = new GeneRepository();

        //"MGI:97490" OR g.primaryKey = "RGD:3258"

        System.out.println("MGI:97490");
        HashMap<String, Gene> geneMap = repo.getGene("RGD:3258");
        System.out.println(geneMap);

    }

    public void checkSecondaryId() {
        Map<String, Object> result = geneService.getGeneBySecondary("ZFIN:ZDB-GENE-030131-3355");
        assertNotNull(result);
    }
}
