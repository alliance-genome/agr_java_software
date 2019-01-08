package org.alliancegenome.core.service;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.view.View;

import java.util.Map;
import java.util.TreeMap;

@Setter
@Getter
class Request {
    @JsonView(View.Default.class)
    String uri;
    @JsonView(View.Default.class)
    TreeMap<String, String[]> parameterMap =new TreeMap<>();

    void setParameterMap(Map<String, String[]> parameterMap) {
        this.parameterMap.putAll(parameterMap);
    }
}

