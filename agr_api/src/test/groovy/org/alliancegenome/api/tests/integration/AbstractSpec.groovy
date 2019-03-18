package org.alliancegenome.api

import org.alliancegenome.core.config.ConfigHelper
import groovy.json.JsonSlurper
import spock.lang.*

class AbstractSpec extends Specification {

    protected def getApiResults(String apiPath) {
        URL url = new URL(ConfigHelper.getApiBaseUrl() + apiPath)
        println url
        return new JsonSlurper().parseText(url.text).results
    }

}