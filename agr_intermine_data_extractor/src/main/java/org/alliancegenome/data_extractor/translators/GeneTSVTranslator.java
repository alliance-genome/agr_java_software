package org.alliancegenome.data_extractor.translators;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.alliancegenome.core.translators.ResultTSVTranslator;

import lombok.Getter;

@Getter
public class GeneTSVTranslator extends ResultTSVTranslator {

	public GeneTSVTranslator(PrintWriter writer) {
		super(writer);
	}

	@Override
	protected List<String> getHeaders() {
		return Arrays.asList(
				"Id",
				"SecondaryId",
				"Synonyms",
				"CrossRefs",
				"Name",
				"Symbol",
				"MOD Description",
				"Auto Description",
				"Species",
				"Chromosome",
				"Start",
				"End",
				"Strand",
				"SoTerm"
			);
	}

	@Override
	protected List<String> mapToRow(Map<String, Object> map) {
		
		String strippedSynopsis = String.valueOf(map.get("g.geneSynopsis")).replaceAll("[\\r\\n]", "").strip();
		String strippedGeneSynopsis = String.valueOf(map.get("g.automatedGeneSynopsis")).replaceAll("[\\r\\n]", "").strip();

		return Arrays.asList(
			String.valueOf(map.get("g.primaryKey")).strip(),
			String.valueOf(map.get("g.modLocalId")).strip(),
			String.valueOf(Arrays.deepToString((Object[]) map.get("synonyms")).strip()),   
			String.valueOf(Arrays.deepToString((Object[]) map.get("crossrefs")).strip()),
			String.valueOf(map.get("g.name")).strip(),
			String.valueOf(map.get("g.symbol")).strip(),
			strippedSynopsis,
			strippedGeneSynopsis,
			String.valueOf(map.get("s.primaryKey")).strip(),
			String.valueOf(map.get("gl.chromosome")).strip(),
			String.valueOf(map.get("gl.start")).strip(),
			String.valueOf(map.get("gl.end")).strip(),
			String.valueOf(map.get("gl.strand")).strip(),
			String.valueOf(map.get("so.name")).strip()

		);
	}

}
