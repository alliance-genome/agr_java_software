package org.alliancegenome.agr_elasticsearch_util.commands;

import java.util.ArrayList;

public class RepoCommand extends Command implements CommandInterface {
	
	public RepoCommand(ArrayList<String> args) {
		super(args);
	}

	@Override
	public void printHelp() {
		
		
	}

	@Override
	public void execute() {
		String command = args.remove(0);
		
		if(command.equals("create")) {
			im.getCreateRepo(args.remove(0));
		} else if(command.equals("list")) {
			im.listRepos();
		} else {
			printHelp();
		}
		
	}

}
