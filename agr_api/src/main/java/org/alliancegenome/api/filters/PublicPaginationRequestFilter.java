package org.alliancegenome.api.filters;

import java.io.IOException;
import java.util.List;

import org.alliancegenome.api.exceptions.RestErrorMessage;
import org.alliancegenome.core.es.schema.settings.SiteIndexSettings;
import org.alliancegenome.core.es.schema.settings.VariantIndexSettings;

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
	public static final int DEFAULT_SEARCH_LIMIT = 10;
	public static final int MAX_PUBLIC_RESULT_WINDOW = Math.min(
		SiteIndexSettings.MAX_RESULT_WINDOW,
		VariantIndexSettings.MAX_RESULT_WINDOW
	);
	public static final String ERROR_MESSAGE =
		"Invalid pagination parameters: 'limit' must be an integer from 1 through 1000, "
			+ "'page' must be a positive integer, 'offset' must be a non-negative integer, "
			+ "and the requested result window must be supported";

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		MultivaluedMap<String, String> queryParameters = requestContext.getUriInfo().getQueryParameters(false);
		List<String> rawLimits = queryParameters.get("limit");
		List<String> rawPages = queryParameters.get("page");
		List<String> rawOffsets = queryParameters.get("offset");

		if (rawLimits == null && rawPages == null && rawOffsets == null) {
			return;
		}

		try {
			if (rawLimits != null || rawPages != null) {
				validatePaginationParameters(rawLimits, rawPages);
			}
			if (rawOffsets != null) {
				validateOffsetParameters(rawLimits, rawOffsets);
			}
		} catch (IllegalArgumentException | ArithmeticException exception) {
			requestContext.abortWith(invalidPaginationResponse());
		}
	}

	static void validatePaginationParameters(List<String> rawLimits, List<String> rawPages) {
		List<Long> limits = parseValues(rawLimits, DEFAULT_PUBLIC_LIMIT, 1, MAX_PUBLIC_LIMIT);
		List<Long> pages = parseValues(rawPages, 1, 1, Integer.MAX_VALUE);

		for (long page : pages) {
			for (long limit : limits) {
				long offset = Math.multiplyExact(page - 1, limit);
				validateResultWindow(offset, limit);
			}
		}
	}

	static void validateOffsetParameters(List<String> rawLimits, List<String> rawOffsets) {
		List<Long> limits = parseValues(rawLimits, DEFAULT_SEARCH_LIMIT, 1, MAX_PUBLIC_LIMIT);
		List<Long> offsets = parseValues(rawOffsets, 0, 0, MAX_PUBLIC_RESULT_WINDOW);

		for (long offset : offsets) {
			for (long limit : limits) {
				validateResultWindow(offset, limit);
			}
		}
	}

	private static void validateResultWindow(long offset, long limit) {
		long resultWindow = Math.addExact(offset, limit);
		if (resultWindow > MAX_PUBLIC_RESULT_WINDOW) {
			throw new IllegalArgumentException("Pagination result window exceeds the supported range");
		}
	}

	private static List<Long> parseValues(List<String> rawValues, long defaultValue, long minimum, long maximum) {
		if (rawValues == null) {
			return List.of(defaultValue);
		}
		if (rawValues.isEmpty()) {
			throw new IllegalArgumentException("Pagination parameter has no value");
		}

		return rawValues.stream().map(value -> {
			long parsed = Long.parseLong(value);
			if (parsed < minimum || parsed > maximum) {
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
