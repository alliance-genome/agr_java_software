package org.alliancegenome.agr_elasticsearch_util.commands.index;

import java.util.ArrayList;

import org.alliancegenome.agr_elasticsearch_util.commands.*;

public class AliasCommand extends Command implements CommandInterface {

    public AliasCommand(ArrayList<String> args) {
        super(args);
    }

    @Override
    public void printHelp() {
        
    }

    @Override
    public void execute() {
        if(args.size() > 0) {
            String command = args.remove(0);
            if(command.equals("create")) {
                String alias = args.remove(0);
                String index = args.remove(0);
                im.createAlias(alias, index);
            } else if(command.equals("remove")) {
                String alias = args.remove(0);
                //im.removeAlias(alias);
            } else {
                printHelp();
            }
        } else {
            printHelp();
        }
    }
}
