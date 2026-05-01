package org.alliancegenome.filegenerator.config;

public enum Format {

	TSV("TSV", "tsv", "tsv"),
	TXT("TXT", "txt", "txt"),
	JSON_RAW("JSON", "json", "json"),
	JSON_MAPPED("JSON", "json", "json"),
	PSI_MI_TAB("TSV", "tsv", "PSI-MI TAB 2.7 Format"),
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
}
