package org.alliancegenome.agr_elasticsearch_util.commands.es;

import java.util.ArrayList;
import java.util.List;

import org.alliancegenome.agr_elasticsearch_util.commands.Command;
import org.alliancegenome.agr_elasticsearch_util.commands.CommandInterface;
import org.elasticsearch.cluster.metadata.RepositoryMetadata;

public class RepoCommand extends Command implements CommandInterface {

	public RepoCommand(ArrayList<String> args) {
		super(args);
	}

	@Override
	public void printHelp() {
		System.out.println("repo list -- List current repos");
		System.out.println("repo create <repoName> <type> -- Creates a repo. <repoName> is the local repo registration name; <type> is 'site' or 'variant' and drives base_path");
	}

	@Override
	public void execute() {
		if (args.size() > 0) {
			String command = args.remove(0);

			if (command.equals("create")) {
				if (args.size() > 1) {
					String name = args.remove(0);
					String type = args.remove(0);
					im.setBasePath(type);
					im.getCreateRepo(name);
				} else {
					printHelp();
				}
			} else if (command.equals("delete")) {
				if (args.size() > 0) {
					im.deleteRepo(args.remove(0));
				} else {
					printHelp();
				}
			} else if (command.equals("list")) {
				List<RepositoryMetadata> meta = im.listRepos();
				if (meta != null) {
					for (RepositoryMetadata data : meta) {
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
