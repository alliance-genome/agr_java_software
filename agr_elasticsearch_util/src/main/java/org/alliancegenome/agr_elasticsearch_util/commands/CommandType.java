package org.alliancegenome.agr_elasticsearch_util.commands;

public enum CommandType {
    
    repo(RepoCommand.class),
    quit(QuitCommand.class),
    snapshot(SnapShotCommand.class),
    alias(AliasCommand.class),
    index(IndexCommand.class),
    config(ConfigCommand.class),
    dataindex(DataIndexCommand.class),
    ;

    private Class<?> implClass;

    public Class<?> getImplClass() {
        return implClass;
    }
    
    private CommandType(Class<?> implClass) {
        this.implClass = implClass;
    }

}
