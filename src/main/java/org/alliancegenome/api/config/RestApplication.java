package org.alliancegenome.api.config;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import io.swagger.jaxrs.config.BeanConfig;

@ApplicationPath("/v1")
@ApplicationScoped
public class RestApplication extends Application {

    public RestApplication() {
        final BeanConfig beanConfig = new BeanConfig();
        beanConfig.setTitle("AGR API");
        beanConfig.setDescription("This API is for retrieving data from AGR");
        beanConfig.setVersion("1.0.0");
        beanConfig.setSchemes(new String[] { "http" });
        //beanConfig.setHost("localhost:8080/api");
        beanConfig.setBasePath("/api/v1");
        beanConfig.setResourcePackage("org.alliancegenome.api");
        beanConfig.setPrettyPrint(true);
        beanConfig.setScan(true);
    }

}
