package org.alliancegenome.agr_elasticsearch_util;

import org.alliancegenome.core.config.ConfigHelper;

public class Main {

    public static void main(String[] args) {
        try {
        ConfigHelper.init();
        new CommandProcessor(args);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

}
