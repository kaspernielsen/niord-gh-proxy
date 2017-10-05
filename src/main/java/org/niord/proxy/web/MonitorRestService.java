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

package org.niord.proxy.web;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * An extremely simple REST endpoint that can be used to monitor the basic dead-or-alive state of the Niord-Proxy service
 */
@Path("/monitor")
public class MonitorRestService {

    /** Can be used to see if Niord is running at all **/
    @GET
    @Path("/ping")
    @Produces("text/plain")
    public String ping() {
        return "pong";
    }

}
