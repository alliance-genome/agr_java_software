package org.alliancegenome.indexer.indexers;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Jackson MixIn applied to AuditedObject during ES indexing only. SCRUM-6035: drop curation-internal
 * fields (internal, obsolete, dbDateCreated, dbDateUpdated, dateCreated, dateUpdated) from every public
 * ES document — they leak via nested entities (Gene, Allele, Variant, etc.) because most indexers serialize
 * without an explicit @JsonView. Adding these field-level @JsonIgnore overrides via a MixIn suppresses
 * them globally on the indexer's ObjectMapper without modifying agr_curation entity annotations.
 */
public abstract class AuditedObjectIndexerMixin {

	@JsonIgnore
	Boolean internal;

	@JsonIgnore
	Boolean obsolete;

	@JsonIgnore
	OffsetDateTime dateCreated;

	@JsonIgnore
	OffsetDateTime dateUpdated;

	@JsonIgnore
	OffsetDateTime dbDateCreated;

	@JsonIgnore
	OffsetDateTime dbDateUpdated;
}