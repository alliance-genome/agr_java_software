package org.alliancegenome.agr_elasticsearch_util.commands.ec2;

import java.util.ArrayList;

import org.alliancegenome.agr_elasticsearch_util.commands.Command;
import org.alliancegenome.agr_elasticsearch_util.commands.CommandInterface;
import org.alliancegenome.aws.EC2Helper;

public class EC2Command extends Command implements CommandInterface {

    EC2Helper ec2 = new EC2Helper();

    public EC2Command(ArrayList<String> args) {
        super(args);
    }

    @Override
    public void printHelp() {

        System.out.println("EC2 Help: ");
    }

    @Override
    public void execute() {
        if(args.size() > 0) {
            String command = args.remove(0);
            if(command.equals("list")) {
                //String alias = args.remove(0);
                //String index = args.remove(0);
                ec2.listInstances();
            } else if(command.equals("create")) {
                ec2.createInstance();
            } else {
                printHelp();
            }
        } else {
            printHelp();
        }

    }

}
