package org.alliancegenome.agr_elasticsearch_util.commands;

public interface CommandInterface {

	void printHelp();

	void execute() throws Exception;

}
