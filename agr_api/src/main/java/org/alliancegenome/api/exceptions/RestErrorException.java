package org.alliancegenome.api.exceptions;

import lombok.Data;

@Data
public class RestErrorException extends RuntimeException {

	private RestErrorMessage error;

	public RestErrorException(RestErrorMessage error) {
		super(String.join(", ", error.getErrors()));
		this.error = error;
	}

}
