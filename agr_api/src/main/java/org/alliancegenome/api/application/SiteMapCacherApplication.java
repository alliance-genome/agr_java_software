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

import org.alliancegenome.api.model.xml.XMLURL;
import org.alliancegenome.api.model.xml.XMLURLSet;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.index.site.dao.SearchDAO;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SiteMapCacherApplication {

    private final Logger log = Logger.getLogger(getClass());

    private SearchDAO searchDAO = new SearchDAO();

    private final Integer fileSize = 15000; // 15000 is the max that the index is configured with see site_index settings file
    private final HashMap<String, File> files = new HashMap<>();

    public SiteMapCacherApplication() {
        File dir = new File(System.getProperty("java.io.tmpdir") + "/sitemap/");
        if(!dir.exists()) dir.mkdir();
    }
    
    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {
        if(ConfigHelper.getGenerateSitemap()) {
            log.info("Caching Sitemap Files: ");
            cacheSiteMap("gene");
            // We don't have allele page yet.
            //cacheSiteMap("allele");
            cacheSiteMap("disease");
            log.info("Caching Sitemap Files Finished: ");
        }
    }

    public void destroy(@Observes @Destroyed(ApplicationScoped.class) Object init) {
        for(File f: files.values()) {
            log.debug("Deleting File: " + f.getAbsolutePath());
            f.delete();
        }
    }

    public void cacheSiteMap(String category) {
        log.debug("Getting all ids for: " + category);
        
        List<String> allIds = searchDAO.getAllIds(termQuery("category", category), fileSize);

        log.debug("Finished Loading all ids: " + allIds.size() + " for " + category);
        
        List<XMLURL> urls = new ArrayList<XMLURL>();

        int c = 0;

        for(String id: allIds) {
            Date date = null;

            //System.out.println(hit.getFields());

            //if(hit.getSource().get("dateProduced") != null) {
            //  date = new Date((long)hit.getSource().get("dateProduced"));
            //}

            urls.add(new XMLURL(category + "/" + id, date, "monthly", "0.6"));

            if(urls.size() >= fileSize) {
                saveFile(urls, category, c);
                urls.clear();
                c++;
            }
        }
        if(urls.size() > 0) {
            saveFile(urls, category, c);
        }
        urls.clear();
        allIds.clear();
    }

    private void saveFile(List<XMLURL> urls, String category, int c) {
        String fileName = category + "-sitemap-" + c;
        String filePath = System.getProperty("java.io.tmpdir") + "/sitemap/" + fileName;
        files.put(fileName, new File(filePath));
        log.trace("Saving File: " + filePath);
        save(urls, files.get(fileName));
    }


    public XMLURLSet getHits(String category, Integer page) {
        String fileName = category + "-sitemap-" + page;
        log.info("Loading: " + fileName);
        XMLURLSet set = new XMLURLSet();
        if(files.containsKey(fileName)) {
            set.setUrl(load(files.get(fileName)));
        }
        return set;
    }


    private boolean save(List<XMLURL> dataObjects, File file) {
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

    @SuppressWarnings("unchecked")
    private ArrayList<XMLURL> load(File file) {
        ArrayList<XMLURL> dataObjects = new ArrayList<XMLURL>();
        try {
            FileInputStream fis = new FileInputStream(file);
            GZIPInputStream gzis = new GZIPInputStream(fis);
            ObjectInputStream in = new ObjectInputStream(gzis);
            dataObjects = (ArrayList<XMLURL>)in.readObject();
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
