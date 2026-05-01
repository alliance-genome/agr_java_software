package org.alliancegenome.core.config;

import static org.alliancegenome.core.config.Constants.ALLIANCE_RELEASE;
import static org.alliancegenome.core.config.Constants.AO_TERM_LIST;
import static org.alliancegenome.core.config.Constants.API_HOST;
import static org.alliancegenome.core.config.Constants.API_PORT;
import static org.alliancegenome.core.config.Constants.API_SECURE;
import static org.alliancegenome.core.config.Constants.AWS_ACCESS_KEY;
import static org.alliancegenome.core.config.Constants.AWS_BUCKET_NAME;
import static org.alliancegenome.core.config.Constants.AWS_PROFILE;
import static org.alliancegenome.core.config.Constants.AWS_SECRET_KEY;
import static org.alliancegenome.core.config.Constants.GENERATED_FILES_FOLDER;
import static org.alliancegenome.core.config.Constants.SKIP_S3_UPLOAD;
import static org.alliancegenome.core.config.Constants.BLUETEAM_ES_INDEX;
import static org.alliancegenome.core.config.Constants.BLUETEAM_ES_URL;
import static org.alliancegenome.core.config.Constants.CURATION_API_TOKEN;
import static org.alliancegenome.core.config.Constants.CURATION_API_URL;
import static org.alliancegenome.core.config.Constants.DEBUG;
import static org.alliancegenome.core.config.Constants.ES_BULK_ACTION_SIZE;
import static org.alliancegenome.core.config.Constants.ES_BULK_CONCURRENT_REQUESTS;
import static org.alliancegenome.core.config.Constants.ES_BULK_REQUEST_SIZE;
import static org.alliancegenome.core.config.Constants.ES_HOST;
import static org.alliancegenome.core.config.Constants.ES_INDEX;
import static org.alliancegenome.core.config.Constants.ES_INDEX_PREFIX;
import static org.alliancegenome.core.config.Constants.ES_INDEX_SUFFIX;
import static org.alliancegenome.core.config.Constants.ES_PORT;
import static org.alliancegenome.core.config.Constants.ES_SHARD_COUNT;
import static org.alliancegenome.core.config.Constants.EXTRACTOR_OUTPUTDIR;
import static org.alliancegenome.core.config.Constants.FMS_URL;
import static org.alliancegenome.core.config.Constants.GO_TERM_LIST;
import static org.alliancegenome.core.config.Constants.INDEX_VARIANTS;
import static org.alliancegenome.core.config.Constants.NEO4J_HOST;
import static org.alliancegenome.core.config.Constants.NEO4J_PORT;
import static org.alliancegenome.core.config.Constants.POPULARITY_DOWNLOAD_URL;
import static org.alliancegenome.core.config.Constants.POPULARITY_FILE_NAME;
import static org.alliancegenome.core.config.Constants.RIBBON_TERM_SPECIES_APPLICABILITY;
import static org.alliancegenome.core.config.Constants.THREADED;
import static org.alliancegenome.core.config.Constants.VARIANT_CACHER_CONFIG_FILE;
import static org.alliancegenome.core.config.Constants.VARIANT_DOWNLOAD_PATH;

import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

