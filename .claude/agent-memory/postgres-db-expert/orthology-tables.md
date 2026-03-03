# Orthology Table Schema Details

## genetogeneorthology (parent table)
```
id             | bigint    | PK
datecreated    | timestamp
dateupdated    | timestamp
dbdatecreated  | timestamp
dbdateupdated  | timestamp
internal       | boolean   | default false
obsolete       | boolean   | default false
createdby_id   | bigint    | FK -> person(id)
updatedby_id   | bigint    | FK -> person(id)
subjectgene_id | bigint    | FK -> gene(id)
objectgene_id  | bigint    | FK -> gene(id)
```
Indexes: PK, createdby, objectgene, subjectgene, updatedby

## genetogeneorthologygenerated (child table, JOINED inheritance)
```
id                    | bigint  | PK, FK -> genetogeneorthology(id)
isbestscore_id        | bigint  | FK -> vocabularyterm(id)
isbestscorereverse_id | bigint  | FK -> vocabularyterm(id)
confidence_id         | bigint  | FK -> vocabularyterm(id)
strictfilter          | boolean
moderatefilter        | boolean
```
Indexes: PK, confidence, isbestscore, isbestscorereverse, composite(isbestscore,isbestscorereverse) partial

## Join Tables (all have indexes on both FK columns)
- genetogeneorthologygenerated_predictionmethodsmatched (genetogeneorthologygenerated_id, predictionmethodsmatched_id)
- genetogeneorthologygenerated_predictionmethodsnotmatched (genetogeneorthologygenerated_id, predictionmethodsnotmatched_id)
- genetogeneorthologygenerated_predictionmethodsnotcalled (genetogeneorthologygenerated_id, predictionmethodsnotcalled_id)
