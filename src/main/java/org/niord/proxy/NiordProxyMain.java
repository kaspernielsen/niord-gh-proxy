/*
 * Copyright 2016 Danish Maritime Authority.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.niord.proxy;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.niord.proxy.conf.LogConfiguration;
import org.niord.proxy.rest.RestApplication;
import org.niord.proxy.util.JtsConverter;
import org.niord.proxy.web.TldFunctions;
import org.wildfly.swarm.Swarm;
import org.wildfly.swarm.jaxrs.JAXRSArchive;

/**
 * Bootstrapping the Niord Proxy
 */
public class NiordProxyMain {

    public static void main(String[] args) throws Exception {

        // Instantiate the container
        Swarm swarm = new Swarm();

        // Create one or more deployments
        JAXRSArchive deployment = ShrinkWrap.create(JAXRSArchive.class)
            .setContextRoot( "/" )
            .addPackage( NiordProxyMain.class.getPackage() )
            .addPackage( LogConfiguration.class.getPackage() )
            .addPackage( RestApplication.class.getPackage() )
            .addPackage( JtsConverter.class.getPackage() )
            .addPackage( TldFunctions.class.getPackage() )
            .addAllDependencies()
            .staticContent();

        swarm.start()
            .deploy(deployment);
    }

}
