package org.alliancegenome.agr_elasticsearch_util.commands;

import java.util.ArrayList;

import org.alliancegenome.shared.es.util.IndexManager;

public class SnapShotCommand extends Command implements CommandInterface {

	public SnapShotCommand(ArrayList<String> args) {
		super(args);
	}

	@Override
	public void printHelp() {
		
		
		
	}

	@Override
	public void execute() {
		
		String command = args.remove(0);
		
		if(command.equals("list")) {
			im.listSnapShots();
		} else if(command.equals("")) {

		} else {
			printHelp();
		}
		
	}

}
