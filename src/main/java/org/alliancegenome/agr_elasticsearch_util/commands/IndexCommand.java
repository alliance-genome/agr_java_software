package org.alliancegenome.agr_elasticsearch_util.commands;

import java.util.ArrayList;
import java.util.List;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.index.data.dao.DataTypeDAO;
import org.alliancegenome.es.index.data.dao.MetaDataDAO;
import org.alliancegenome.es.index.data.dao.TaxonIdDAO;
import org.elasticsearch.cluster.metadata.IndexMetaData;

import com.carrotsearch.hppc.cursors.ObjectCursor;

public class IndexCommand extends Command implements CommandInterface {

	public IndexCommand(ArrayList<String> args) {
		super(args);
	}

	@Override
	public void printHelp() {
		
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
			} else if(command.equals("switchalias")) {
				if(args.size() > 2) {
					String alias = args.remove(0);
					String old_index = args.remove(0);
					String new_index = args.remove(0);
					im.createAlias(alias, new_index);
					im.removeAlias(alias, old_index);
				} else {
					printHelp();
				}
			} else if(command.equals("check")) {
				if(args.size() > 0) {
					String index = args.remove(0);
					if(index.equals(ConfigHelper.getEsDataIndex())) {
						MetaDataDAO metaDataDao = new MetaDataDAO();
						DataTypeDAO dataTypeDao = new DataTypeDAO();
						TaxonIdDAO taxonIdDao = new TaxonIdDAO();
					} else {
						System.out.println("Don't know about that index: " + index);
					}
				} else {
					printHelp();
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