import org.alliancegenome.core.util.FileHelper;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConfigHelper {

	private ConfigHelper() { }
	
	private static Date appStart = new Date();
	private static Properties configProperties = new Properties();

	private static HashMap<String, String> defaults = new HashMap<>();
	private static HashMap<String, String> config = new HashMap<>();
	private static Set<String> allKeys;
	private static boolean init;

	{ init(); }

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
		defaults.put(THREADED, "true"); // Indexer Value

		defaults.put(DEBUG, "false");

		defaults.put(ES_INDEX, "site_index"); // Can be over written
		defaults.put(ES_INDEX_PREFIX, ""); // This can be used to prefix "{ES_INDEX_PREFIX}_{ES_INDEX}" Example mod and human variant indexer
		defaults.put(ES_INDEX_SUFFIX, ""); // This can be used to suffix "{ES_INDEX}_{ES_INDEX_SUFFIX}" Example Prod, Dev, Stage, etc
		// If both are used then the resulting index name is: {ES_INDEX_PREFIX}_{ES_INDEX}_{ES_INDEX_SUFFIX}_{TIMESTAMP}"
		defaults.put(ES_HOST, "localhost");
		defaults.put(ES_PORT, "9200");
		defaults.put(ES_SHARD_COUNT, "4");

		// ES Bulk Processing defaults
		defaults.put(ES_BULK_ACTION_SIZE, "400");
		defaults.put(ES_BULK_REQUEST_SIZE, "20");
		defaults.put(ES_BULK_CONCURRENT_REQUESTS, "1");

		defaults.put(INDEX_VARIANTS, "false");

		defaults.put(API_HOST, "localhost");
		defaults.put(CURATION_API_URL, "http://localhost:8080/api");
		defaults.put(CURATION_API_TOKEN, "");
		defaults.put(API_PORT, "8080");
		defaults.put(API_SECURE, "false");

		defaults.put(EXTRACTOR_OUTPUTDIR, "data");

		defaults.put(NEO4J_HOST, "localhost");
		defaults.put(NEO4J_PORT, "7687");

		defaults.put(AWS_BUCKET_NAME, "mod-datadumps-dev"); // This needs to always be a dev bucket unless running in production

		// File Generator
		defaults.put(GENERATED_FILES_FOLDER, "data");
		defaults.put(SKIP_S3_UPLOAD, "false");

		defaults.put(AO_TERM_LIST, "anatomy-term-order.csv");
		defaults.put(GO_TERM_LIST, "go-term-order.csv");
		defaults.put(RIBBON_TERM_SPECIES_APPLICABILITY, "ribbon-term-species-applicability.csv");
		defaults.put(POPULARITY_DOWNLOAD_URL, "https://raw.githubusercontent.com/alliance-genome/agr_pageview_popularity_calculator/master/alliance_popularity.tsv");
		defaults.put(POPULARITY_FILE_NAME, "alliance_popularity.tsv");

		defaults.put(VARIANT_DOWNLOAD_PATH, "data");
		defaults.put(VARIANT_CACHER_CONFIG_FILE, "variantDownloadFiles.yaml");

		defaults.put(ALLIANCE_RELEASE, "0.0.0");
		defaults.put(FMS_URL, "https://fms.alliancegenome.org/api");
		
		//literature indexer
		defaults.put(BLUETEAM_ES_URL, "localhost");
		defaults.put(BLUETEAM_ES_INDEX, "public_references_index");

		// This next item needs to be set in order to prevent the
		// Caused by: java.lang.IllegalStateException: availableProcessors is already set to [16], rejecting [16]
		// error from happening.
		System.setProperty("es.set.netty.runtime.available.processors", "false");

		allKeys = defaults.keySet();

		if (configProperties.size() == 0) {
			final String configPropertiesFileName = "application.properties";
			configProperties = FileHelper.getPropertiesFromFile(configPropertiesFileName);
		}

		for (String key : allKeys) {
			// First checks the -D params and sets config[key] = value otherwise it will be null.
			if (config.get(key) == null) {
				config.put(key, loadSystemProperty(key));
			}
			// Second checks the config.properties file built into the application otherwise it will be null.
			if (config.get(key) == null) {
				config.put(key, loadConfigProperty(key));
			}
			// Third checks the environment for a NAME = value otherwise leaves it null.
			if (config.get(key) == null) {
				config.put(key, loadSystemENVProperty(key));
			}
			// Lastly loads the default value for NAME = value and loadDefaultProperty ensures it won't be null.
			if (config.get(key) == null) {
				config.put(key, loadDefaultProperty(key));
			}
		}
		printProperties();
		init = true;
	}

	private static String loadSystemProperty(String key) {
		String ret = System.getProperty(key);
		if (ret != null) {
			log.debug("Found: -D " + key + "=" + ret);
		}
		return ret;
	}

	private static String loadConfigProperty(String key) {
		String ret = configProperties.getProperty(key);
		if (ret != null) {
			log.debug("Config File Property: " + key + "=" + ret);
		}
		return ret;
	}

	public static String loadSystemENVProperty(String key) {
		String ret = System.getenv(key);
		if (ret != null) {
			log.debug("Found Enviroment ENV[" + key + "]=" + ret);
		}
		return ret;
	}

	private static String loadDefaultProperty(String key) {
		String ret = defaults.get(key);
		if (ret != null) {
			log.debug("Setting default: " + key + "=" + ret);
		}
		return ret;
	}

	public static int getEsBulkActionSize() {
		if (!init) {
			init();
		}
		try {
			return Integer.parseInt(config.get(ES_BULK_ACTION_SIZE));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public static long getEsBulkSizeMB() {
		if (!init) {
			init();
		}
		try {
			return Long.parseLong(config.get(ES_BULK_REQUEST_SIZE));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public static int getEsBulkConcurrentRequests() {
		if (!init) {
			init();
		}
		try {
			return Integer.parseInt(config.get(ES_BULK_CONCURRENT_REQUESTS));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public static String getEsHost() {
		if (!init) {
			init();
		}
		return config.get(ES_HOST);
	}
	
	public static int getEsShardCount() {
		if (!init) {
			init();
		}
		try {
			return Integer.parseInt(config.get(ES_SHARD_COUNT));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public static Multimap<String, Integer> getEsHostMap() {

		Multimap<String, Integer> hostMap = ArrayListMultimap.create();

		String esHostConfig = getEsHost();
		if (esHostConfig.contains(",")) {
			String[] hosts = esHostConfig.split(",");
			for (String host : hosts) {
				if (host.contains(":")) {
					String[] array = host.split(":");
					hostMap.put(array[0], Integer.parseInt(array[1]));
				} else {
					hostMap.put(host, getEsPort());
				}
			}
		} else {
			if (esHostConfig.contains(":")) {
				String[] array = esHostConfig.split(":");
				hostMap.put(array[0], Integer.parseInt(array[1]));
			} else {
				hostMap.put(esHostConfig, getEsPort());
			}
		}
		return hostMap;
	}

	public static int getEsPort() {
		if (!init) {
			init();
		}
		try {
			return Integer.parseInt(config.get(ES_PORT));
		} catch (NumberFormatException e) {
			return 9200;
		}
	}

	public static String getApiHost() {
		if (!init) {
			init();
		}
		return config.get(API_HOST);
	}

	public static String getCurationApiUrl() {
		if (!init) {
			init();
		}
		return config.get(CURATION_API_URL);
	}

	public static int getApiPort() {
		if (!init) {
			init();
		}
		try {
			return Integer.parseInt(config.get(API_PORT));
		} catch (NumberFormatException e) {
			return 443;
		}
	}

	public static Boolean isApiSecure() {
		if (!init) {
			init();
		}
		return Boolean.parseBoolean(config.get(API_SECURE));
	}

	public static String getApiBaseUrl() {
		String url = "";

		if (isApiSecure()) {
			url = "https://";
			url += getApiHost();
			if (getApiPort() != 443) {
				url += ":" + getApiPort();
			}
		} else {
			url = "http://";
			url += getApiHost();
			if (getApiPort() != 80) {
				url += ":" + getApiPort();
			}
		}

		return url + "/api";
	}

	public static String getNeo4jHost() {
		if (!init) {
			init();
		}
		return config.get(NEO4J_HOST);
	}

	public static int getNeo4jPort() {
		if (!init) {
			init();
		}
		try {
			return Integer.parseInt(config.get(NEO4J_PORT));
		} catch (NumberFormatException e) {
			return 7687;
		}
	}

	public static boolean isThreaded() {
		if (!init) {
			init();
		}
		return Boolean.parseBoolean(config.get(THREADED));
	}

	public static String getEsIndexPrefix() {
		if (!init) {
			init();
		}
		return config.get(ES_INDEX_PREFIX);
	}

	public static String getEsIndexSuffix() {
		if (!init) {
			init();
		}
		return config.get(ES_INDEX_SUFFIX);
	}

	public static String getDataExtractorDirectory() {
		if (!init) {
			init();
		}
		return config.get(EXTRACTOR_OUTPUTDIR);
	}

	public static String getEsIndex() {
		if (!init) {
			init();
		}
		return config.get(ES_INDEX);
	}

	public static Date getAppStart() {
		if (!init) {
			init();
		}
		return appStart;
	}

	public static boolean getDebug() {
		if (!init) {
			init();
		}
		return Boolean.parseBoolean(config.get(DEBUG));
	}

	public static String getAWSBucketName() {
		if (!init) {
			init();
		}
		return config.get(AWS_BUCKET_NAME);
	}

	public static String getAwsProfile() {
		if (!init) {
			init();
		}
		return config.get(AWS_PROFILE);
	}

	public static String getAwsAccessKey() {
		if (!init) {
			init();
		}
		return config.get(AWS_ACCESS_KEY);
	}

	public static String getAwsSecretKey() {
		if (!init) {
			init();
		}
		return config.get(AWS_SECRET_KEY);
	}

	public static String getGeneratedFilesFolder() {
		if (!init) {
			init();
		}
		return config.get(GENERATED_FILES_FOLDER);
	}

	public static boolean getSkipS3Upload() {
		if (!init) {
			init();
		}
		return Boolean.parseBoolean(config.get(SKIP_S3_UPLOAD));
	}

	public static String getVariantDownloadPath() {
		if (!init) {
			init();
		}
		return config.get(VARIANT_DOWNLOAD_PATH);
	}

	public static boolean getIndexVariants() {
		if (!init) {
			init();
		}
		return Boolean.parseBoolean(config.get(INDEX_VARIANTS));
	}

	public static String getJavaLineSeparator() {
		if (!init) {
			init();
		}
		return System.getProperty("line.separator");
	}

	public static String getJavaTmpDir() {
		if (!init) {
			init();
		}
		return System.getProperty("java.io.tmpdir");
	}

	public static String getValidationSoftwarePath() {
		if (!init) {
			init();
		}
		return getJavaTmpDir();
	}

	public static boolean hasEsIndexPrefix() {
		if (!init) {
			init();
		}
		return ConfigHelper.getEsIndexPrefix() != null && !ConfigHelper.getEsIndexPrefix().equals("") && ConfigHelper.getEsIndexPrefix().length() > 0;
	}

	public static boolean hasEsIndexSuffix() {
		if (!init) {
			init();
		}
		return ConfigHelper.getEsIndexSuffix() != null && !ConfigHelper.getEsIndexSuffix().equals("") && ConfigHelper.getEsIndexSuffix().length() > 0;
	}

	public static String getAOTermListFilePath() {
		if (!init) {
			init();
		}
		return config.get(AO_TERM_LIST);
	}

	public static String getGOTermListFilePath() {
		if (!init) {
			init();
		}
		return config.get(GO_TERM_LIST);
	}

	public static String getRibbonTermSpeciesApplicabilityPath() {
		if (!init) {
			init();
		}
		return config.get(RIBBON_TERM_SPECIES_APPLICABILITY);
	}

	public static String getPopularityDownloadUrl() {
		if (!init) {
			init();
		}
		return config.get(POPULARITY_DOWNLOAD_URL);
	}

	public static String getPopularityFileName() {
		if (!init) {
			init();
		}
		return config.get(POPULARITY_FILE_NAME);
	}

	public static String getFMSUrl() {
		if (!init) {
			init();
		}
		return config.get(FMS_URL);
	}

	public static String getAllianceRelease() {
		if (!init) {
			init();
		}
		return config.get(ALLIANCE_RELEASE);
	}

	public static void setNameValue(String key, String value) {
		config.put(key, value);
	}

	public static void printProperties() {
		log.info("Running with Properties:");
		for (String key : allKeys) {
			log.info("\t" + key + ": " + config.get(key));
		}
	}

	public static String getStringParam(String configParam) {
		if (!init) {
			init();
		}
		return config.get(configParam);
	}

	public static boolean isProduction() {
		return getNeo4jHost().contains("production");
	}

	public static String getCurationApiToken() {
		if (!init) {
			init();
		}
		if (config.get(CURATION_API_TOKEN) != null) {
			return "APIToken " + config.get(CURATION_API_TOKEN);
		}
		return null;
	}
	
	public static String getBlueTeamESUrl() {
		if (!init) {
			init();
		}
		return config.get(BLUETEAM_ES_URL);
	}
	
	public static String getBlueTeamESIndex() {
		if (!init) {
			init();
		}
		return config.get(BLUETEAM_ES_INDEX);
	}
}