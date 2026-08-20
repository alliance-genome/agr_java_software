package org.alliancegenome.api.filters;

import java.io.IOException;
import java.util.List;

import org.alliancegenome.api.exceptions.RestErrorMessage;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION - 100)
public class PublicPaginationRequestFilter implements ContainerRequestFilter {

	public static final int MAX_PUBLIC_LIMIT = 1000;
	public static final int DEFAULT_PUBLIC_LIMIT = 20;
	public static final String ERROR_MESSAGE =
		"Invalid pagination parameters: 'limit' must be an integer from 1 through 1000, "
			+ "'page' must be a positive integer, and the requested offset must be supported";

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		MultivaluedMap<String, String> queryParameters = requestContext.getUriInfo().getQueryParameters(false);
		List<String> rawLimits = queryParameters.get("limit");
		List<String> rawPages = queryParameters.get("page");

		if (rawLimits == null && rawPages == null) {
			return;
		}

		try {
			validatePaginationParameters(rawLimits, rawPages);
		} catch (IllegalArgumentException | ArithmeticException exception) {
			requestContext.abortWith(invalidPaginationResponse());
		}
	}

	static void validatePaginationParameters(List<String> rawLimits, List<String> rawPages) {
		List<Long> limits = parseValues(rawLimits, DEFAULT_PUBLIC_LIMIT, MAX_PUBLIC_LIMIT);
		List<Long> pages = parseValues(rawPages, 1, Integer.MAX_VALUE);

		for (long page : pages) {
			for (long limit : limits) {
				long offset = Math.multiplyExact(page - 1, limit);
				if (offset > Integer.MAX_VALUE) {
					throw new IllegalArgumentException("Pagination offset exceeds the supported range");
				}
			}
		}
	}

	private static List<Long> parseValues(List<String> rawValues, long defaultValue, long maximum) {
		if (rawValues == null) {
			return List.of(defaultValue);
		}
		if (rawValues.isEmpty()) {
			throw new IllegalArgumentException("Pagination parameter has no value");
		}

		return rawValues.stream().map(value -> {
			long parsed = Long.parseLong(value);
			if (parsed < 1 || parsed > maximum) {
				throw new IllegalArgumentException("Pagination parameter is outside the supported range");
			}
			return parsed;
		}).toList();
	}

	static Response invalidPaginationResponse() {
		RestErrorMessage error = new RestErrorMessage(ERROR_MESSAGE);
		error.setStatusCode(Response.Status.BAD_REQUEST.getStatusCode());
		error.setStatusCodeName(Response.Status.BAD_REQUEST.getReasonPhrase());
		return Response.status(Response.Status.BAD_REQUEST)
			.type(MediaType.APPLICATION_JSON_TYPE)
			.entity(error)
			.build();
	}
}
