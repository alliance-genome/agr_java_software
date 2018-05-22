package org.alliancegenome.es.index.data;

import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmissionResponce extends APIResponce {

    @JsonInclude(value=Include.NON_NULL)
    private HashMap<String, String> fileStatus = new HashMap<>();

}
