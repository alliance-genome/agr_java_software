package org.alliancegenome.indexer;

import org.alliancegenome.indexer.config.ConfigHelper;
import org.alliancegenome.indexer.util.IndexManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MainIndexAdmin {
    private static Logger log = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        ConfigHelper.init();

        IndexManager im = new IndexManager();

        // Be very careful running this
        //im.checkSnapShotRepo("dev");

    }

}
