package org.alliancegenome.api

import groovy.json.JsonSlurper
import org.alliancegenome.core.config.ConfigHelper
import spock.lang.Specification

class AbstractSpec extends Specification {

    protected def getApiResults(String apiPath) {
        URL url = new URL(ConfigHelper.getApiBaseUrl() + apiPath)
        println url
        return new JsonSlurper().parseText(url.text).results
    }

    protected def getApiResult(String apiPath) {
        URL url = new URL(ConfigHelper.getApiBaseUrl() + apiPath)
        println url
        return new JsonSlurper().parseText(url.text)
    }

    protected def getApiMetaData(String apiPath) {
        return getApiResult(apiPath)
    }

}