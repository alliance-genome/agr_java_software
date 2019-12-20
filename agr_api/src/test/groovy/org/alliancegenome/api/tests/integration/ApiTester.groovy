package org.alliancegenome.api.tests.integration

import groovy.json.JsonSlurper
import org.alliancegenome.core.config.ConfigHelper

class ApiTester {

    static def getApiResults(String apiPath) {
        URL url = new URL(ConfigHelper.getApiBaseUrl() + apiPath)
        return new JsonSlurper().parseText(url.text).results
    }

    static getApiResult(String apiPath) {
        URL url = new URL(ConfigHelper.getApiBaseUrl() + apiPath)
        return new JsonSlurper().parseText(url.text)
    }

    static getApiResultRaw(String apiPath) {
        URL url = new URL(ConfigHelper.getApiBaseUrl() + apiPath)
        return url.text
    }

    static getApiMetaData(String apiPath) {
        return getApiResult(apiPath)
    }

}
