package org.alliancegenome.neo4j.view;

public class View {

    // Generic Views
    public static class DefaultView { }
    public static class DetailView { }
    public static class SimpleView { }
    
    public static class OrthologyView extends DefaultView { }
    public static class PhenotypeView extends DefaultView { }
    public static class InteractionView extends DefaultView { }
    public static class OrthologyMethodView extends DefaultView { }
    public static class ExpressionView extends DefaultView { }
}
