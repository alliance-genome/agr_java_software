package org.alliancegenome.agr_elasticsearch_util.commands;

import java.util.ArrayList;

import org.alliancegenome.es.util.IndexManager;

public abstract class Command {

    protected ArrayList<String> args;
    protected static IndexManager im = new IndexManager();

    public Command(ArrayList<String> args) {
        this.args = args;
    }
    
    protected void resetIndexManager() {
        im = new IndexManager();
    }
}
