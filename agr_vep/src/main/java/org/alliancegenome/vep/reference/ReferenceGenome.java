package org.alliancegenome.vep.reference;

import java.io.File;
import java.io.IOException;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ReferenceGenome implements AutoCloseable {

	private final IndexedFastaSequenceFile fasta;

	public ReferenceGenome(String fastaPath) throws IOException {
		File fastaFile = new File(fastaPath);
		File faiFile = new File(fastaPath + ".fai");
		if (!faiFile.exists()) {
			throw new IOException("FASTA index not found: " + faiFile
				+ ". Run 'samtools faidx " + fastaPath + "' to create it.");
		}
		this.fasta = new IndexedFastaSequenceFile(fastaFile);
		log.info("Reference genome loaded: {}", fastaPath);
	}

	public String getSequence(String chr, int start, int end) {
		ReferenceSequence seq = fasta.getSubsequenceAt(chr, start, end);
		return new String(seq.getBases());
	}

	public boolean hasContig(String chr) {
		return fasta.getSequenceDictionary().getSequence(chr) != null;
	}

	@Override
	public void close() throws IOException {
		fasta.close();
	}
}
