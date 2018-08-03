package org.alliancegenome.neo4j.view;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.alliancegenome.es.index.site.doclet.OrthologyDoclet;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.relationship.Orthologous;

@SuppressWarnings("serial")
public class OrthologyModule extends SimpleModule
{
    public OrthologyModule() {
        super("OrthologyModule");
    }

/*
    @Override
    public void setupModule(SetupContext context)
    {
        context.setMixInAnnotations(OrthologyDoclet.class, View.OrthologyView.class);
    }
*/
}

