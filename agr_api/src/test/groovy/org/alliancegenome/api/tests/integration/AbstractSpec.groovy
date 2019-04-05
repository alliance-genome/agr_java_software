package org.alliancegenome.api

import groovy.json.JsonSlurper
import org.alliancegenome.core.config.ConfigHelper
import spock.lang.Specification

class AbstractSpec extends Specification {

    def getApiResults(String apiPath) {
        URL url = new URL(ConfigHelper.getApiBaseUrl() + apiPath)
        return new JsonSlurper().parseText(url.text).results
    }

    def getApiResult(String apiPath) {
        URL url = new URL(ConfigHelper.getApiBaseUrl() + apiPath)
        return new JsonSlurper().parseText(url.text)
    }

    def getApiResultRaw(String apiPath) {
        URL url = new URL(ConfigHelper.getApiBaseUrl() + apiPath)
        return url.text
    }

    def getApiMetaData(String apiPath) {
        return getApiResult(apiPath)
    }

}