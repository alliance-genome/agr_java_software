package org.alliancegenome.shared;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import org.alliancegenome.neo4j.entity.SpeciesType;
import org.junit.Test;

public class SpeciesTypeTest {

    @Test
    public void testParameters(){
        SpeciesType zfin = SpeciesType.ZEBRAFISH;
        assertThat(zfin.getDisplayName(), equalTo("ZFIN"));
        assertThat(SpeciesType.MOUSE.getModName(), equalTo("MGD"));
        assertThat(SpeciesType.HUMAN.getName(), equalTo("Homo sapiens"));
        assertThat(SpeciesType.HUMAN.getTaxonID(), equalTo("NCBITaxon:9606"));
        assertThat(SpeciesType.HUMAN.getTaxonIDPart(), equalTo("9606"));
        assertThat(SpeciesType.RAT.getAbbreviation(), equalTo("Rno"));
        assertThat(SpeciesType.FLY.getDatabaseName(), equalTo("Fly Base"));
    }
}
