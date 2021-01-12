package org.alliancegenome.core.exceptions;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonPropertyOrder({"statusCode", "errors", "statusCodeName"})
public class RestErrorMessage {

    private int statusCode;
    private String statusCodeName;
    private List<String> errors = new ArrayList<>();

    public RestErrorMessage() {
    }

    public RestErrorMessage(String errorMessage) {
        this.errors.add(errorMessage);
    }

    public void addErrorMessage(String message) {
        errors.add(message);
    }
}
