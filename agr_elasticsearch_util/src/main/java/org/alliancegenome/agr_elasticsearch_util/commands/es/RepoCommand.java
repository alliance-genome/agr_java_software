package org.alliancegenome.agr_elasticsearch_util.commands.es;

import java.util.*;

import org.alliancegenome.agr_elasticsearch_util.commands.*;
import org.elasticsearch.cluster.metadata.RepositoryMetaData;

public class RepoCommand extends Command implements CommandInterface {

    public RepoCommand(ArrayList<String> args) {
        super(args);
    }

    @Override
    public void printHelp() {
        System.out.println("repo list -- List current repos");
        System.out.println("repo create <repoName> -- Creates a repo current repoName's are stage, prod, data, test");
    }

    @Override
    public void execute() {
        if(args.size() > 0) {
            String command = args.remove(0);

            if(command.equals("create")) {
                if(args.size() > 0) {
                    im.getCreateRepo(args.remove(0));
                } else {
                    printHelp();
                }
            } else if(command.equals("list")) {
                List<RepositoryMetaData> meta = im.listRepos();
                if(meta != null) {
                    for(RepositoryMetaData data: meta) {
                        System.out.println("Name: " + data.name() + " Type: " + data.type());
                    }
                } else {
                    System.out.println("No Repos");
                }
            } else {
                printHelp();
            }
        } else {
            printHelp();
        }

    }

}
