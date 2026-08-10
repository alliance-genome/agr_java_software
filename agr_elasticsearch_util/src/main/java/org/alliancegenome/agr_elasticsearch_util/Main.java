package org.alliancegenome.agr_elasticsearch_util;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.exceptional.client.ExceptionCatcher;

public class Main {

	private Main() {
	}

	public static void main(String[] args) {
		try {
			ConfigHelper.init();
			ExceptionCatcher.initialize();
			new CommandProcessor(args);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

}
