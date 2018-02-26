package org.alliancegenome.agr_elasticsearch_util;

import org.alliancegenome.shared.config.ConfigHelper;

public class Main {

	public static void main(String[] args) throws Exception {
		ConfigHelper.init();
		new CommandProcessor(args);
	}

}
