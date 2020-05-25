package org.alliancegenome.agr_elasticsearch_util.commands.index;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.alliancegenome.agr_elasticsearch_util.commands.Command;
import org.alliancegenome.agr_elasticsearch_util.commands.CommandInterface;
import org.elasticsearch.cluster.metadata.AliasMetaData;
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
                if(args.size() > 0) {
                    String index = args.remove(0);
//                    Set<AliasMetaData> aliases = im.getIndex(index);
                    List<String> aliases = im.getAliasesForIndex(index);
                    if(aliases != null) {
                        for (String alias : aliases) {
                            System.out.println("Alias: " + alias);
                        }
                        System.out.println(index);
                    } else {
                        System.out.println("Index not found: " + index);
                    }
                } else {
                    printHelp();
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
