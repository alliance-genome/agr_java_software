package org.alliancegenome.core.variant.config;

import static org.alliancegenome.core.config.Constants.VARIANTS_TO_INDEX;
import static org.alliancegenome.core.config.Constants.VARIANT_BULK_PROCESSOR_SETTINGS;
import static org.alliancegenome.core.config.Constants.VARIANT_CACHER_CONFIG_FILE;
import static org.alliancegenome.core.config.Constants.VARIANT_CONFIG_CREATING;
import static org.alliancegenome.core.config.Constants.VARIANT_CONFIG_DOWNLOAD;
import static org.alliancegenome.core.config.Constants.VARIANT_CONFIG_GATHERSTATS;
import static org.alliancegenome.core.config.Constants.VARIANT_CONFIG_INDEXING;
import static org.alliancegenome.core.config.Constants.VARIANT_DISPLAY_INTERVAL;
import static org.alliancegenome.core.config.Constants.VARIANT_FILE_DOWNLOAD_FILTER_THREADS;
import static org.alliancegenome.core.config.Constants.VARIANT_FILE_DOWNLOAD_PATH;
import static org.alliancegenome.core.config.Constants.VARIANT_FILE_DOWNLOAD_THREADS;
import static org.alliancegenome.core.config.Constants.VARIANT_HUMAN_DOWNLOAD_SET_FILE;
import static org.alliancegenome.core.config.Constants.VARIANT_INDEXER_BULK_PROCESSOR_THREADS;
import static org.alliancegenome.core.config.Constants.VARIANT_INDEXER_SHARDS;
import static org.alliancegenome.core.config.Constants.VARIANT_MOD_DOWNLOAD_SET_FILE;
import static org.alliancegenome.core.config.Constants.VARIANT_PRODUCER_THREADS;
import static org.alliancegenome.core.config.Constants.VARIANT_SOURCE_DOCUMENT_CREATOR_OBJECT_QUEUE_BUCKET_SIZE;
import static org.alliancegenome.core.config.Constants.VARIANT_SOURCE_DOCUMENT_CREATOR_OBJECT_QUEUE_SIZE;
import static org.alliancegenome.core.config.Constants.VARIANT_SOURCE_DOCUMENT_CREATOR_THREADS;
import static org.alliancegenome.core.config.Constants.VARIANT_SOURCE_DOCUMENT_CREATOR_VCQUEUE_BUCKET_SIZE;
import static org.alliancegenome.core.config.Constants.VARIANT_SOURCE_DOCUMENT_CREATOR_VCQUEUE_SIZE;
import static org.alliancegenome.core.config.Constants.VARIANT_TRANSFORMER_THREADS;

import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

