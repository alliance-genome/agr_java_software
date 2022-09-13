package org.alliancegenome.neo4j.view;

import com.fasterxml.jackson.databind.module.SimpleModule;

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

