package org.alliancegenome.es.index.data.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.alliancegenome.es.index.dao.ESDocumentDAO;
import org.alliancegenome.es.index.data.document.DataFileDocument;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

public class DataFileDAO extends ESDocumentDAO<DataFileDocument> {

    public List<DataFileDocument> search(String schemaVersion, Date snapShotDate) {
        MatchQueryBuilder mqb = QueryBuilders.matchQuery("schemaVersion", schemaVersion);
        List<DataFileDocument> docs = search(mqb);
        HashMap<MultiKey<String>, DataFileDocument> map = new HashMap<MultiKey<String>, DataFileDocument>();
        
        for(DataFileDocument doc: docs) {
            MultiKey<String> key = new MultiKey<>(schemaVersion, doc.getDataType(), doc.getTaxonIDPart());
            
            if(map.containsKey(key)) {
                DataFileDocument current = map.get(key);
                if(doc.getUploadDate().after(current.getUploadDate()) && doc.getUploadDate().before(snapShotDate)) {
                    map.put(key, doc);
                }
            } else {
                if(doc.getUploadDate().before(snapShotDate)) {
                    map.put(key, doc);
                }
            }
        }
        
        return new ArrayList<DataFileDocument>(map.values());
    }

}
