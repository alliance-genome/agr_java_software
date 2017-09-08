package org.alliancegenome.indexer.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConfigHelper {

	private static Logger log = LogManager.getLogger(ConfigHelper.class);
	private static Properties configProperties = new Properties();

	private static HashMap<String, String> defaults = new HashMap<>();
	private static HashMap<String, String> config = new HashMap<>();

	public ConfigHelper() {
		init();
	}

	public static void init() {

		// DO NOT MODIFY THESE VALUES
		// USE -DNAME=value on the command line
		// or export NAME=value before running.
		defaults.put("THREADED", "false");
		defaults.put("ES_HOST", "localhost");
		defaults.put("ES_PORT", "9300");
		defaults.put("ES_INDEX", "site_index");
		defaults.put("NEO4J_HOST", "localhost");
		defaults.put("NEO4J_PORT", "7474");

		if (configProperties.size() == 0) {
			InputStream in = ConfigHelper.class.getClassLoader().getResourceAsStream("config.properties");
			if (in == null) {
				log.debug("No config.properties file, other config options will be used");
			} else {
				try {
					configProperties.load(in);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		for (String key : defaults.keySet()) {
			if (config.get(key) == null) config.put(key, loadSystemProperty(key));
			if (config.get(key) == null) config.put(key, loadConfigProperty(key));
			if (config.get(key) == null) config.put(key, loadSystemENVProperty(key));
			if (config.get(key) == null) config.put(key, loadDefaultProperty(key));
		}
		printProperties();
	}

	private static String loadSystemProperty(String key) {
		String ret = System.getProperty(key);
		if (ret != null) log.debug("Found: -D " + key + "=" + ret);
		return ret;
	}

	private static String loadConfigProperty(String key) {
		String ret = configProperties.getProperty(key);
		if (ret != null) log.debug("Config File Property: " + key + "=" + ret);
		return ret;
	}

	private static String loadSystemENVProperty(String key) {
		String ret = System.getenv(key);
		if (ret != null) log.debug("Found Enviroment ENV[" + key + "]=" + ret);
		return ret;
	}

	private static String loadDefaultProperty(String key) {
		String ret = defaults.get(key);
		if (ret != null) log.debug("Setting default: " + key + "=" + ret);
		return ret;
	}

	public static String getEsHost() {
		return config.get("ES_HOST");
	}

	public static int getEsPort() {
		try {
			return Integer.parseInt(config.get("ES_PORT"));
		} catch (NumberFormatException e) {
			return 9300;
		}
	}

	public static String getNeo4jHost() {
		return config.get("NEO4J_HOST");
	}

	public static int getNeo4jPort() {
		try {
			return Integer.parseInt(config.get("NEO4J_PORT"));
		} catch (NumberFormatException e) {
			return 7687;
		}
	}

	public static void printProperties() {
		log.info("Running with Properties:");
		for (String key : defaults.keySet()) {
			log.info("\t" + key + ": " + config.get(key));
		}
	}

	public static boolean isThreaded() {
		return Boolean.parseBoolean(config.get("THREADED"));
	}

	public static String getEsIndex() {
		return config.get("ES_INDEX");
	}


}
