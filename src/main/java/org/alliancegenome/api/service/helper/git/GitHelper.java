package org.alliancegenome.api.service.helper.git;

import java.io.File;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.alliancegenome.api.config.ConfigHelper;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jboss.logging.Logger;

@ApplicationScoped
public class GitHelper {

    @Inject
    protected ConfigHelper config;

    private Logger log = Logger.getLogger(getClass());
    private String prefix = "/git";

    public void setupRelease(String release) {
        String localPath = config.getValidationSoftwarePath() + prefix + "/" + release;
        log.debug(config);
        File f = new File(localPath);

        if(f.exists() && f.isDirectory()) {
            log.debug("Directory already exists: " + localPath);
        } else {
            try {
                Git.cloneRepository()
                        .setURI( "https://github.com/alliance-genome/agr_schemas.git" )
                        .setDirectory(new File(localPath))
                        .setBranch(release)
                        .call();
            } catch (GitAPIException e) {
                e.printStackTrace();
            }
        }
    }

    public File getFile(String release, String filePath) {
        setupRelease(release);
        String path = config.getValidationSoftwarePath() + prefix + "/" + release;
        log.debug("Validation File: " + path + filePath);
        return new File(path + filePath);
    }

}
