package org.alliancegenome.cache.repository.helper;

import java.util.*;

import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.*;

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

