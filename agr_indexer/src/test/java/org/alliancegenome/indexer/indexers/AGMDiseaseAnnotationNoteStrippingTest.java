package org.alliancegenome.indexer.indexers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.alliancegenome.curation_api.model.entities.AGMDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.AffectedGenomicModel;
import org.alliancegenome.curation_api.model.entities.Note;
import org.alliancegenome.curation_api.model.entities.ontology.DOTerm;
import org.junit.Test;

/**
 * SCRUM-6327: MGI:7596100 is one of the 128 AGMs (per the curation DB pull) whose disease
 * annotation carries internal=true disease_note notes - 3 of them, the most of any AGM in the
 * report. Verifies DiseaseAnnotationCurationIndexer's note-stripping calls
 * (stripInternalNotes(da.getRelatedNotes()) / stripInternalNotes(biologicalEntity.getRelatedNotes())
 * from populateBaseDiseaseAnnotationDocument) remove all 3 internal notes while leaving a public
 * note and the annotation itself (subject, disease object) intact - annotation passes through,
 * only the internal notes don't.
 */
public class AGMDiseaseAnnotationNoteStrippingTest {

	private static final String AGM_CURIE = "MGI:7596100";

	@Test
	public void stripInternalNotesRemovesAllThreeInternalDiseaseNotesButKeepsAnnotationAndPublicNote() {
		AffectedGenomicModel agm = new AffectedGenomicModel();
		agm.setCurie(AGM_CURIE);

		DOTerm disease = new DOTerm();
		disease.setCurie("DOID:13994");
		disease.setName("cleidocranial dysplasia");

		AGMDiseaseAnnotation da = new AGMDiseaseAnnotation();
		da.setDiseaseAnnotationSubject(agm);
		da.setDiseaseAnnotationObject(disease);

		List<Note> notes = new ArrayList<>();
		notes.add(internalDiseaseNote("internal note 1 of 3"));
		notes.add(internalDiseaseNote("internal note 2 of 3"));
		notes.add(internalDiseaseNote("internal note 3 of 3"));
		notes.add(publicDiseaseNote("public disease note"));
		da.setRelatedNotes(notes);

		// Mirrors DiseaseAnnotationCurationIndexer.populateBaseDiseaseAnnotationDocument:
		// stripInternalNotes(da.getRelatedNotes()); stripInternalNotes(biologicalEntity.getRelatedNotes());
		// followed by dad.addPrimaryAnnotation(da) - the annotation itself is never dropped.
		Indexer.stripInternalNotes(da.getRelatedNotes());
		Indexer.stripInternalNotes(agm.getRelatedNotes());

		assertEquals("all 3 internal notes should be removed, public note kept", 1, da.getRelatedNotes().size());
		Note remaining = da.getRelatedNotes().get(0);
		assertFalse("remaining note must not be internal", Boolean.TRUE.equals(remaining.getInternal()));
		assertEquals("public disease note", remaining.getFreeText());

		// The annotation itself - subject and disease object - must still be intact and indexable.
		assertEquals(AGM_CURIE, da.getDiseaseAnnotationSubject().getCurie());
		assertEquals("DOID:13994", da.getDiseaseAnnotationObject().getCurie());
	}

	@Test
	public void stripInternalNotesHandlesAgmWithNoNotes() {
		AffectedGenomicModel agm = new AffectedGenomicModel();
		agm.setCurie(AGM_CURIE);

		Indexer.stripInternalNotes(agm.getRelatedNotes());

		assertNull(agm.getRelatedNotes());
	}

	private Note internalDiseaseNote(String text) {
		Note note = new Note();
		note.setFreeText(text);
		note.setInternal(true);
		return note;
	}

	private Note publicDiseaseNote(String text) {
		Note note = new Note();
		note.setFreeText(text);
		note.setInternal(false);
		return note;
	}

}
