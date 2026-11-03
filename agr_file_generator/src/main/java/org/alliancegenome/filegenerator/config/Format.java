package org.alliancegenome.filegenerator.config;

public enum Format {

	TSV("TSV", "tsv", "tsv"),
	TXT("TXT", "txt", "txt"),
	JSON_RAW("JSON", "json", "json"),
	JSON_MAPPED("JSON", "json", "json"),
	VCF("VCF", "vcf", "vcf"),
	GFF("GFF", "gff", "gff");

	private final String filetypeToken;
	private final String extension;
	private final String dataFormatLabel;

	Format(String filetypeToken, String extension, String dataFormatLabel) {
		this.filetypeToken = filetypeToken;
		this.extension = extension;
		this.dataFormatLabel = dataFormatLabel;
	}

	public String getFiletypeToken() {
		return filetypeToken;
	}

	public String getExtension() {
		return extension;
	}

	public String getDataFormatLabel() {
		return dataFormatLabel;
	}

	public boolean isJson() {
		return this == JSON_RAW || this == JSON_MAPPED;
	}

	/**
	 * Doc formats write the full hit verbatim (one ES hit = one output entry). Generators that expand a hit into multiple flattened rows via {@link org.alliancegenome.filegenerator.generators.FileGenerator#customizeRows} only fan out for {@link #isRowFormat()} formats — doc formats keep one entry per hit.
	 */
	public boolean isDocFormat() {
		return this == JSON_RAW || this == VCF || this == GFF;
	}

	public boolean isRowFormat() {
		return !isDocFormat();
	}
}
