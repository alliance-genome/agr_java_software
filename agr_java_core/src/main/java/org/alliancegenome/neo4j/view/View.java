package org.alliancegenome.neo4j.view;

public class View {

    

    // Generic Views
    public static class Default { }
    public static class DetailView { }
    public static class SimpleView { }
    
    public static class API extends Default { }
    
    public static class Orthology extends Default { }
    public static class Phenotype extends Default { }
    public static class OrthologyMethod extends Default { }
    public static class Expression extends Default { }
    
    public static class Interaction extends API { }
    public static class GeneAPI extends API { }
    public static class GeneAllelesAPI extends API { }
    public static class AlleleAPI extends API { }
    
}
