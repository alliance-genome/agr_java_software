# Phase 1: Module Skeleton + VCF Pass-Through

**Status**: Complete

## Goal

Create the Maven module, read a VCF, write an identical VCF. Proves the build and I/O work.

## Files to Create

### 1. `agr_vep/pom.xml`
- Copy pattern from `agr_variant_indexer/pom.xml`
- Parent: `agr_java_software`, artifactId: `agr_vep`
- Depends on `agr_java_core` (with Quarkus/slf4j-simple exclusions) + `log4j-slf4j2-impl`
- Main class: `org.alliancegenome.vep.Main`
- Fat JAR via `maven-assembly-plugin`

### 2. `pom.xml` (parent)
- Add `<module>agr_vep</module>` to `<modules>` block

### 3. `Dockerfile.agr_vep`
- Copy from `Dockerfile.agr_variant_indexer`, change module name to `agr_vep`

### 4. `agr_vep/src/main/java/org/alliancegenome/vep/Main.java`
- Parse CLI args: `--vcf`, `--gff`, `--fasta`, `--bam` (optional), `--output`, `--mod`
- Instantiate `VcfAnnotationPipeline` and run

### 5. `agr_vep/src/main/java/org/alliancegenome/vep/config/VepConfig.java`
- Hold all input file paths and options
- MOD name (SGD, FB, WB, ZFIN, HUMAN, RGD, MGI)

### 6. `agr_vep/src/main/java/org/alliancegenome/vep/vcf/VcfAnnotationPipeline.java`
- Open input VCF with `VCFFileReader` (htsjdk)
- Create output with `VariantContextWriterBuilder`
- Copy header, iterate all records, write unchanged

### 7. `agr_vep/src/main/resources/log4j2.xml`
- Copy from `agr_variant_indexer`, remove ES-specific loggers

## Template Files (existing)

- `agr_variant_indexer/pom.xml` — POM template
- `Dockerfile.agr_variant_indexer` — Dockerfile template
- `agr_variant_indexer/src/main/java/org/alliancegenome/indexer/variant/Main.java` — entry point pattern
- `agr_variant_indexer/src/main/java/org/alliancegenome/indexer/variant/es/managers/SourceDocumentCreation.java` — VCF reading pattern with htsjdk

## Acceptance Criteria

- `mvn clean package -pl agr_vep -am` builds successfully
- `java -jar agr_vep.jar --vcf SGD.vcf --output test.vcf` produces a valid VCF identical to input
- Docker image builds via `Dockerfile.agr_vep`
