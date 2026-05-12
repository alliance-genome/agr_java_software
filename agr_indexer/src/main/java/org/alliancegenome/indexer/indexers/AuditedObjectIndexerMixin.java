package org.alliancegenome.indexer.indexers;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Jackson MixIn applied to AuditedObject during ES indexing only. SCRUM-6035: drop curation-internal
 * fields (internal, obsolete, dbDateCreated, dbDateUpdated) from every public ES document — they leak
 * via nested entities (Gene, Allele, Variant, etc.) because most indexers serialize without an explicit
 * @JsonView.
 *
 * Annotation form: both field-level and getter-level @JsonIgnore are needed because Lombok @Data
 * generates a public getter, and Jackson combines field+getter annotations for property resolution.
 * Marking only the field leaves the getter visible.
 */
public abstract class AuditedObjectIndexerMixin {

	@JsonIgnore
	Boolean internal;

	@JsonIgnore
	Boolean obsolete;

	@JsonIgnore
	OffsetDateTime dbDateCreated;

	@JsonIgnore
	OffsetDateTime dbDateUpdated;

	@JsonIgnore
	public abstract Boolean getInternal();

	@JsonIgnore
	public abstract Boolean getObsolete();

	@JsonIgnore
	public abstract OffsetDateTime getDbDateCreated();

	@JsonIgnore
	public abstract OffsetDateTime getDbDateUpdated();
}
