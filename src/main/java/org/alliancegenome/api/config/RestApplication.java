/*
 * Copyright (C) 2016, 2017 Antonio Goncalves and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.alliancegenome.api.config;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import io.swagger.jaxrs.config.BeanConfig;

@ApplicationPath("/")
@ApplicationScoped
public class RestApplication extends Application {

    public RestApplication() {
        final BeanConfig beanConfig = new BeanConfig();
        beanConfig.setTitle("AGR API");
        beanConfig.setDescription("This API is for retrieving data from AGR");
        beanConfig.setVersion("1.0.0");
        beanConfig.setSchemes(new String[] { "http" });
        beanConfig.setHost("localhost:8080/api");
        beanConfig.setBasePath("/");
        beanConfig.setResourcePackage("org.alliancegenome.api");
        beanConfig.setPrettyPrint(true);
        beanConfig.setScan(true);
    }

}
