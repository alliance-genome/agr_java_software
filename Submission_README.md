# Submission System

The submission system will be used for submitting all data files to AGR.
The rest endpoint: `http://www.alliancegenome.org/api/data/submit` is using a multipart post.

Here is an example using curl:

```
curl \
	-H "api_access_token: 2C07D715..." \
	-X POST "http://www.alliancegenome.org/api/data/submit" \
	-F "SchemaVersion-DataType-TaxonId=@/full/path/to/file1.json" \
	-F "SchemaVersion-DataType-TaxonId=@/full/path/to/file2.json"
```

Valid values for SchemaVersion, DataType, and TaxonId can be found in the examples below.

## Contents

- [Schema Version](#schema-version)
- [Data Type](#data-type)
- [TaxonId](#TaxonId)
- [Examples](#examples)
  * [Schema-DataType-TaxonId String format](#schema-dataType-taxonid-string-format)
  * [Valid examples for submitting files](#valid-examples-for-submitting-files)
- [Return object](#return-object)
  * [Success example](#success-example)
  * [Failed example](#failed-example)

## API Access Token

This will be a key that is generated for the DQM's to use for uploading files.

## Schema Version

| Schema Version |
| --- |
| 0.6.0 |
| 0.6.1 |
| 0.7.0 |
| etc... |

This will be the current release of the schema can be found in the [releases](https://github.com/alliance-genome/agr_schemas/releases) section for the schema repository. Schema does not follow the same release schedule as the main branches.

## Data Type

| Data Type | What it means | Schema Validation File | Format | TaxonId Required | Validation Required |
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

## TaxonId

| Taxon Id | Mod Id | Mod Name |
| --- | --- | --- |
| 7227 | FB | Fly Base |
| 9606 | Human | Human Supplied by RGD |
| 10090 | MGD | Mouse Genome Database |
| 10116 | RGD | Rat Genome Database |
| 4932 | SGD | Saccharomyces Genome Database |
| 6239 | WB | Worm Base |
| 7955 | ZFIN | Zebrafish Information Network |

## Examples

### SchemaVersion-DataType-TaxonId String format

Valid combinations for Schema-DataType-TaxonId are as follows:

| Type | What does it mean? |
| --------------- | --- |
| SchemaVersion-DataType-TaxonId | Validation will occur for BGI, DOA, ORTHO, FEATURE and not for GOA and GFF, all files will be stored under the Schema Directory in S3. |
| DataType-TaxonId | Validation will occur for BGI, DOA, ORTHO, FEATURE and not for GOA and GFF, the current schema version will get looked up from Github. All files will be stored under the Schema Directory in S3.
| DataType | This is only valid for GO, SO, DO. These files will not get validated, but the current schema will get looked up from Github and these files will get stored under the Schema Directory in S3. |
| SchemaVersion-DataType | Invalid (Data Type not found for: SchemaVersion) |
| DataType-TaxonId | Invalid for GO, SO, DO. (TaxonId is not required for this data type)  |

### Valid examples for submitting files

#### One file at a time

The following examples are with the API Access Token omitted `-H "api_access_token: 2C07D715..."`

	> curl -X POST "http://www.alliancegenome.org/api/data/submit" -F "0.7.0-GFF-10090=@MGI_1.0.4_GFF.gff"
	> curl -X POST "http://www.alliancegenome.org/api/data/submit" -F "0.6.2-FEATURE-10090=@MGI_1.0.4_feature.json"
	> curl -X POST "http://www.alliancegenome.org/api/data/submit" -F "0.6.1-BGI-7227=@FB_1.0.4_BGI.json"
	
	> curl -X POST "http://www.alliancegenome.org/api/data/submit" -F "GOA-10090=@gene_association_1.0.mgi.gaf"
	> curl -X POST "http://www.alliancegenome.org/api/data/submit" -F "FEATURE-7955=@ZFIN_1.0.4_feature.json"
	
	> curl -X POST "http://www.alliancegenome.org/api/data/submit" -F "GO=@go_1.0.obo"
	
#### Multiple files at a time

	> curl
		-H "api_access_token: 2C07D715..." \
		-X POST "http://www.alliancegenome.org/api/data/submit" \
		-F "0.7.0-BGI-7227=@FB_1.0.4_BGI.json" \
		-F "0.7.0-FEATURE-7227=@FB_1.0.4_feature.json" \
		-F "0.7.0-DOA-7227=@FB_1.0.4_disease.json" \
		-F "0.7.0-GFF-7227=@FB_1.0.4_GFF.gff"
		
	> curl
		-H "api_access_token: 2C07D715..." \
		-X POST "http://www.alliancegenome.org/api/data/submit" \
		-F "BGI-7227=@FB_1.0.4_BGI.json" \
		-F "FEATURE-7227=@FB_1.0.4_feature.json" \
		-F "DOA-7227=@FB_1.0.4_disease.json" \
		-F "GFF-7227=@FB_1.0.4_GFF.gff"	


## Return object

The responce object that is returned will be based on the files that were submitted.

### Success example

For the following command:

	> curl \
		-H "api_access_token: 2C07D715..." \
		-X POST "http://www.alliancegenome.org/api/data/submit" \
		-F "0.7.0-BGI-7227=@FB_1.0.4_BGI.json" \
		-F "0.7.0-FEATURE-7227=@FB_1.0.4_feature.json" \
		-F "0.7.0-DOA-7227=@FB_1.0.4_disease.json" \
		-F "0.7.0-GFF-7227=@FB_1.0.4_GFF.gff"

<details>
<summary>View Response</summary>
<pre>
```{
	"fileStatus": {
		"0.7.0-BGI-7227":"success",
		"0.7.0-DOA-7227":"success",
		"0.7.0-GFF-7227":"success",
		"0.7.0-FEATURE-7227":"success"
	},
	"status":"success"
}```
</details>

### Failed example

For the following command (Missing API Access Token):

	> curl \
		-X POST "http://www.alliancegenome.org/api/data/submit" \
		-F "0.7.0-BGI-10090=@MGI_1.0.4_BGI.json" \
		-F "0.7.0-FEATURE-10090=@MGI_1.0.4_feature.json" \
		-F "0.7.0-DOA-10090=@MGI_1.0.4_disease.json" \
		-F "0.7.0-GFF-10090=@MGI_1.0.4_GFF.gff" 

<details>
<summary>View Failure Response</summary>
<pre>
```{
	"fileStatus": {
		"0.7.0-BGI-10090":"Authentication Failure: Please check your api_access_token",
		"0.7.0-FEATURE-10090":"Authentication Failure: Please check your api_access_token",
		"0.7.0-DOA-10090":"Authentication Failure: Please check your api_access_token",
		"0.7.0-GFF-10090":"Authentication Failure: Please check your api_access_token"
	},
	"status":"failed"
}```</pre>
</details>

For the following command (Errors in BGI):

	> curl \
		-H "api_access_token: 2C07D715..." \
		-X POST "http://www.alliancegenome.org/api/data/submit" \
		-F "0.7.0-BGI-10090=@MGI_1.0.4_BGI.json" \
		-F "0.7.0-FEATURE-10090=@MGI_1.0.4_feature.json" \
		-F "0.7.0-DOA-10090=@MGI_1.0.4_disease.json" \
		-F "0.7.0-GFF-10090=@MGI_1.0.4_GFF.gff" 

<details>
<summary>View Failure Response</summary>
<pre>
```{
	"fileStatus": {
		"0.7.0-FEATURE-10090":"success",
		"0.7.0-BGI-10090":"string \"https://en.wikipedia.org/wiki/Cathepsin L2\" is not a valid URI",
		"0.7.0-DOA-10090":"success",
		"0.7.0-GFF-10090":"Unable to complete multi-part upload. Individual part upload failed : Your socket connection to the server was not read from or written to within the timeout period. Idle connections will be closed. (Service: Amazon S3; Status Code: 400; Error Code: RequestTimeout; Request ID: 3ABBDFD90F0C4CAA)"
	},
	"status":"failed"
}```</pre>
</details>
	
In a failed example only the files that failed need to be attempted again:

	> curl \
		-H "api_access_token: 2C07D715..." \
		-X POST "http://www.alliancegenome.org/api/data/submit" \
		-F "0.7.0-BGI-10090=@MGI_1.0.4_BGI.json" \
		-F "0.7.0-GFF-10090=@MGI_1.0.4_GFF.gff" 

<details>
<summary>View Success Response</summary>
<pre>
```{
	"fileStatus": {
		"0.7.0-BGI-10090":"success",
		"0.7.0-GFF-10090":"success"
	},
	"status":"success"
}```
</details>
