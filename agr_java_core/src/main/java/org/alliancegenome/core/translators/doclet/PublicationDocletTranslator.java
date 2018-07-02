package org.alliancegenome.core.translators.doclet;

import org.alliancegenome.core.translators.EntityDocletTranslator;
import org.alliancegenome.es.index.site.doclet.PublicationDoclet;
import org.alliancegenome.neo4j.entity.node.Publication;

public class PublicationDocletTranslator extends EntityDocletTranslator<Publication, PublicationDoclet> {

    @Override
    protected PublicationDoclet entityToDocument(Publication publication, int translationDepth) {
        PublicationDoclet pubDoc = new PublicationDoclet();
        pubDoc.setPrimaryKey(publication.getPrimaryKey());
        pubDoc.setPubMedId(publication.getPubMedId());
        pubDoc.setPubMedUrl(publication.getPubMedUrl());
        pubDoc.setPubModId(publication.getPubModId());
        pubDoc.setPubModUrl(publication.getPubModUrl());

        return pubDoc;
    }

    @Override
    protected Publication documentToEntity(PublicationDoclet doument, int translationDepth) {
        throw new RuntimeException("Not yet implemented");
    }

}
