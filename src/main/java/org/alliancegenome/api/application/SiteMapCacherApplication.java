package org.alliancegenome.api.application;

import static org.elasticsearch.index.query.QueryBuilders.termQuery;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.alliancegenome.api.dao.search.SearchDAO;
import org.alliancegenome.api.model.xml.XMLURL;
import org.alliancegenome.api.model.xml.XMLURLSet;
import org.elasticsearch.search.SearchHit;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SiteMapCacherApplication {

    private final Logger log = Logger.getLogger(getClass());

    @Inject
    private SearchDAO searchDAO;

    private final Integer fileSize = 5000;
    private final HashMap<String, File> files = new HashMap<>();

    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {
        log.info("Caching Sitemap Files: ");
        cacheSiteMap("gene");
        cacheSiteMap("disease");
        log.info("Caching Sitemap Files Finished: ");
    }


    public void destroy(@Observes @Destroyed(ApplicationScoped.class) Object init) {
        for(File f: files.values()) {
            log.debug("Deleting File: " + f.getAbsolutePath());
            f.delete();
        }
    }

    private void cacheSiteMap(String category) {

        List<SearchHit> allIds = searchDAO.getAllIds(termQuery("category", category), fileSize);

        List<XMLURL> urls = new ArrayList<XMLURL>();

        int c = 0;

        for(SearchHit hit: allIds) {
            Date date = null;

            //System.out.println(hit.getFields());

            //if(hit.getSource().get("dateProduced") != null) {
            //  date = new Date((long)hit.getSource().get("dateProduced"));
            //}

            urls.add(new XMLURL(hit.getType() + "/" + hit.getId(), date, "monthly", "0.6"));

            if(urls.size() >= fileSize) {
                saveFile(urls, category, c);
                urls.clear();
                c++;
            }
        }
        if(urls.size() > 0) {
            saveFile(urls, category, c);
        }

    }

    private void saveFile(List<XMLURL> urls, String category, int c) {
        String fileName = category + "-sitemap-" + c;
        String filePath = System.getProperty("jboss.server.temp.dir") + "/" + fileName;
        files.put(fileName, new File(filePath));
        log.debug("Saving File: " + filePath);
        save(urls, files.get(fileName));
    }

    public XMLURLSet getHits(String category, Integer page) {
        String fileName = category + "-sitemap-" + page;
        log.debug("Loading: " + fileName);
        XMLURLSet set = new XMLURLSet();
        if(files.containsKey(fileName)) {
            set.setUrl(load(files.get(fileName)));
        }
        return set;
    }


    private boolean save(List dataObjects, File file) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            GZIPOutputStream gzos = new GZIPOutputStream(fos);
            ObjectOutputStream out = new ObjectOutputStream(gzos);
            out.writeObject(dataObjects);
            out.flush();
            out.close();
            return true;
        }
        catch (IOException e) {
            log.error(e);
            return false;
        }
    }

    private ArrayList load(File file) {
        ArrayList dataObjects = new ArrayList();
        try {
            FileInputStream fis = new FileInputStream(file);
            GZIPInputStream gzis = new GZIPInputStream(fis);
            ObjectInputStream in = new ObjectInputStream(gzis);
            dataObjects = (ArrayList)in.readObject();
            in.close();
            return dataObjects;
        }
        catch (Exception e) {
            log.error(e);
            return dataObjects;
        }
    }


    public Set<String> getFiles() {
        return files.keySet();
    }

}
