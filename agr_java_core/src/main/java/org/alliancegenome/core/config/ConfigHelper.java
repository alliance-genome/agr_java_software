package org.alliancegenome.core.config;

import static org.alliancegenome.core.config.Constants.AO_TERM_LIST;
import static org.alliancegenome.core.config.Constants.API_ACCESS_TOKEN;
import static org.alliancegenome.core.config.Constants.API_HOST;
import static org.alliancegenome.core.config.Constants.API_PORT;
import static org.alliancegenome.core.config.Constants.API_SECURE;
import static org.alliancegenome.core.config.Constants.AWS_ACCESS_KEY;
import static org.alliancegenome.core.config.Constants.AWS_BUCKET_NAME;
import static org.alliancegenome.core.config.Constants.AWS_SECRET_KEY;
import static org.alliancegenome.core.config.Constants.CACHE_HOST;
import static org.alliancegenome.core.config.Constants.CACHE_PORT;
import static org.alliancegenome.core.config.Constants.DEBUG;
import static org.alliancegenome.core.config.Constants.ES_DATA_INDEX;
import static org.alliancegenome.core.config.Constants.ES_HOST;
import static org.alliancegenome.core.config.Constants.ES_INDEX;
import static org.alliancegenome.core.config.Constants.ES_INDEX_SUFFIX;
import static org.alliancegenome.core.config.Constants.ES_PORT;
import static org.alliancegenome.core.config.Constants.GO_TERM_LIST;
import static org.alliancegenome.core.config.Constants.KEEPINDEX;
import static org.alliancegenome.core.config.Constants.NEO4J_HOST;
import static org.alliancegenome.core.config.Constants.NEO4J_PORT;
import static org.alliancegenome.core.config.Constants.SPECIES;
import static org.alliancegenome.core.config.Constants.THREADED;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class ConfigHelper {

    private static Date appStart = new Date();
    private static Logger log = LogManager.getLogger(ConfigHelper.class);
    private static Properties configProperties = new Properties();

    private static HashMap<String, String> defaults = new HashMap<>();
    private static HashMap<String, String> config = new HashMap<>();
    private static Set<String> allKeys;
    private static boolean init = false;

    public ConfigHelper() {
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
        defaults.put(THREADED, "true"); // Indexer Value
        defaults.put(API_ACCESS_TOKEN, "api_access_token"); // Api Value

        defaults.put(DEBUG, "false");

        defaults.put(ES_INDEX, "site_index"); // Can be over written
        defaults.put(ES_INDEX_SUFFIX, ""); // Prod, Dev, Stage, etc
        defaults.put(ES_DATA_INDEX, "data_index");
        defaults.put(ES_HOST, "localhost");
        defaults.put(ES_PORT, "9300");

        defaults.put(KEEPINDEX, "false");
        defaults.put(SPECIES, null);

        defaults.put(API_HOST, "localhost");
        defaults.put(API_PORT, "8080");
        defaults.put(API_SECURE, "false");

        defaults.put(CACHE_HOST, "localhost");
        defaults.put(CACHE_PORT, "11222");

        defaults.put(NEO4J_HOST, "localhost");
        defaults.put(NEO4J_PORT, "7687");

        defaults.put(AWS_ACCESS_KEY, null);
        defaults.put(AWS_SECRET_KEY, null);
        defaults.put(AWS_BUCKET_NAME, "mod-datadumps-dev"); // This needs to always be a dev bucket unless running in production

        defaults.put(AO_TERM_LIST, "anatomy-term-order.csv");
        defaults.put(GO_TERM_LIST, "go-term-order.csv");
        
        // This next item needs to be set in order to prevent the 
        // Caused by: java.lang.IllegalStateException: availableProcessors is already set to [16], rejecting [16]
        // error from happening.
        System.setProperty("es.set.netty.runtime.available.processors", "false");

        allKeys = defaults.keySet();

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

    public static String getCacheHost() {
        if (!init) init();
        return config.get(CACHE_HOST);
    }

    public static int getCachePort() {
        if (!init) init();
        try {
            return Integer.parseInt(config.get(CACHE_PORT));
        } catch (NumberFormatException e) {
            return 11222;
        }
    }

    public static String getEsHost() {
        if (!init) init();
        return config.get(ES_HOST);
    }

    public static int getEsPort() {
        if (!init) init();
        try {
            return Integer.parseInt(config.get(ES_PORT));
        } catch (NumberFormatException e) {
            return 9300;
        }
    }

    public static String getApiHost() {
        if (!init) init();
        return config.get(API_HOST);
    }

    public static int getApiPort() {
        if (!init) init();
        try {
            return Integer.parseInt(config.get(API_PORT));
        } catch (NumberFormatException e) {
            return 443;
        }
    }

    public static Boolean isApiSecure() {
        if (!init) init();
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

        return url;
    }

    public static String getNeo4jHost() {
        if (!init) init();
        return config.get(NEO4J_HOST);
    }

    public static int getNeo4jPort() {
        if (!init) init();
        try {
            return Integer.parseInt(config.get(NEO4J_PORT));
        } catch (NumberFormatException e) {
            return 7687;
        }
    }

    public static boolean isThreaded() {
        if (!init) init();
        return Boolean.parseBoolean(config.get(THREADED));
    }

    public static String getEsIndexSuffix() {
        if (!init) init();
        return config.get(ES_INDEX_SUFFIX);
    }

    public static String getAWSAccessKey() {
        if (!init) init();
        return config.get(AWS_ACCESS_KEY);
    }

    public static String getAWSSecretKey() {
        if (!init) init();
        return config.get(AWS_SECRET_KEY);
    }

    public static String getEsIndex() {
        if (!init) init();
        return config.get(ES_INDEX);
    }

    public static String getEsDataIndex() {
        if (!init) init();
        return config.get(ES_DATA_INDEX);
    }

    public static String getApiAccessToken() {
        if (!init) init();
        return config.get(API_ACCESS_TOKEN);
    }

    public static Date getAppStart() {
        if (!init) init();
        return appStart;
    }

    public static boolean getDebug() {
        if (!init) init();
        return Boolean.parseBoolean(config.get(DEBUG));
    }

    public static boolean getKeepIndex() {
        if (!init) init();
        return Boolean.parseBoolean(config.get(KEEPINDEX));
    }

    public static String getAWSBucketName() {
        if (!init) init();
        return config.get(AWS_BUCKET_NAME);
    }

    public static String getSpecies() {
        if (!init) init();
        return config.get(SPECIES);
    }

    public static String getJavaLineSeparator() {
        if (!init) init();
        return System.getProperty("line.separator");
    }

    public static String getJavaTmpDir() {
        if (!init) init();
        return System.getProperty("java.io.tmpdir");
    }

    public static String getValidationSoftwarePath() {
        if (!init) init();
        return getJavaTmpDir();
    }

    public static boolean hasEsIndexSuffix() {
        if (!init) init();
        return (ConfigHelper.getEsIndexSuffix() != null && !ConfigHelper.getEsIndexSuffix().equals("") && ConfigHelper.getEsIndexSuffix().length() > 0);
    }

    public static String getAOTermListFilePath() {
        if (!init) init();
        return config.get(AO_TERM_LIST);
    }

    public static String getGOTermListFilePath() {
        if (!init) init();
        return config.get(GO_TERM_LIST);
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

    private static LinkedHashMap<String, String> getNameValuePairsList(String filePath) {

        LinkedHashMap<String, String> nameValueList = new LinkedHashMap<>();
        InputStream in = null;
        BufferedReader reader = null;
        try {
            String str = null;
            in = ConfigHelper.class.getClassLoader().getResourceAsStream(filePath);
            if (in != null) {
                reader = new BufferedReader(new InputStreamReader(in));
                while ((str = reader.readLine()) != null) {
                    String[] token = str.split("\t");
                    if (token.length < 2) {
                        final String message = "Could not find two columns in ordering file: " + filePath;
                        log.error(message);
                        throw new RuntimeException(message);
                    }
                    nameValueList.put(token[0], token[1]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return nameValueList;
    }

    public static LinkedHashMap<String, String> getAOTermList() {
        return getNameValuePairsList(getAOTermListFilePath());
    }

    public static LinkedHashMap<String, String> getGOTermList() {
        return getNameValuePairsList(getGOTermListFilePath());
    }

    public static boolean isProduction() {
        return getNeo4jHost().contains("production");
    }
}
