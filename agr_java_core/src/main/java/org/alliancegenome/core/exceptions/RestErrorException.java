package org.alliancegenome.core.exceptions;

import lombok.Getter;
import lombok.Setter;

import java.util.stream.Collectors;

@Setter
@Getter
public class RestErrorException extends RuntimeException {

    private RestErrorMessage error;

    public RestErrorException(String message) {
        super();
        error = new RestErrorMessage(message);
    }

    public RestErrorException(RestErrorMessage error) {
        super(String.join(", ", error.getErrors()));
        this.error = error;
    }


}
