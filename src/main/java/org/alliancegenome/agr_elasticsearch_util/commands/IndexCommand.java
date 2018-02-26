package org.alliancegenome.agr_elasticsearch_util.commands;

import java.util.ArrayList;

public class IndexCommand extends Command implements CommandInterface {

	public IndexCommand(ArrayList<String> args) {
		super(args);
	}

	@Override
	public void printHelp() {
		// TODO Auto-generated method stub

	}

	@Override
	public void execute() {
		
		String command = args.remove(0);
		
		if(command.equals("start")) {
			// check tmp index and delete
			// create new and alias it to tmp
		} else if(command.equals("end")) {
			// remove site_index alias
			// alias site_index to tmp index
			// remove tmp alias
			// clean up unalias indexes with site_index_suffix_{data} pattern
		} else {
			printHelp();
		}

	}

}
