package org.alliancegenome.agr_elasticsearch_util.commands.config;

import java.util.ArrayList;

import org.alliancegenome.agr_elasticsearch_util.commands.Command;
import org.alliancegenome.agr_elasticsearch_util.commands.CommandInterface;
import org.alliancegenome.core.config.ConfigHelper;

public class ConfigCommand extends Command implements CommandInterface {

    public ConfigCommand(ArrayList<String> args) {
        super(args);
    }

    @Override
    public void printHelp() {
        System.out.println("config set KEY VALUE -- Example config set DEBUG true");
        System.out.println("config print -- prints out the current running config");
        System.out.println("config load <filePath> -- loads config from filePath");
    }

    @Override
    public void execute() {
        if(args.size() > 0) {
            String command = args.remove(0);

            if(command.equals("set")) {
                if(args.size() == 2) {
                    String key = args.remove(0);
                    String value = args.remove(0);
                    ConfigHelper.setNameValue(key, value);
                    resetIndexManager();
                } else {
                    printHelp();
                }
            } else if(command.equals("print")) {
                ConfigHelper.printProperties();
            } else if(command.equals("load")) {
                //String file = args.remove(0);
                System.out.println("Not Implemented yet");
            } else {
                printHelp();
            }
        } else {
            printHelp();
        }

    }

}
