package org.alliancegenome.api.filters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.alliancegenome.api.exceptions.RestErrorMessage;
import org.junit.Test;

import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

public class PublicPaginationRequestFilterTest {

	@Test
	public void isRegisteredAsGlobalPreMatchingProvider() {
		assertNotNull(PublicPaginationRequestFilter.class.getAnnotation(Provider.class));
		assertNotNull(PublicPaginationRequestFilter.class.getAnnotation(PreMatching.class));
	}

	@Test
	public void acceptsAbsentAndBoundedPaginationValues() {
		PublicPaginationRequestFilter.validatePaginationParameters(null, null);
		PublicPaginationRequestFilter.validatePaginationParameters(List.of("1"), List.of("1"));
		PublicPaginationRequestFilter.validatePaginationParameters(List.of("1000"), List.of("1"));
		PublicPaginationRequestFilter.validatePaginationParameters(List.of("1000"), List.of("150"));
		PublicPaginationRequestFilter.validatePaginationParameters(List.of("10", "20"), List.of("2"));
		PublicPaginationRequestFilter.validateOffsetParameters(null, List.of("0"));
		PublicPaginationRequestFilter.validateOffsetParameters(List.of("1000"), List.of("149000"));
	}

	@Test
	public void rejectsInvalidLimits() {
		for (String limit : List.of("", "0", "-1", "1001", "not-a-number", "2147483648")) {
			assertThrows(RuntimeException.class, () ->
				PublicPaginationRequestFilter.validatePaginationParameters(List.of(limit), null));
		}
	}

	@Test
	public void rejectsInvalidPagesAndOffsets() {
		for (String page : List.of("", "0", "-1", "not-a-number", "2147483648", "7501")) {
			assertThrows(RuntimeException.class, () ->
				PublicPaginationRequestFilter.validatePaginationParameters(null, List.of(page)));
		}
		assertThrows(RuntimeException.class, () ->
			PublicPaginationRequestFilter.validatePaginationParameters(List.of("2"), List.of("2147483647")));
	}

	@Test
	public void rejectsInvalidOffsetsAndResultWindows() {
		for (String offset : List.of("", "-1", "not-a-number", "2147483648", "150000")) {
			assertThrows(RuntimeException.class, () ->
				PublicPaginationRequestFilter.validateOffsetParameters(List.of("1000"), List.of(offset)));
		}
	}

	@Test
	public void rejectsAnyInvalidRepeatedValue() {
		assertThrows(RuntimeException.class, () ->
			PublicPaginationRequestFilter.validatePaginationParameters(List.of("10", "1001"), List.of("1")));
	}

	@Test
	public void returnsStableClientFacingError() {
		Response response = PublicPaginationRequestFilter.invalidPaginationResponse();
		assertEquals(400, response.getStatus());
		assertEquals("application/json", response.getMediaType().toString());

		RestErrorMessage error = (RestErrorMessage) response.getEntity();
		assertEquals(400, error.getStatusCode());
		assertEquals("Bad Request", error.getStatusCodeName());
		assertEquals(List.of(PublicPaginationRequestFilter.ERROR_MESSAGE), error.getErrors());
		assertFalse(error.getErrors().get(0).contains("max_result_window"));
		assertTrue(error.getErrors().get(0).contains("1000"));
	}
}
