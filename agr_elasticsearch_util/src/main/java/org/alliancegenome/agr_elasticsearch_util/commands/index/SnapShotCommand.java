package org.alliancegenome.agr_elasticsearch_util.commands.index;

import java.util.*;

import org.alliancegenome.agr_elasticsearch_util.commands.*;
import org.elasticsearch.cluster.metadata.RepositoryMetadata;

public class SnapShotCommand extends Command implements CommandInterface {

    public SnapShotCommand(ArrayList<String> args) {
        super(args);
    }

    @Override
    public void printHelp() {

        System.out.println("snapshot list <reponame> -- Where <reponame> is the name of a loaded repository");
        System.out.println("snapshot restorelatest <reponame> <index>");
        System.out.println("snapshot delete <reponame> <snapshot>");
        System.out.println("snapshot clean <reponame> <snapshot>");
    }

    @Override
    public void execute() {

        if(args.size() > 0) {
            String command = args.remove(0);

            if(command.equals("list")) {
                if(args.size() > 0) {
                    String repo = args.remove(0);
                    im.listRepo(repo);
                } else {
                    printHelp();
                }
            } else if(command.equals("restorelatest")) {
                if(args.size() > 1) {
                    String repo = args.remove(0);
                    String index = args.remove(0);
                    im.restoreSnapShot(repo, index);
                } else {
                    printHelp();
                }
            } else if(command.equals("clean")) {
                if(args.size() > 1) {
                    String repo = args.remove(0);
                    String snapShotName = args.remove(0);
                    im.cleanSnapShots(repo, snapShotName);
                } else {
                    printHelp();
                }
            } else if(command.equals("delete")) {
                if(args.size() > 1) {
                    String repo = args.remove(0);
                    String snapShotName = args.remove(0);
                    im.deleteSnapShot(repo, snapShotName);
                } else {
                    printHelp();
                }
            } else if(command.equals("logstash")) {
                boolean logsFound = false;
                for(RepositoryMetadata repo: im.listRepos()) {
                    if(repo.name().equals("logs")) {
                        logsFound = true;
                        break;
                    }
                }
                if(logsFound) {
                    //List<SnapshotInfo> list = im.getSnapshots("logs");
                    TreeMap<String, String> indices = new TreeMap<>();
                    for(String index: im.getIndexList()) {
                        if(index.startsWith("logstash")) {
                            indices.put(index, index);
                        }
                    }
                    String first = indices.firstKey().replace("logstash-", "");
                    String last = indices.lastKey().replace("logstash-", "");
                    String snapshotName = "logstash-" + first + "-" + last;
                    indices.remove(indices.lastKey());
                    if(indices.size() > 0) {
                        im.createSnapShot("logs", snapshotName, new ArrayList<String>(indices.keySet()));
                        im.deleteIndices(new ArrayList<String>(indices.keySet()));
                    } else {
                        System.out.println("No logstash to backup (needs to be 2 or more)");
                    }
                } else {
                    System.out.println("Please `repo create logs` first before doing a backup");
                }
            } else if(command.equals("create")) {
                if(args.size() > 2) {
                    String repo = args.remove(0);
                    String snapShotName = args.remove(0);
                    String index_name = args.remove(0);
                    im.createSnapShot(repo, snapShotName, index_name);
                } else {
                    printHelp();
                }
            } else {
                printHelp();
            }
        } else {
            printHelp();
        }

    }

}
