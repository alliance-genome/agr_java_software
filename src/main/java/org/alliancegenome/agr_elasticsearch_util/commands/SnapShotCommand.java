package org.alliancegenome.agr_elasticsearch_util.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.elasticsearch.cluster.metadata.RepositoryMetaData;
import org.elasticsearch.snapshots.SnapshotInfo;

public class SnapShotCommand extends Command implements CommandInterface {

	public SnapShotCommand(ArrayList<String> args) {
		super(args);
	}

	@Override
	public void printHelp() {

		System.out.println("snapshot list <reponame> -- Where <reponame> is the name of a loaded repository");
		System.out.println("snapshot restore <reponame> <snapshot> <index>");
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
						System.out.print(info.snapshotId() + "[");
						String delim = "";
						for(String index: info.indices()) {
							System.out.print(delim + index);
							delim = ",";
						}
						System.out.println("]");
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
					String snapshotName = args.remove(0);
					String index_name = args.remove(0);
					
					List<String> list = new ArrayList<String>();
					list.add(index_name);
					im.restoreSnapShot(repo, snapshotName, new ArrayList<String>(list));
					im.removeAlias("site_index", index_name);
					im.updateIndexSetting(index_name, "index.number_of_replicas", 2);
				} else {
					printHelp();
				}
			} else if(command.equals("logstash")) {
				boolean logsFound = false;
				for(RepositoryMetaData repo: im.listRepos()) {
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
			} else {
				printHelp();
			}
		} else {
			printHelp();
		}

	}

}
