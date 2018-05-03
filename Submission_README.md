# Submission System

The submission system will be used for submitting all data files to AGR.
The rest endpoint: `http://www.alliancegenome.org/api/data/submit` is using a multipart post.

Here is an example using curl:

```
curl \
	-H "api_access_token: 2C07D715..." \
	-X POST "http://www.alliancegenome.org/api/data/submit" \
	-F "SchemaVersion_DataType_TaxonId=@/full/path/to/file1.json" \
	-F "SchemaVersion_DataType_TaxonId=@/full/path/to/file2.json"
```

Valid values for SchemaVersion, DataType, and TaxonId can be found in the examples below.

## Contents

- [Schema Version](#schema-version)
- [Data Type](#data-type)
- [TaxonId](#taxonid)
- [Examples](#examples)
  * [SchemaVersion DataType TaxonId String format](#schemaversion-datatype-taxonid-string-format)
  * [Valid examples for submitting files](#valid-examples-for-submitting-files)
- [Return object](#return-object)
  * [Success example](#success-example)
  * [Failed example](#failed-example)
- [Loader](#loader)
  * [Releases](#releases)
  * [Snap Shot](#snap-shot)
  * [Take Snap Shot](#take-snap-shot)

## API Access Token

This will be a key that is generated for the DQM's to use for uploading files.

## Schema Version

| Schema Version |
| --- |
| 0.6.0.0 |
| 0.6.1.0 |
| 0.7.0.0 |
| 1.0.0.0 |
| etc... |

This will be the current release of the schema can be found in the [releases](https://github.com/alliance-genome/agr_schemas/releases) section for the schema repository. Schema does not follow the same release schedule as the main branches.

## Data Type

| Data Type | What it means | Schema Validation File | Format | TaxonId Required | Validation Required |
| --- | --- | --- | --- | --- | --- |
| BGI | Basic Gene information | geneMetadata.json | json | true | true |
| DAF | Disease Annotations File (DAF) | diseaseMetaDataDefinition.json | json | true | true |
| ORTHO | Orthology File | orthoHeader.json | json | true | true |
| ALLELE | Allele File | alleleMetadata.json | json | true | true |
| GAF | Gene Annotations File (GAF) | - | gaf | true | false |
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

### SchemaVersion DataType TaxonId String format

Valid combinations for Schema-DataType-TaxonId are as follows:

| Type | What does it mean? |
| --------------- | --- |
| SchemaVersion\_DataType\_TaxonId | Validation will occur for BGI, DAF, ORTHO, FEATURE and not for GAF and GFF, all files will be stored under the Schema Directory in S3. |
| DataType\_TaxonId | Validation will occur for BGI, DAF, ORTHO, FEATURE and not for GAF and GFF, the current schema version will get looked up from Github. All files will be stored under the Schema Directory in S3. Invalid for GO, SO, DO. (TaxonId is not required for this data type)
| DataType | This is only valid for GO, SO, DO. These files will not get validated, but the current schema will get looked up from Github and these files will get stored under the Schema Directory in S3. |
| SchemaVersion-DataType | Invalid (Data Type not found for: SchemaVersion) |

### Valid examples for submitting files

#### One file at a time

	> curl \
		-H "api_access_token: 2C07D715..." \
		-X POST "http://www.alliancegenome.org/api/data/submit" \
		-F "0.7.0_GFF_10090=@MGI_1.0.4_GFF.gff"
	> curl \
		-H "api_access_token: 2C07D715..." \
		-X POST "http://www.alliancegenome.org/api/data/submit" \
		-F "0.6.2_FEATURE_10090=@MGI_1.0.4_feature.json"
	> curl \
		-H "api_access_token: 2C07D715..." \
		-X POST "http://www.alliancegenome.org/api/data/submit" \
		-F "0.6.1_BGI_7227=@FB_1.0.4_BGI.json"
	> curl \
		-H "api_access_token: 2C07D715..." \
		-X POST "http://www.alliancegenome.org/api/data/submit" \
		-F "GAF_10090=@gene_association_1.0.mgi.gaf"
	> curl \
		-H "api_access_token: 2C07D715..." \
		-X POST "http://www.alliancegenome.org/api/data/submit" \
		-F "FEATURE_7955=@ZFIN_1.0.4_feature.json"
	> curl \
		-H "api_access_token: 2C07D715..." \
		-X POST "http://www.alliancegenome.org/api/data/submit" \
		-F "GO=@go_1.0.obo"
	
#### Multiple files at a time

	> curl \
		-H "api_access_token: 2C07D715..." \
		-X POST "http://www.alliancegenome.org/api/data/submit" \
		-F "0.7.0_BGI_7227=@FB_1.0.4_BGI.json" \
		-F "0.7.0_FEATURE_7227=@FB_1.0.4_feature.json" \
		-F "0.7.0_DAF_7227=@FB_1.0.4_disease.json" \
		-F "0.7.0_GFF_7227=@FB_1.0.4_GFF.gff"
		
	> curl \
		-H "api_access_token: 2C07D715..." \
		-X POST "http://www.alliancegenome.org/api/data/submit" \
		-F "BGI_7227=@FB_1.0.4_BGI.json" \
		-F "FEATURE_7227=@FB_1.0.4_feature.json" \
		-F "DAF_7227=@FB_1.0.4_disease.json" \
		-F "GFF_7227=@FB_1.0.4_GFF.gff"	


## Return object

The responce object that is returned will be based on the files that were submitted.

### Success example

For the following command:

	> curl \
		-H "api_access_token: 2C07D715..." \
		-X POST "http://www.alliancegenome.org/api/data/submit" \
		-F "0.7.0_BGI_7227=@FB_1.0.4_BGI.json" \
		-F "0.7.0_FEATURE_7227=@FB_1.0.4_feature.json" \
		-F "0.7.0_DAF_7227=@FB_1.0.4_disease.json" \
		-F "0.7.0_GFF_7227=@FB_1.0.4_GFF.gff"

<details>
<summary>View Response</summary>
<pre>
```{
	"fileStatus": {
		"0.7.0_BGI_7227":"success",
		"0.7.0_DAF_7227":"success",
		"0.7.0_GFF_7227":"success",
		"0.7.0_FEATURE_7227":"success"
	},
	"status":"success"
}```
</details>

### Failed example

For the following command (Missing API Access Token):

	> curl \
		-X POST "http://www.alliancegenome.org/api/data/submit" \
		-F "0.7.0_BGI_10090=@MGI_1.0.4_BGI.json" \
		-F "0.7.0_FEATURE_10090=@MGI_1.0.4_feature.json" \
		-F "0.7.0_DAF_10090=@MGI_1.0.4_disease.json" \
		-F "0.7.0_GFF_10090=@MGI_1.0.4_GFF.gff" 

<details>
<summary>View Failure Response</summary>
<pre>
{
	"fileStatus": {
		"0.7.0_BGI_10090":"Authentication Failure: Please check your api_access_token",
		"0.7.0_FEATURE_10090":"Authentication Failure: Please check your api_access_token",
		"0.7.0_DAF_10090":"Authentication Failure: Please check your api_access_token",
		"0.7.0_GFF_10090":"Authentication Failure: Please check your api_access_token"
	},
	"status":"failed"
}</pre>
</details>

For the following command (Errors in BGI):

	> curl \
		-H "api_access_token: 2C07D715..." \
		-X POST "http://www.alliancegenome.org/api/data/submit" \
		-F "0.7.0_BGI_10090=@MGI_1.0.4_BGI.json" \
		-F "0.7.0_FEATURE_10090=@MGI_1.0.4_feature.json" \
		-F "0.7.0_DAF_10090=@MGI_1.0.4_disease.json" \
		-F "0.7.0_GFF_10090=@MGI_1.0.4_GFF.gff" 

<details>
<summary>View Failure Response</summary>
<pre>
{
	"fileStatus": {
		"0.7.0_FEATURE_10090":"success",
		"0.7.0_BGI_10090":"string \"https://en.wikipedia.org/wiki/Cathepsin L2\" is not a valid URI",
		"0.7.0_DAF_10090":"success",
		"0.7.0_GFF_10090":"Unable to complete multi-part upload. Individual part upload failed : Your socket connection to the server was not read from or written to within the timeout period. Idle connections will be closed. (Service: Amazon S3; Status Code: 400; Error Code: RequestTimeout; Request ID: 3ABBDFD90F0C4CAA)"
	},
	"status":"failed"
}</pre>
</details>
	
In a failed example only the files that failed need to be attempted again:

	> curl \
		-H "api_access_token: 2C07D715..." \
		-X POST "http://www.alliancegenome.org/api/data/submit" \
		-F "0.7.0_BGI_10090=@MGI_1.0.4_BGI.json" \
		-F "0.7.0_GFF_10090=@MGI_1.0.4_GFF.gff" 

<details>
<summary>View Success Response</summary>
<pre>
{
	"fileStatus": {
		"0.7.0_BGI_10090":"success",
		"0.7.0_GFF_10090":"success"
	},
	"status":"success"
}</pre>
</details>

## Loader

The loader will run against the snapshot and releases API's, using the API Access Token for the "take snapshot" endpoint. One extra optional parameter is "system" which desinates the pipeline that will be used for releasing data. If the parameter is omited then it will be assumed value of "production". Links to download these files will be in the following format:

	https://s3.amazonaws.com/mod-datadumps/<path>

### Releases

The following command can be used to pull a list of releases from the system that are available:
	
	> curl "http://www.alliancegenome.org/api/data/releases"

<details>
<summary>View Success Response</summary>
<pre>
{
    "1.4.0.0": 1523284823719,
    "1.0.0.0": 1523284848246,
    "1.3.0.0": 1523284837284
}</pre>
</details>

### Snap Shot

The following command, can be used to pull a specific SnapShot by release version, release version is required.

	> curl "http://www.alliancegenome.org/api/data/snapshot?releaseVersion=1.4.0.0"

<details>
<summary>View Success Response</summary>
<pre>
{
    "releaseVersion": "1.4.0.0",
    "schemaVersion": "1.0.0.2",
    "system": "production",
    "snapShotDate": 1523284823719,
    "dataFiles": [
        {
            "schemaVersion": "1.0.0.2",
            "dataType": "ALLELE",
            "taxonId": "10090",
            "path": "1.0.0.2/ALLELE/10090/1.0.0.2\_ALLELE\_10090\_0.json",
            "uploadDate": 1522181792721
        },
        {
            "schemaVersion": "1.0.0.2",
            "dataType": "DAF",
            "taxonId": "10090",
            "path": "1.0.0.2/DAF/10090/1.0.0.2\_DAF\_10090\_0.json",
            "uploadDate": 1522181816273
        },
        {
            "schemaVersion": "1.0.0.2",
            "dataType": "ALLELE",
            "taxonId": "7955",
            "path": "1.0.0.2/ALLELE/7955/1.0.0.2\_ALLELE\_7955\_2.json",
            "uploadDate": 1522179715428
        },
        {
            "schemaVersion": "1.0.0.2",
            "dataType": "BGI",
            "taxonId": "7955",
            "path": "1.0.0.2/BGI/7955/1.0.0.2\_BGI\_7955\_1.json",
            "uploadDate": 1522181715592
        },
        {
            "schemaVersion": "1.0.0.2",
            "dataType": "GFF",
            "taxonId": "7955",
            "path": "1.0.0.2/GFF/7955/1.0.0.2\_GFF\_7955\_1.gff",
            "uploadDate": 1522181475376
        },
        {
            "schemaVersion": "1.0.0.2",
            "dataType": "DAF",
            "taxonId": "7955",
            "path": "1.0.0.2/DAF/7955/1.0.0.2\_DAF\_7955\_1.json",
            "uploadDate": 1522180298184
        }
    ]
}</pre>
</details>

### Take Snap Shot

This will take a snapshot of all the latest datafiles for each Taxon Id by each DataType. 

	> curl -H "api_access_token: 2C07D715..." \
	"http://www.alliancegenome.org/api/data/takesnapshot?system=production&releaseVersion=1.4.0.0"

<details>
<summary>View Success Response</summary>
<pre>
{
    "releaseVersion": "1.4.0.0",
    "schemaVersion": "1.0.0.2",
    "system": "production",
    "snapShotDate": 1523284823719,
    "dataFiles": [
        {
            "schemaVersion": "1.0.0.2",
            "dataType": "ALLELE",
            "taxonId": "10090",
            "path": "1.0.0.2/ALLELE/10090/1.0.0.2\_ALLELE\_10090\_0.json",
            "uploadDate": 1522181792721
        },
        {
            "schemaVersion": "1.0.0.2",
            "dataType": "DAF",
            "taxonId": "10090",
            "path": "1.0.0.2/DAF/10090/1.0.0.2\_DAF\_10090\_0.json",
            "uploadDate": 1522181816273
        },
        {
            "schemaVersion": "1.0.0.2",
            "dataType": "ALLELE",
            "taxonId": "7955",
            "path": "1.0.0.2/ALLELE/7955/1.0.0.2\_ALLELE\_7955\_2.json",
            "uploadDate": 1522179715428
        },
        {
            "schemaVersion": "1.0.0.2",
            "dataType": "BGI",
            "taxonId": "7955",
            "path": "1.0.0.2/BGI/7955/1.0.0.2\_BGI\_7955\_1.json",
            "uploadDate": 1522181715592
        },
        {
            "schemaVersion": "1.0.0.2",
            "dataType": "GFF",
            "taxonId": "7955",
            "path": "1.0.0.2/GFF/7955/1.0.0.2\_GFF\_7955\_1.gff",
            "uploadDate": 1522181475376
        },
        {
            "schemaVersion": "1.0.0.2",
            "dataType": "DAF",
            "taxonId": "7955",
            "path": "1.0.0.2/DAF/7955/1.0.0.2\_DAF\_7955\_1.json",
            "uploadDate": 1522180298184
        }
    ]
}</pre>
</details>



