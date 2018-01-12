package org.alliancegenome.api.service.helper.github;

import java.util.List;

import org.jboss.logging.Logger;

import si.mazi.rescu.HttpStatusIOException;
import si.mazi.rescu.RestProxyFactory;

public class GithubRESTAPI implements GithubRESTAPIInterface {

    private GithubRESTAPIInterface api = null;
    private Logger log = Logger.getLogger(getClass());

    public GithubRESTAPI() {
        api = RestProxyFactory.createProxy(GithubRESTAPIInterface.class, "https://api.github.com");
    }

    @Override
    public GithubRelease getLatestRelease(String repo) {
        try {
            return api.getLatestRelease(repo);
        } catch (HttpStatusIOException e) {
            log.warn("HTTP error code returned from server: " + e.getHttpStatusCode());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public GithubRelease getRelease(String repo, String release) {
        try {
            return api.getRelease(repo, release);
        } catch (HttpStatusIOException e) {
            log.warn("HTTP error code returned from server: " + e.getHttpStatusCode());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<GithubRelease> getReleases(String repo) {
        try {
            return api.getReleases(repo);
        } catch (HttpStatusIOException e) {
            log.warn("HTTP error code returned from server: " + e.getHttpStatusCode());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
