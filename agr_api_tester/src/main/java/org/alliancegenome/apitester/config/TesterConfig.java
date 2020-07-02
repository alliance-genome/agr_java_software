package org.alliancegenome.apitester.config;

import org.alliancegenome.apitester.testers.GeneTester;

public enum TesterConfig {

    GeneTester(GeneTester.class),
    ;

    private String testerName;
    private Class<?> testerClass;

    TesterConfig(Class<?> testerClazz) {
        this.testerName = testerClazz.getSimpleName();
        this.testerClass = testerClazz;
    }

    public String getTesterName() {
        return testerName;
    }

    public Class<?> getTesterClass() {
        return testerClass;
    }

}
