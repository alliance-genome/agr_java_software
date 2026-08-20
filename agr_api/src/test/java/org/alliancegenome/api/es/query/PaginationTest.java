package org.alliancegenome.api.es.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PaginationTest {

	@Test
	public void limitZeroIsInvalid() {
		Pagination pagination = new Pagination(1, 0, null, null);

		assertTrue(pagination.hasErrors());
		assertEquals(
			"'limit' request parameter invalid: Found [0].  It has to be an integer number greater than 0",
			pagination.getErrors().get(0));
	}

	@Test
	public void offsetUsesCheckedArithmetic() {
		assertEquals(2147483646, new Pagination(2147483647, 1, null, null).getOffset());
		assertThrows(ArithmeticException.class,
			() -> new Pagination(2147483647, 2, null, null).getOffset());
	}

	@Test
	public void largeInternalPageOneDownloadLimitsRemainValid() {
		Pagination pagination = new Pagination(1, 250000, null, null);

		assertFalse(pagination.hasErrors());
		assertEquals(Integer.valueOf(250000), pagination.getLimit());
		assertEquals(0, pagination.getOffset());
	}
}
