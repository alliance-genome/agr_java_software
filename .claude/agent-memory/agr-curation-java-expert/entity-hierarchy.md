# Entity Hierarchy Notes

## Gene hierarchy (JPA JOINED inheritance)
AuditedObject (@MappedSuperclass) -> id, createdBy, updatedBy, internal, obsolete, dates
  CurieObject (@MappedSuperclass) -> curie
    SubmittedObject (@MappedSuperclass) -> primaryExternalId, modInternalId, dataProvider, dataProviderCrossReference, relatedNotes, getIdentifier()
      BiologicalEntity (@Entity, JOINED) -> taxon (NCBITaxonTerm)
        GenomicEntity (@Entity, JOINED) -> crossReferences
          Gene (@Entity, JOINED) -> geneSymbol, geneType, geneFullName, etc.

## Orthology hierarchy (JPA JOINED inheritance)
AuditedObject (@MappedSuperclass)
  GeneToGeneOrthology (@Entity, JOINED) -> subjectGene (Gene), objectGene (Gene)
    GeneToGeneOrthologyGenerated (@Entity, JOINED) -> isBestScore, isBestScoreReverse, confidence (VocabularyTerm), strictFilter, moderateFilter (Boolean), predictionMethodsMatched/NotMatched/NotCalled (List<VocabularyTerm>)

## DB Tables for orthology
- genetogeneorthology: id, subjectgene_id, objectgene_id, createdby_id, updatedby_id, internal, obsolete, dates
- genetogeneorthologygenerated: id (FK to genetogeneorthology), isbestscore_id, isbestscorereverse_id, confidence_id, strictfilter, moderatefilter
- genetogeneorthologygenerated_predictionmethodsmatched: genetogeneorthologygenerated_id, predictionmethodsmatched_id
- genetogeneorthologygenerated_predictionmethodsnotmatched: genetogeneorthologygenerated_id, predictionmethodsnotmatched_id
- genetogeneorthologygenerated_predictionmethodsnotcalled: genetogeneorthologygenerated_id, predictionmethodsnotcalled_id

## Key: VocabularyTerm has `name` and `abbreviation` fields, both shown in GeneToGeneOrthologyDocument view
