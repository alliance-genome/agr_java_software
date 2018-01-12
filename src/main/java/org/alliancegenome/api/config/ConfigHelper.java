package org.alliancegenome.api.config;

import org.jboss.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import java.util.Date;
import java.util.HashMap;

@ApplicationScoped
public class ConfigHelper {

    private static Logger log = Logger.getLogger(ConfigHelper.class);
    private Date appStart = new Date();
    private HashMap<String, String> defaults = new HashMap<>();
    private HashMap<String, String> config = new HashMap<>();

    @PostConstruct
    public void init() {
        log.info("Config Helper");
        defaults.put("API_ACCESS_TOKEN", "api_password");
        defaults.put("DEBUG", "false");
        defaults.put("ES_INDEX", "site_index");
        defaults.put("ES_DATA_INDEX", "data_index");
        defaults.put("ES_HOST", "localhost");
        defaults.put("ES_PORT", "9300");

        defaults.put("AWS_ACCESS_KEY", null);
        defaults.put("AWS_SECRET_KEY", null);

        for (String key : defaults.keySet()) {
            if (config.get(key) == null) config.put(key, loadSystemProperty(key));
            if (config.get(key) == null) config.put(key, loadSystemENVProperty(key));
            if (config.get(key) == null) config.put(key, loadDefaultProperty(key));
        }
        printProperties();
    }

    private String loadSystemProperty(String key) {
        //log.info(System.getProperties());
        String ret = System.getProperty(key);
        if (ret != null) log.info("Found: -D " + key + "=" + ret);
        return ret;
    }

    private String loadSystemENVProperty(String key) {
        //log.info(System.getenv());
        String ret = System.getenv(key);
        if (ret != null) log.info("Found Enviroment ENV[" + key + "]=" + ret);
        return ret;
    }

    private String loadDefaultProperty(String key) {
        String ret = defaults.get(key);
        if (ret != null) log.info("Setting default: " + key + "=" + ret);
        return ret;
    }

    public String getEsIndex() {
        return config.get("ES_INDEX");
    }

    public String getEsDataIndex() {
        return config.get("ES_DATA_INDEX");
    }

    public String getEsHost() {
        return config.get("ES_HOST");
    }

    public int getEsPort() {
        try {
            return Integer.parseInt(config.get("ES_PORT"));
        } catch (NumberFormatException e) {
            return 9300;
        }
    }

    public String getApiAccessToken() {
        return config.get("API_ACCESS_TOKEN");
    }

    public boolean getDebug() {
        return Boolean.parseBoolean(config.get("DEBUG"));
    }

    public String getAWSAccessKey() {
        return config.get("AWS_ACCESS_KEY");
    }

    public String getAWSSecretKey() {
        return config.get("AWS_SECRET_KEY");
    }

    public String getValidationSoftwarePath() {
        return System.getProperty("java.io.tmpdir");
    }

    public Date getAppStart() {
        return appStart;
    }

    public void printProperties() {
        log.info("Running with Properties:");
        for (String key : defaults.keySet()) {
            log.info("\t" + key + ": " + config.get(key));
        }
    }



}
