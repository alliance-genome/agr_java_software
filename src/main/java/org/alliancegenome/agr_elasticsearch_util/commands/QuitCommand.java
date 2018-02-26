package org.alliancegenome.agr_elasticsearch_util.commands;

import java.util.ArrayList;

public class QuitCommand extends Command implements CommandInterface {

	public QuitCommand(ArrayList<String> args) {
		super(args);
	}

	@Override
	public void printHelp() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void execute() {
		System.exit(0);
	}

}
