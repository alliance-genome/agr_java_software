package org.alliancegenome.indexer.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConfigHelper {

    private static Logger log = LogManager.getLogger(ConfigHelper.class);
    private static Properties configProperties = new Properties();

    private static HashMap<String, String> defaults = new HashMap<>();
    private static HashMap<String, String> config = new HashMap<>();
    private static Set<String> allKeys;

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
        defaults.put("THREADED", "true");

        defaults.put("ES_HOST", "localhost");
        defaults.put("ES_PORT", "9300");
        defaults.put("ES_INDEX_SUFFIX", "");

        defaults.put("NEO4J_HOST", "localhost");
        defaults.put("NEO4J_PORT", "7687");

        defaults.put("AWS_ACCESS_KEY", null);
        defaults.put("AWS_SECRET_KEY", null);

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
        for (String key : allKeys) {
            log.info("\t" + key + ": " + config.get(key));
        }
    }

    public static boolean isThreaded() {
        return Boolean.parseBoolean(config.get("THREADED"));
    }

    public static String getEsIndexSuffix() {
        return config.get("ES_INDEX_SUFFIX");
    }
    public static boolean hasEsIndexSuffix() {
        return (ConfigHelper.getEsIndexSuffix() != null && !ConfigHelper.getEsIndexSuffix().equals("") && ConfigHelper.getEsIndexSuffix().length() > 0);
    }

    public static String getAWSAccessKey() {
        return config.get("AWS_ACCESS_KEY");
    }

    public static String getAWSSecretKey() {
        return config.get("AWS_SECRET_KEY");
    }
}
