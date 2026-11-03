package org.alliancegenome.api.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.alliancegenome.api.exceptions.RestErrorException;
import org.junit.Test;

public class GeneControllerTest {

	@Test
	public void rejectsAlleleViewerPagesAboveEndpointLimit() {
		GeneController controller = new GeneController();

		RestErrorException exception = assertThrows(RestErrorException.class, () ->
			controller.getAlleleViewerIds("HGNC:11998", 1001, 1, null, null, null, null, null, null, null, null));

		assertEquals("'limit' request parameter invalid: Found [1001]. It must not exceed 1000",
			exception.getError().getErrors().get(0));
	}
}
