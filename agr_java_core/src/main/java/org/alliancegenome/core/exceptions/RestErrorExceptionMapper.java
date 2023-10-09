package org.alliancegenome.core.exceptions;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class RestErrorExceptionMapper implements ExceptionMapper<RestErrorException> {

	@Override
	@Produces(MediaType.APPLICATION_JSON)
	public Response toResponse(RestErrorException e) {

		Response.ResponseBuilder rb = Response.status(Response.Status.BAD_REQUEST);
		RestErrorMessage error = e.getError();
		error.setStatusCode(Response.Status.BAD_REQUEST.getStatusCode());
		error.setStatusCodeName(Response.Status.BAD_REQUEST.getReasonPhrase());
		rb.entity(error);

		return rb.build();
	}
}
