package org.alliancegenome.agr_elasticsearch_util.commands;

import org.alliancegenome.agr_elasticsearch_util.commands.config.ConfigCommand;
import org.alliancegenome.agr_elasticsearch_util.commands.es.RepoCommand;
import org.alliancegenome.agr_elasticsearch_util.commands.index.AliasCommand;
import org.alliancegenome.agr_elasticsearch_util.commands.index.IndexCommand;
import org.alliancegenome.agr_elasticsearch_util.commands.index.SnapShotCommand;

public enum CommandType {
	
	repo(RepoCommand.class),
	quit(QuitCommand.class),
	snapshot(SnapShotCommand.class),
	alias(AliasCommand.class),
	index(IndexCommand.class),
	config(ConfigCommand.class),
	;

	private Class<?> implClass;

	public Class<?> getImplClass() {
		return implClass;
	}
	
	private CommandType(Class<?> implClass) {
		this.implClass = implClass;
	}

}
