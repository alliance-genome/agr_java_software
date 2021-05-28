package org.alliancegenome.neo4j.view;

public class View {

    // Generic Views
    public static class Default { }
    public static class DetailView { }
    public static class SimpleView { }

    public static class API extends Default { }

    public static class Orthology extends Default { }
    public static class OrthologyCacher extends Orthology { }
    public static class Cacher extends Default { }
    public static class CacherDetail extends Cacher { }
    public static class PhenotypeAPI extends API { }
    public static class OrthologyMethod extends Default { }
    public static class Expression extends API { }
    public static class DiseaseAnnotation extends API { }
    public static class DiseaseAnnotationSummary extends DiseaseAnnotation { }
    public static class PrimaryAnnotation extends API { }
    public static class DiseaseCacher extends DiseaseAnnotationSummary { }
    public static class DiseaseAnnotationAll extends DiseaseAnnotation { }

    public static class Interaction extends API { }
    public static class GeneAPI extends API { }
    public static class DiseaseAPI extends API { }
    public static class GeneAllelesAPI extends API { }
    public static class GeneAlleleVariantSequenceAPI extends Default { }
    public static class AlleleAPI extends API { }
    public static class TransgenicAlleleAPI extends API { }
    public static class VariantAPI extends API { }
    public static class ReleaseInfo extends API {}
    
    
    public static class AlleleVariantSequenceConverterForES { } // This needs to NOT extend Default as this controls specifically what gets serialized and what doesn't.
}
