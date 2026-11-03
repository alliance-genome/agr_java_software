package org.alliancegenome.api.service;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.alliancegenome.api.es.query.Pagination;
import org.junit.Test;

public class ESServiceSourceProjectionTest {

	@Test
	public void explicitSourceIncludesReplaceTheDefaultWildcardBeforeTheDaoCall() {
		TestESService service = new TestESService();
		Pagination pagination = new Pagination(1, 20, null, null);
		pagination.setSourceIncludes(List.of("allele.primaryExternalId"));

		assertEquals(List.of("allele.primaryExternalId"), service.responseFields(pagination));
	}

	@Test
	public void absentSourceIncludesRetainExistingFullSourceBehavior() {
		TestESService service = new TestESService();

		assertEquals(List.of("*"), service.responseFields(new Pagination(1, 20, null, null)));
	}

	private static class TestESService extends ESService {
		private List<String> responseFields(Pagination pagination) {
			return getDiseaseSearchResponseFields(pagination);
		}
	}
}
