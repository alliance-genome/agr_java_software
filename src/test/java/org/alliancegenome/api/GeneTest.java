package org.alliancegenome.api;

import org.alliancegenome.api.service.GeneService;
import org.alliancegenome.core.config.ConfigHelper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class GeneTest {

    private GeneService geneService;

    @Before
    public void before() {
        Configurator.setRootLevel(Level.WARN);
        ConfigHelper.init();
        geneService = new GeneService();
    }


    @Test
    @Ignore
    public void checkForSecondaryId() {
        // ZFIN:ZDB-GENE-030131-3355 is a secondary ID for ZFIN:ZDB-LINCRNAG-160518-1
        Map<String, Object> result = geneService.getById("ZFIN:ZDB-GENE-030131-3355");
        assertNotNull(result);
        assertThat(result.get("primaryId"), equalTo("ZFIN:ZDB-LINCRNAG-160518-1"));
        assertThat(result.get("species"), equalTo("Danio rerio"));
    }

}