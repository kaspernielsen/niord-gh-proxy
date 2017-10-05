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
package org.niord.proxy.rest;

import org.niord.model.message.MainType;
import org.niord.model.message.MessageVo;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.util.List;
import java.util.Set;


/**
 * Main REST endpoint for fetching messages from the NW-NM backend
 */
@Path("/messages")
public class MessagesRestService {

    @Inject
    MessageService messageService;


    /**
     * Returns a filtered set of messages
     *
     * @param language the language of the descriptive fields to include
     * @param mainTypes the main types to include
     * @param areaIds the area IDs of the messages to include
     * @param wkt the geometric boundary of the messages to include
     * @param active whether or not to only show messages that are currently active
     * @return the filtered set of messages
     */
	@GET
    @Path("/search")
	@Produces("application/json;charset=UTF-8")
	public List<MessageVo> search(
	        @QueryParam("language") @DefaultValue("en") String language,
            @QueryParam("mainType") Set<MainType> mainTypes,
            @QueryParam("areaId") Set<Integer> areaIds,
            @QueryParam("wkt") String wkt,
            @QueryParam("active") boolean active
            ) throws Exception {

        return messageService.getMessages(language, mainTypes, areaIds, wkt, active);
    }


    /**
     * Returns the message with the given ID
     *
     * @param language the language of the descriptive fields to include
     * @param messageId the message ID
     * @return the message with the given ID
     */
    @GET
    @Path("/message/{messageId}")
    @Produces("application/json;charset=UTF-8")
    public MessageVo details(
            @QueryParam("language") @DefaultValue("en") String language,
            @PathParam("messageId") String messageId
    ) throws Exception {

        return messageService.getMessageDetails(language, messageId);
    }


    /**
     * Fetches the area roots - which may be used for message filtering
     *
     * @return the area roots
     */
    @GET
    @Path("/area-roots")
    @Produces("application/json;charset=UTF-8")
    public List<RootArea>  areaRoots() {

        return messageService.getAreaRoots();
    }

}