import org.alliancegenome.core.util.FileHelper;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class VariantConfigHelper {

	private static Properties configProperties = new Properties();

	private static HashMap<String, String> defaults = new HashMap<>();
	private static HashMap<String, String> config = new HashMap<>();
	private static Set<String> allKeys;
	private static boolean init = false;

	public VariantConfigHelper() {
		init();
	}

	public static void init() {
		/* The purpose of the default values is that these are the values required by the application to run
		 * This config help looks at 3 methods to over write these values.
		 * 1. Via System Properties with are passed to the application at run time via the -DNAME=value
		 * 2. Via a config.properties file, this file only need to over write the values it wants to change from the defaults
		 * otherwise the config will be loaded from the default values.
		 * 3. Via the environment. Before the application is run export NAME=value in the shell the config helper will load the
		 * NAME and value that it finds in the environments and use those values rather then the defaults.
		 * The names and values need to not be changed as other things might depend on the defaulted values for executing. For each
		 * key in the defaults map there should be a corresponding get method for that value.
		 */


		defaults.put(VARIANT_HUMAN_DOWNLOAD_SET_FILE, "HumanFileSet.yaml");
		defaults.put(VARIANT_MOD_DOWNLOAD_SET_FILE, "ModFileSet.yaml");
		
		defaults.put(VARIANT_CACHER_CONFIG_FILE, "variantDownloadFiles.yaml");

		defaults.put(VARIANTS_TO_INDEX, "");
		
		defaults.put(VARIANT_CONFIG_DOWNLOAD, "true");
		defaults.put(VARIANT_CONFIG_CREATING, "true");
		defaults.put(VARIANT_CONFIG_INDEXING, "true");
		defaults.put(VARIANT_CONFIG_GATHERSTATS, "false");
		
		defaults.put(VARIANT_FILE_DOWNLOAD_THREADS, "10");
		defaults.put(VARIANT_FILE_DOWNLOAD_FILTER_THREADS, "10");
		defaults.put(VARIANT_FILE_DOWNLOAD_PATH, "data");
		
		defaults.put(VARIANT_DISPLAY_INTERVAL, "30");
		
		defaults.put(VARIANT_SOURCE_DOCUMENT_CREATOR_THREADS, "6");
		
		defaults.put(VARIANT_SOURCE_DOCUMENT_CREATOR_VCQUEUE_SIZE, "400");
		defaults.put(VARIANT_SOURCE_DOCUMENT_CREATOR_VCQUEUE_BUCKET_SIZE, "25");
		defaults.put(VARIANT_SOURCE_DOCUMENT_CREATOR_OBJECT_QUEUE_SIZE, "125");
		defaults.put(VARIANT_SOURCE_DOCUMENT_CREATOR_OBJECT_QUEUE_BUCKET_SIZE, "120");
		
		
		defaults.put(VARIANT_PRODUCER_THREADS, "18");
		defaults.put(VARIANT_TRANSFORMER_THREADS, "12");
		
		defaults.put(VARIANT_INDEXER_SHARDS, "16");
		defaults.put(VARIANT_INDEXER_BULK_PROCESSOR_THREADS, "8");
		
		// BulkActions;ConcurrentRequests;BulkSize;JsonQueueSize;QueueCutOffSize
		defaults.put(VARIANT_BULK_PROCESSOR_SETTINGS, "1000;10;10;10000;1000,133;10;10;1333;7500,100;10;10;1000;10000,50;10;10;500;10000000");

		allKeys = defaults.keySet();

		if (configProperties.size() == 0) {
			final String configPropertiesFileName = "config.properties";
			configProperties = FileHelper.getPropertiesFromFile(configPropertiesFileName);
		}

		for (String key : allKeys) {
			// First checks the -D params and sets config[key] = value otherwise it will be null.
			if (config.get(key) == null) config.put(key, loadSystemProperty(key));
			// Second checks the config.properties file built into the application otherwise it will be null.
			if (config.get(key) == null) config.put(key, loadConfigProperty(key));
			// Third checks the environment for a NAME = value otherwise leaves it null.
			if (config.get(key) == null) config.put(key, loadSystemENVProperty(key));
			// Lastly loads the default value for NAME = value and loadDefaultProperty ensures it won't be null.
			if (config.get(key) == null) config.put(key, loadDefaultProperty(key));
		}
		printProperties();
		init = true;
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

	public static String getVariantHumanDownloadSetFile() {
		if (!init) init();
		return config.get(VARIANT_HUMAN_DOWNLOAD_SET_FILE);
	}
	
	public static String getVariantModDownloadSetFile() {
		if (!init) init();
		return config.get(VARIANT_MOD_DOWNLOAD_SET_FILE);
	}
	
	public static String getVariantToIndex() {
		if (!init) init();
		return config.get(VARIANTS_TO_INDEX);
	}

	public static String getDownloadSetFile() {
		if (!init) init();
		if(getVariantToIndex().equals("HUMAN")) {
			return getVariantHumanDownloadSetFile();
		}
		if(getVariantToIndex().equals("MOD")) {
			return getVariantModDownloadSetFile();
		}
		log.warn("need to set VARIANTS_TO_INDEX = \"HUMAN\" or \"MOD\" to choose between which variants to index");
		return null;
	}
	
	public static String getVariantCacherConfigFile() {
		if (!init) init();
		return config.get(VARIANT_CACHER_CONFIG_FILE);
	}

	public static int[][] getBulkProcessorSettingsArray() {
		String settings = getBulkProcessorSettings();
		String[] array = settings.split(",");
		int[][] ret = new int[array.length][];
		
		for(int i = 0; i < array.length; i++) {
			String[] array2 = array[i].split(";");
			ret[i] = new int[array2.length];
			for(int k = 0; k < array2.length; k++) {
				ret[i][k] = Integer.parseInt(array2[k]);
				log.debug("ret[" + i + "][" + k + "]=" + ret[i][k]);
			}
		}
		
		return ret;
	}
	
	public static String getBulkProcessorSettings() {
		if (!init) init();
		return config.get(VARIANT_BULK_PROCESSOR_SETTINGS);
	}

	public static String getVariantFileDownloadPath() {
		if (!init) init();
		return config.get(VARIANT_FILE_DOWNLOAD_PATH);
	}

	public static void printProperties() {
		log.info("Running with Properties:");
		for (String key : allKeys) {
			log.info("\t" + key + ": " + config.get(key));
		}
	}

	public static Boolean isDownloading() {
		if (!init) init();
		return Boolean.parseBoolean(config.get(VARIANT_CONFIG_DOWNLOAD));
	}
	public static Boolean isCreating() {
		if (!init) init();
		return Boolean.parseBoolean(config.get(VARIANT_CONFIG_CREATING));
	}
	public static Boolean isIndexing() {
		if (!init) init();
		return Boolean.parseBoolean(config.get(VARIANT_CONFIG_INDEXING));
	}
	public static Boolean isGatherStats() {
		if (!init) init();
		return Boolean.parseBoolean(config.get(VARIANT_CONFIG_GATHERSTATS));
	}

	public static Integer getIndexerShards() {
		if (!init) init();
		try {
			return Integer.parseInt(config.get(VARIANT_INDEXER_SHARDS));
		} catch (NumberFormatException e) {
			return 4;
		}
	}

	public static Integer getFileDownloadThreads() {
		if (!init) init();
		try {
			return Integer.parseInt(config.get(VARIANT_FILE_DOWNLOAD_THREADS));
		} catch (NumberFormatException e) {
			return 10;
		}
	}
	
	public static int getFileDownloadFilterThreads() {
		if (!init) init();
		try {
			return Integer.parseInt(config.get(VARIANT_FILE_DOWNLOAD_FILTER_THREADS));
		} catch (NumberFormatException e) {
			return 10;
		}
	}

	public static Integer getSourceDocumentCreatorThreads() {
		if (!init) init();
		try {
			return Integer.parseInt(config.get(VARIANT_SOURCE_DOCUMENT_CREATOR_THREADS));
		} catch (NumberFormatException e) {
			return 4;
		}
	}
	public static Integer getDisplayInterval() {
		if (!init) init();
		try {
			return Integer.parseInt(config.get(VARIANT_DISPLAY_INTERVAL)) * 1000;
		} catch (NumberFormatException e) {
			return 4000;
		}
	}
	public static Integer getSourceDocumentCreatorVCQueueSize() {
		if (!init) init();
		try {
			return Integer.parseInt(config.get(VARIANT_SOURCE_DOCUMENT_CREATOR_VCQUEUE_SIZE));
		} catch (NumberFormatException e) {
			return 100;
		}
	}
	
	public static int getSourceDocumentCreatorVCQueueBucketSize() {
		if (!init) init();
		try {
			return Integer.parseInt(config.get(VARIANT_SOURCE_DOCUMENT_CREATOR_VCQUEUE_BUCKET_SIZE));
		} catch (NumberFormatException e) {
			return 100;
		}
	}
	
	public static Integer getSourceDocumentCreatorObjectQueueSize() {
		if (!init) init();
		try {
			return Integer.parseInt(config.get(VARIANT_SOURCE_DOCUMENT_CREATOR_OBJECT_QUEUE_SIZE));
		} catch (NumberFormatException e) {
			return 100;
		}
	}
	
	public static int getSourceDocumentCreatorObjectQueueBucketSize() {
		if (!init) init();
		try {
			return Integer.parseInt(config.get(VARIANT_SOURCE_DOCUMENT_CREATOR_OBJECT_QUEUE_BUCKET_SIZE));
		} catch (NumberFormatException e) {
			return 100;
		}
	}
	
	public static int getIndexerBulkProcessorThreads() {
		if (!init) init();
		try {
			return Integer.parseInt(config.get(VARIANT_INDEXER_BULK_PROCESSOR_THREADS));
		} catch (NumberFormatException e) {
			return 16;
		}
	}
	
	public static int getTransformerThreads() {
		if (!init) init();
		try {
			return Integer.parseInt(config.get(VARIANT_TRANSFORMER_THREADS));
		} catch (NumberFormatException e) {
			return 4;
		}
	}
	
	public static int getProducerThreads() {
		if (!init) init();
		try {
			return Integer.parseInt(config.get(VARIANT_PRODUCER_THREADS));
		} catch (NumberFormatException e) {
			return 4;
		}
	}

}
