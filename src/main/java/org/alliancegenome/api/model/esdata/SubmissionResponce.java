package org.alliancegenome.api.model.esdata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties()
public class SubmissionResponce {

    @JsonInclude(value=Include.NON_NULL)
    private String error = null;
    private String status = "success";
}
