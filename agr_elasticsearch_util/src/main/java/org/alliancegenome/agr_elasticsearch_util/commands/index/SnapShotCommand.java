package org.alliancegenome.agr_elasticsearch_util.commands.index;

import java.util.*;

import org.alliancegenome.agr_elasticsearch_util.commands.*;
import org.elasticsearch.cluster.metadata.*;
import org.elasticsearch.snapshots.SnapshotInfo;

public class SnapShotCommand extends Command implements CommandInterface {

    public SnapShotCommand(ArrayList<String> args) {
        super(args);
    }

    @Override
    public void printHelp() {

        System.out.println("snapshot list <reponame> -- Where <reponame> is the name of a loaded repository");
        System.out.println("snapshot restore <reponame> <snapshot> <index>");
        System.out.println("snapshot restorelatest <reponame>");
        System.out.println("snapshot delete <reponame> <snapshot>");
    }

    @Override
    public void execute() {

        if(args.size() > 0) {
            String command = args.remove(0);

            if(command.equals("list")) {
                if(args.size() > 0) {
                    String repo = args.remove(0);
                    List<SnapshotInfo> list = im.getSnapshots(repo);
                    for(SnapshotInfo info: list) {
                        Date end = new Date(info.endTime());
                        System.out.print(info.snapshotId() + "[");
                        String delim = "";
                        for(String index: info.indices()) {
                            System.out.print(delim + index);
                            delim = ",";
                        }
                        System.out.println("] " + end);
                    }
                } else {
                    printHelp();
                }
            } else if(command.equals("restorelatest")) {
                if(args.size() > 0) {
                    String repo = args.remove(0);
                    List<String> indexes = im.getIndexList();
                    List<SnapshotInfo> list = im.getSnapshots(repo);
                    TreeMap<Date, SnapshotInfo> map = new TreeMap<>();
                    for(SnapshotInfo info: list) {
                        String[] array = info.snapshotId().getName().split("_");
                        Date d = new Date(Long.parseLong(array[array.length - 1]));
                        map.put(d, info);
                    }
                    
                    System.out.println("First Snapshot: " + map.firstKey());
                    System.out.println("Lastest Snapshot: " + map.lastKey());
                    SnapshotInfo info = map.get(map.lastKey());
                    
                    if(indexes.contains(info.snapshotId().getName())) {
                        System.out.println("Index already exists: " + info.snapshotId().getName() + " not restoring");
                    } else {
                        System.out.println("Need to restore index: " + info.snapshotId().getName());
                        String snapshot_name = info.snapshotId().getName();
                        
                        List<String> index_list = new ArrayList<String>();
                        index_list.add(snapshot_name);
                        im.restoreSnapShot(repo, snapshot_name, new ArrayList<String>(index_list));
                        try {
                            Thread.sleep(90000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        System.out.println("Restore: " + snapshot_name + " is complete");
                        System.out.println("Switching Aliases: ");
                        im.removeAlias("site_index");
                        im.createAlias("site_index", snapshot_name);
                        System.out.println("Index restore complete");
                    }

                    
                } else {
                    printHelp();
                }
            } else if(command.equals("restore")) {
                //snapshot restore repo_name snapshot_name index_name
                //snapshot restore stage site_index_stage_1522084524618 site_index_stage_1522084524618
                // remove alias site_index from snapshot_name
                // change the index setting to 2 replicas
                if(args.size() > 2) {
                    String repo = args.remove(0);
                    String snapShotName = args.remove(0);
                    String index_name = args.remove(0);

                    List<String> list = new ArrayList<String>();
                    list.add(index_name);
                    im.restoreSnapShot(repo, snapShotName, new ArrayList<String>(list));
                    //im.removeAlias("site_index", index_name);
                    //im.updateIndexSetting(index_name, "index.number_of_replicas", 2);
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
