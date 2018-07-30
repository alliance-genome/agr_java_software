package org.alliancegenome.core.translators.doclet;

import org.alliancegenome.es.index.site.doclet.PublicationDoclet;
import org.alliancegenome.neo4j.entity.node.EntityJoin;
import org.alliancegenome.neo4j.entity.node.EvidenceCode;
import org.alliancegenome.neo4j.entity.node.Publication;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class PublicationDocletListTranslator {

    private static PublicationDocletTranslator publicationDocletTranslator = new PublicationDocletTranslator();
    private final Logger log = LogManager.getLogger(getClass());

    public List<PublicationDoclet> getPublicationDoclets(List<? extends EntityJoin> entityJoinList) {
        List<PublicationDoclet> pubDocletListRaw = entityJoinList.stream()
                // filter out records that do not have valid pub / evidence code entries
                .filter(entityJoin ->
                        publicationDocletTranslator.translate(entityJoin.getPublication()) != null
                )
                .map(entityJoin -> {
                    Publication publication = entityJoin.getPublication();
                    PublicationDoclet publicationDoclet = publicationDocletTranslator.translate(publication);
                    List<EvidenceCode> evidenceCodes = entityJoin.getEvidenceCodes();
                    if (evidenceCodes == null) {
                        return publicationDoclet;
                    }

                    Set<String> evidencesDocument = evidenceCodes.stream()
                            .map(EvidenceCode::getPrimaryKey)
                            .collect(Collectors.toSet());
                    publicationDoclet.setEvidenceCodes(evidencesDocument);
                    return publicationDoclet;
                }).distinct().filter(Objects::nonNull).sorted(PublicationDoclet::compareTo).collect(Collectors.toList());

        // get evidence codes for same pub onto s
        List<PublicationDoclet> pubDocletList = new ArrayList<>();
        for (PublicationDoclet doclet : pubDocletListRaw) {
            PublicationDoclet existingDoclet = null;
            for (PublicationDoclet finalDoclet : pubDocletList) {
                if (doclet.compareTo(finalDoclet) == 0) {
                    existingDoclet = finalDoclet;
                }
            }
            if (existingDoclet == null) {
                pubDocletList.add(doclet);
            } else {
                if (doclet.getEvidenceCodes() != null)
                    existingDoclet.getEvidenceCodes().addAll(doclet.getEvidenceCodes());
            }
        }
        return pubDocletList;
    }

}
