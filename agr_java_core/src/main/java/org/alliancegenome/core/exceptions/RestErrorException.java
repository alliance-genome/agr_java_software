package org.alliancegenome.core.exceptions;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RestErrorException extends RuntimeException {

    public RestErrorException(RestErrorMessage error) {
        super();
        this.error = error;
    }

    private RestErrorMessage error;

}
