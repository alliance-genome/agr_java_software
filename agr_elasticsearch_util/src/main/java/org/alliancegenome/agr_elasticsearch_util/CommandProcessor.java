package org.alliancegenome.agr_elasticsearch_util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.alliancegenome.agr_elasticsearch_util.commands.Command;
import org.alliancegenome.agr_elasticsearch_util.commands.CommandInterface;
import org.alliancegenome.agr_elasticsearch_util.commands.CommandType;

public class CommandProcessor {

    public CommandProcessor(String[] args) throws Exception {
        if(args.length > 0) {
            List<String> argList = new ArrayList<String>(Arrays.asList(args));
            processArgs(argList);
            Command.close();
        } else {
            Scanner scanner = new Scanner(System.in);
            System.out.print("> ");
            while(scanner.hasNextLine()) {
                String line = scanner.nextLine();

                List<String> argList = new ArrayList<String>(Arrays.asList(line.split(" ")));
                processArgs(argList);
                System.out.print("> ");
            }
            scanner.close();
        }
    }

    private void processArgs(List<String> argList) throws Exception {
        String initialCommand = argList.remove(0);
        try {
            CommandType commandType = CommandType.valueOf(initialCommand);
            CommandInterface command = (CommandInterface)commandType.getImplClass().getDeclaredConstructor(ArrayList.class).newInstance(argList);
            command.execute();
        } catch (IllegalArgumentException e) {
            for(CommandType ct: CommandType.values()) {
                CommandInterface command = (CommandInterface)ct.getImplClass().getDeclaredConstructor(ArrayList.class).newInstance(argList);
                command.printHelp();
            }
        }

    }
}
