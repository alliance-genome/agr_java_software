package org.alliancegenome.agr_elasticsearch_util.commands;

import java.util.ArrayList;

import org.alliancegenome.shared.config.ConfigHelper;

public class ConfigCommand extends Command implements CommandInterface {

	public ConfigCommand(ArrayList<String> args) {
		super(args);
	}

	@Override
	public void printHelp() {


	}

	@Override
	public void execute() {
		if(args.size() > 0) {
			String command = args.remove(0);

			if(command.equals("set")) {
				String key = args.remove(0);
				String value = args.remove(0);
				ConfigHelper.setNameValue(key, value);
				resetIndexManager();
			} else if(command.equals("print")) {
				ConfigHelper.printProperties();
			} else {
				printHelp();
			}
		} else {
			printHelp();
		}

	}

}
