# Submission System

The submission system will be used for submitting all data files to AGR.
The rest endpoint: `http://www.alliancegenome.org/api/data/submit` is using a multipart post.

Here is an example using curl:

`curl -X POST "http://www.alliancegenome.org/api/data/submit" -F "Schema-DataType-Mod=@/full/path/to/file.json"`

Valid values for Schema DataType and Mod can be found in the following sections.

## Schema Version

| Schema Version |
| --- |
| 0.6.0 |
| 0.6.1 |
| 0.7.0 |
| etc... |

This will be the current release of the schema can be found in the [releases](https://github.com/alliance-genome/agr_schemas/releases) section for the schema repository. Schema does not follow the same release schedule as the main branches.

## Data Type

| Data Type | What it means | Schema Validation File | Format | Mod Required | Validation Required |
| --- | --- | --- | --- | --- | --- |
| BGI | Basic Gene information | basicGeneInfoFile.json | json | true | true |
| DOA | Disease Term Annotations (DAF) | diseaseMetaDataDefinition.json | json | true | true |
| ORTHO | Orthology File | orthoHeader.json | json | true | true |
| FEATURE | Feature File | featureMetadata.json | json | true | true |
| GOA | Gene Ontology Annotations (GAF) | - | gaf | true | false |
| GFF | Gene Feature File | - | gff | true | false |
| DO | Disease Ontology File | - | obo | false | false |
| SO | Sequence Ontology File | - | obo | false | false |
| GO | Gene Ontology File | - | obo | false | false |

## Mod

| Mod | Mod name |
| --- | --- |
| FB | Fly Base |
| Human | Human Supplied by RGD |
| MGD | Mouse Genome Database |
| RGD | Rat Genome Database |
| SGD | Saccharomyces Genome Database |
| WB | Worm Base |
| ZFIN | Zebrafish Information Network |


## Schema-DataType-Mod String format

Valid combinations for Schema-DataType-Mode are as follows:

| Type | What does it mean? |
| --------------- | --- |
| Schema-DataType-Mod | Validation will occur for BGI, DOA, ORTHO, FEATURE and not for GOA and GFF, all files will be stored under the Schema Directory in S3. |
| DataType-Mod | Validation will occur for BGI, DOA, ORTHO, FEATURE and not for GOA and GFF, the current schema version will get looked up from Github. All files will be stored under the Schema Directory in S3.
| DataType | This is only valid for GO, SO, DO. These files will not get validated, but the current schema will get looked up from Github and these files will get stored under the Schema Directory in S3. |
| Schema-DataType | Invalid (Data Type not found for: Schema) |
| DataType-Mod | Invalid for GO, SO, DO. (Mod is not required for this data type)  |

## Valid examples for submitting files

### One file at a time

	> curl -X POST "http://www.alliancegenome.org/api/data/submit" -F "0.7.0-GFF-MGD=@MGI_1.0.4_GFF.gff"
	> curl -X POST "http://www.alliancegenome.org/api/data/submit" -F "0.6.2-FEATURE-MGD=@MGI_1.0.4_feature.json"
	> curl -X POST "http://www.alliancegenome.org/api/data/submit" -F "0.6.1-BGI-FB=@FB_1.0.4_BGI.json"
	
	> curl -X POST "http://www.alliancegenome.org/api/data/submit" -F "GOA-MGD=@gene_association_1.0.mgi.gaf"
	> curl -X POST "http://www.alliancegenome.org/api/data/submit" -F "FEATURE-ZFIN=@ZFIN_1.0.4_feature.json"
	
	> curl -X POST "http://www.alliancegenome.org/api/data/submit" -F "GO=@go_1.0.obo"
	
### Multiple files at a time

	> curl -X POST "http://www.alliancegenome.org/api/data/submit" \
		-F "0.7.0-BGI-FB=@FB_1.0.4_BGI.json" \
		-F "0.7.0-FEATURE-FB=@FB_1.0.4_feature.json" \
		-F "0.7.0-DOA-FB=@FB_1.0.4_disease.json" \
		-F "0.7.0-GFF-FB=@FB_1.0.4_GFF.gff"
		
	> curl -X POST "http://www.alliancegenome.org/api/data/submit" \
		-F "BGI-FB=@FB_1.0.4_BGI.json" \
		-F "FEATURE-FB=@FB_1.0.4_feature.json" \
		-F "DOA-FB=@FB_1.0.4_disease.json" \
		-F "GFF-FB=@FB_1.0.4_GFF.gff"	


## Return object

The responce object that is returned will be based on the files that were submitted.

### Success example

For the following command:

	> curl -X POST "http://www.alliancegenome.org/api/data/submit" \
		-F "0.7.0-BGI-FB=@FB_1.0.4_BGI.json" \
		-F "0.7.0-FEATURE-FB=@FB_1.0.4_feature.json" \
		-F "0.7.0-DOA-FB=@FB_1.0.4_disease.json" \
		-F "0.7.0-GFF-FB=@FB_1.0.4_GFF.gff"

This is the responce object:

```json
{
	"fileStatus": {
		"0.7.0-BGI-FB":"success",
		"0.7.0-DOA-FB":"success",
		"0.7.0-GFF-FB":"success",
		"0.7.0-FEATURE-FB":"success"
	},
	"status":"success"
}
```

### Failed example

For the following command:

	> curl -X POST "http://localhost:8080/api/data/submit" \
		-F "0.7.0-BGI-MGD=@MGI_1.0.4_BGI.json" \
		-F "0.7.0-FEATURE-MGD=@MGI_1.0.4_feature.json" \
		-F "0.7.0-DOA-MGD=@MGI_1.0.4_disease.json" \
		-F "0.7.0-GFF-MGD=@MGI_1.0.4_GFF.gff" 

This is the responce object:

```json
{
	"fileStatus": {
		"0.7.0-FEATURE-MGD":"success",
		"0.7.0-BGI-MGD":"string \"https://en.wikipedia.org/wiki/Cathepsin L2\" is not a valid URI",
		"0.7.0-DOA-MGD":"success",
		"0.7.0-GFF-MGD":"Unable to complete multi-part upload. Individual part upload failed : Your socket connection to the server was not read from or written to within the timeout period. Idle connections will be closed. (Service: Amazon S3; Status Code: 400; Error Code: RequestTimeout; Request ID: 3ABBDFD90F0C4CAA)"
	},
	"status":"failed"
}
```
	
In a failed example only the files that failed need to be attempted again:

	> curl -X POST "http://localhost:8080/api/data/submit" \
		-F "0.7.0-BGI-MGD=@MGI_1.0.4_BGI.json" \
		-F "0.7.0-GFF-MGD=@MGI_1.0.4_GFF.gff" 

This is the responce object:

```json
{
	"fileStatus": {
		"0.7.0-BGI-MGD":"success",
		"0.7.0-GFF-MGD":"success"
	},
	"status":"failed"
}
```

