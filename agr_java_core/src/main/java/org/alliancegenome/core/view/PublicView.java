package org.alliancegenome.core.view;

import org.alliancegenome.curation_api.view.CurationView.ForPublic;

public class PublicView {

	// Generic Views
	public static class Default extends ForPublic {
	}

	public static class PhenotypeAnnotation extends API {
	}

	public static class PhenotypeAnnotationAll extends PhenotypeAnnotation {
	}

	public static class API extends Default {
	}

	public static class PhenotypeAPI extends API {
	}

	public static class Expression extends API {
	}

	public static class DiseaseAnnotation extends API {
	}

	public static class DiseaseAnnotationAll extends DiseaseAnnotation {
	}

	public static class DiseaseAnnotationSummary extends DiseaseAnnotation {
	}

	public static class Interaction extends API {
	}

	public static class GeneticInteraction extends Interaction {
	}

	public static class MolecularInteraction extends Interaction {
	}

	public static class VariantAPI extends API {
	}
	public static class ReleaseInfo extends API {
	}

}
