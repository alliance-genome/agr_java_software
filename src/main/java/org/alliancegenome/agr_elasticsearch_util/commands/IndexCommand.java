package org.alliancegenome.agr_elasticsearch_util.commands;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.cluster.metadata.IndexMetaData;

import com.carrotsearch.hppc.cursors.ObjectCursor;
import com.carrotsearch.hppc.cursors.ObjectObjectCursor;

public class IndexCommand extends Command implements CommandInterface {

	public IndexCommand(ArrayList<String> args) {
		super(args);
	}

	@Override
	public void printHelp() {
		// TODO Auto-generated method stub

	}

	@Override
	public void execute() {

		if(args.size() > 0) {
			String command = args.remove(0);
			if(command.equals("list")) {
				List<String> list = im.getIndexList();
				for(String index: list) {
					System.out.println(index);
				}
			} else if(command.equals("info")) {
				String index = args.remove(0);
				IndexMetaData imd = im.getIndex(index);
				if(imd != null) {
					for(ObjectCursor<String> a: imd.getAliases().keys()) {
						System.out.println("Alias: " + a.value);
					}
					System.out.println(imd.getSettings().get("index.provided_name"));
				} else {
					System.out.println("Index not found: " + index);
				}
			} else if(command.equals("start")) {
				// check tmp index and delete
				// create new and alias it to tmp
			} else if(command.equals("end")) {
				// remove site_index alias
				// alias site_index to tmp index
				// remove tmp alias
				// clean up unalias indexesed with site_index_suffix_{data} pattern
			} else {
				printHelp();
			}
		} else {
			printHelp();
		}

	}

}
