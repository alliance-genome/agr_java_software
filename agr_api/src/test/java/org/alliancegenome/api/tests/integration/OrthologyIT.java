package org.alliancegenome.api.tests.integration;

import io.swagger.annotations.Api;
import lombok.extern.log4j.Log4j2;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;


@Api(value = "Orthology Tests")
@Log4j2
public class OrthologyIT {

    public static GeneRepository repo = new GeneRepository();

    public static void main(String[] args) {
    }

    @Test
    @Ignore
    public void getAllOrthologyGeneJoin() {

        MultiKeyMap map = repo.getAllOrthologyGeneJoin();

        assertNotNull(map);
    }
}
