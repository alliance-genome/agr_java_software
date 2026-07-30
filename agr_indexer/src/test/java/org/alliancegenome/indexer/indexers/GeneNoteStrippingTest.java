package org.alliancegenome.indexer.indexers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;

import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.curation_api.model.entities.Note;
import org.junit.Test;

/**
 * SCRUM-6327: WB:WBsf019098, WBsf019099, WBsf019217, WBsf019218, WBsf982337 are the Gene records
 * flagged (in the ticket comments and the curation DB pull) as carrying an internal=true
 * private_comment note that leaked into GeneSummaryDocument. Verifies Indexer.stripInternalNotes
 * removes exactly that note - the same call GeneSummaryCurationIndexer makes on
 * doc.getGene().getRelatedNotes() before indexing - while leaving public notes and the gene itself
 * untouched. WB:WBsf014559, the original example from the ticket description, gets its own test
 * below since it's modeled with two internal notes instead of one.
 */
public class GeneNoteStrippingTest {

	private static final List<String> WBSF_GENE_CURIES = List.of("WB:WBsf019098", "WB:WBsf019099", "WB:WBsf019217", "WB:WBsf019218", "WB:WBsf982337");

	@Test
	public void stripInternalNotesRemovesPrivateCommentButKeepsPublicNoteAndGene() {
		for (String curie : WBSF_GENE_CURIES) {
			Gene gene = buildGeneWithNotes(curie);

			Indexer.stripInternalNotes(gene.getRelatedNotes());

			assertEquals("only the internal note should be removed for " + curie, 1, gene.getRelatedNotes().size());
			Note remaining = gene.getRelatedNotes().get(0);
			assertFalse("remaining note must not be internal for " + curie, Boolean.TRUE.equals(remaining.getInternal()));
			assertEquals("public note text should survive untouched for " + curie, "public annotation note", remaining.getFreeText());
			assertEquals("gene identity must be unaffected by note stripping", curie, gene.getCurie());
		}
	}

	/**
	 * WB:WBsf014559 is the original example from the ticket description. Modeled here with two
	 * distinct internal notes (not just the one private_comment already covered above) to prove
	 * stripInternalNotes removes every internal note on the gene, not just a single expected one.
	 */
	@Test
	public void stripInternalNotesRemovesAllInternalNotesForWBsf014559() {
		String curie = "WB:WBsf014559";
		Gene gene = new Gene();
		gene.setCurie(curie);

		Note privateComment = new Note();
		privateComment.setFreeText("private_comment - do not expose");
		privateComment.setInternal(true);

		Note anotherInternalNote = new Note();
		anotherInternalNote.setFreeText("a different internal note - also must not be exposed");
		anotherInternalNote.setInternal(true);

		Note publicNote = new Note();
		publicNote.setFreeText("public annotation note");
		publicNote.setInternal(false);

		List<Note> notes = new ArrayList<>();
		notes.add(privateComment);
		notes.add(anotherInternalNote);
		notes.add(publicNote);
		gene.setRelatedNotes(notes);

		Indexer.stripInternalNotes(gene.getRelatedNotes());

		assertEquals("both internal notes should be removed, only the public one kept", 1, gene.getRelatedNotes().size());
		Note remaining = gene.getRelatedNotes().get(0);
		assertFalse("remaining note must not be internal", Boolean.TRUE.equals(remaining.getInternal()));
		assertEquals("public annotation note", remaining.getFreeText());
		assertEquals("gene identity must be unaffected by note stripping", curie, gene.getCurie());
	}

	@Test
	public void stripInternalNotesIsNullAndEmptySafe() {
		Indexer.stripInternalNotes(null);
		Indexer.stripInternalNotes(new ArrayList<>());
	}

	private Gene buildGeneWithNotes(String curie) {
		Gene gene = new Gene();
		gene.setCurie(curie);

		Note privateComment = new Note();
		privateComment.setFreeText("private comment - do not expose");
		privateComment.setInternal(true);

		Note publicNote = new Note();
		publicNote.setFreeText("public annotation note");
		publicNote.setInternal(false);

		List<Note> notes = new ArrayList<>();
		notes.add(privateComment);
		notes.add(publicNote);
		gene.setRelatedNotes(notes);

		return gene;
	}

}
