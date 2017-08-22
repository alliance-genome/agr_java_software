package org.alliancegenome.indexer.service;

import org.alliancegenome.indexer.config.ConfigHelper;
import org.alliancegenome.indexer.entity.DOTerm;
import org.alliancegenome.indexer.util.Neo4jSessionFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.cypher.query.Pagination;
import org.neo4j.ogm.session.Session;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Neo4jESService<E> {

    protected Class<E> entityTypeClazz;
    protected Session neo4jSession;
    protected PreBuiltTransportClient esSearchClient = new PreBuiltTransportClient(Settings.EMPTY);

    public Neo4jESService(Class<E> entityTypeClazz) {
        try {
            //entityType = entityType.
            this.entityTypeClazz = entityTypeClazz;
            neo4jSession = Neo4jSessionFactory.getInstance().getNeo4jSession();
            esSearchClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(ConfigHelper.getEsHost()), ConfigHelper.getEsPort()));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public Iterable<E> getPage(int pageNumber, int pageSize, int depth) {
        Pagination p = new Pagination(pageNumber, pageSize);
        return neo4jSession.loadAll(entityTypeClazz, p, depth);
    }

    public Iterable<E> getPage(int pageNumber, int pageSize) {
        return getPage(pageNumber, pageSize, 1);
    }

    public Collection<E> getEntity(String key, String value) {
//        E h = neo4jSession.load(entityTypeClazz, 2381);
        return neo4jSession.loadAll(entityTypeClazz, new Filter(key, value));
    }

    public int getCount() {
        return (int) neo4jSession.countEntitiesOfType(entityTypeClazz);
    }

    public List<DOTerm> getDiseasesWithGenes() {
        String cypher = "match (n:DOTerm), " +
                "(a:Annotation)-[q:ASSOCIATION]->(n), " +
                "(m:Gene)-[qq:ASSOCIATION]->(a), " +
                "(p:Publication)<-[qqq*]-(a), " +
                "(e:EvidenceCode)<-[ee:ANNOTATED_TO]-(p)" +
                "return n, q,a,qq,m,qqq,p, ee, e";
        Iterable<DOTerm> list = neo4jSession.query(entityTypeClazz, cypher, Collections.EMPTY_MAP);
        return (List<DOTerm>) list;
    }

    public List<DOTerm> getDiseaseInfo() {
        String cypher = "match (n:DOTerm)<-[q:IS_A]-(m:DOTerm)<-[r:IS_IMPLICATED_IN]-(g:Gene) return n,q, m";
        Iterable<DOTerm> list = neo4jSession.query(entityTypeClazz, cypher, Collections.EMPTY_MAP);
        return (List<DOTerm>) list;
    }

}