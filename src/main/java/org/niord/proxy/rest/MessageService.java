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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vividsolutions.jts.geom.Geometry;
import org.apache.commons.lang.StringUtils;
import org.niord.model.DataFilter;
import org.niord.model.message.AreaVo;
import org.niord.model.message.MainType;
import org.niord.model.message.MessageVo;
import org.niord.proxy.conf.Settings;
import org.niord.proxy.util.JtsConverter;
import org.niord.proxy.util.WebUtils;

import javax.annotation.PostConstruct;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * The main service for accessing and caching messages from the NW-NM service
 */
@Singleton
@Startup
@Lock(LockType.READ)
@SuppressWarnings("unused")
public class MessageService extends AbstractNiordService {

    /**
     * General messages, i.e. message without an associated area, will be assigned a virtual
     * "General" area for each root area in the active message list, i.e. Denmark -> General, Greenland -> General, etc.
     */
    public static final AreaVo GENERAL_AREA = new AreaVo();

    public static final DataFilter MESSAGE_DETAILS_FILTER =
            DataFilter.get().fields("Message.details", "Message.geometry", "Area.parent", "Category.parent");

    @Inject
    Settings settings;

    @Inject
    Logger log;

    private List<MessageVo> messages = new ArrayList<>();
    private Map<String, List<Geometry>> geometries = new HashMap<>();
    private List<RootArea> areaRoots = new ArrayList<>();


    /** Initialize the service **/
    @PostConstruct
    private void init() {

        // Update the "General" area with a unique ID and localized name
        GENERAL_AREA.setId(-999999);
        Arrays.stream(settings.getLanguages()).forEach(lang -> {
            // Look up the resource bundle
            Locale locale = new Locale(lang);
            ResourceBundle bundle = ResourceBundle.getBundle("MessageDetails", locale);
            GENERAL_AREA.checkCreateDesc(lang).setName(bundle.getString("general_msgs"));
        });


        // Fetch messages from the NW-NM service
        periodicFetchData();
    }


    /** Returns a reference to the messages **/
    public List<MessageVo> getMessages() {
        return messages;
    }


    /** Returns the area roots **/
    public List<RootArea> getAreaRoots() {
        return areaRoots;
    }


    /**
     * Returns a filtered set of messages
     * @param language the language of the descriptive fields to include
     * @param mainTypes the main types to include
     * @param areaIds the area IDs of the messages to include
     * @param wkt the geometric boundary of the messages to include
     * @param active whether or not to only show messages that are currently active
     * @return the filtered set of messages
     */
    public List<MessageVo> getMessages(String language, Set<MainType> mainTypes, Set<Integer> areaIds, String wkt, boolean active) throws Exception {

        language = settings.language(language);
        DataFilter filter = MESSAGE_DETAILS_FILTER.lang(language);

        Geometry geometry = StringUtils.isNotBlank(wkt)
                ? JtsConverter.wktToJts(wkt)
                : null;

        List<MessageVo> result = messages.stream()
                .filter(m -> filterByMainTypes(m, mainTypes))
                .filter(m -> filterByAreaIds(m, areaIds))
                .filter(m -> filterByGeometry(m, geometry))
                .filter(m -> filterByActiveStatus(m, active))
                .map(m -> m.copy(filter))
                .collect(Collectors.toList());

        log.info(String.format("Search for language=%s, mainTypes=%s, areaIds=%s, wkt=%s -> returning %d messages",
                language, mainTypes, areaIds, wkt, result.size()));

        return result;
    }


    /**
     * Returns the message with the given ID
     * @param language the language of the descriptive fields to include
     * @param messageId the ID of the message
     * @return the message with the given ID
     */
    public MessageVo getMessageDetails(String language, String messageId) {

        // First, check if the message is already cached
        MessageVo message = messages.stream()
                .filter(m -> messageId.equals(m.getId()) || messageId.equals(m.getShortId()))
                .findFirst()
                .orElse(null);


        // If not cached here, get it from the NW-NM service
        if (message == null) {

            message = executeNiordJsonRequest(
                    getMessageUrl(messageId),
                    json -> new ObjectMapper().readValue(json, MessageVo.class));

            checkRewriteRepoPath(message);
        }


        if (message == null) {
            return null;
        } else {
            DataFilter filter = MESSAGE_DETAILS_FILTER.lang(language);
            return message.copy(filter);
        }
    }


    /**
     * Rewrite messages fetched from Niord and handle proxying of files.
     * @param message the message to rewrite
     * @return the updated message
     */
    private MessageVo checkRewriteRepoPath(MessageVo message) {

        if (message != null) {
            // Replace absolute links pointing to the Niord server to local links
            message.rewriteRepoPath(
                    settings.getServer() + "/rest/repo/file/",
                    "/rest/repo/file/"
            );
        }

        return message;
    }


    /**
     * Filters messages by their main type
     * @param message the message
     * @param mainTypes the valid main types. If mainTypes is not specified, every message is included
     * @return if the message is included by the filter
     */
    private boolean filterByMainTypes(MessageVo message, Set<MainType> mainTypes) {
        return mainTypes == null || mainTypes.isEmpty() || mainTypes.contains(message.getMainType());
    }


    /**
     * Filters messages by their areas
     * @param message the message
     * @param areaIds the areas which the message must belong to. If areaIds is not specified, every message is included
     * @return if the message is included by the filter
     */
    private boolean filterByAreaIds(MessageVo message, Set<Integer> areaIds) {
        if (areaIds != null && !areaIds.isEmpty()) {
            if (message.getAreas() == null) {
                return false;
            }
            for (AreaVo msgArea : message.getAreas()) {
                // Check the area or any of its parent areas
                do {
                    if (areaIds.contains(msgArea.getId())) {
                        return true;
                    }
                    msgArea = msgArea.getParent();
                } while (msgArea != null);
            }
            return false;
        }
        // If no area IDs is specified, include the message
        return true;
    }


    /**
     * Filters messages by their geometry boundary
     * @param message the message
     * @param geometry the JTS boundary that the message must be within
     * @return if the message is included by the filter
     */
    private boolean filterByGeometry(MessageVo message, Geometry geometry) {
        if (geometry != null) {
            List<Geometry> msgGeometries = geometries.get(message.getId());
            if (msgGeometries != null) {
                return msgGeometries.stream()
                        .anyMatch(geometry::contains);
            }
        }
        return true;
    }


    /**
     * Filters messages on whether they are current active or not
     * @param message the message
     * @param active if set, only include messages that are currently active
     * @return if the message is included by the filter
     */
    private boolean filterByActiveStatus(MessageVo message, boolean active) {
        if (active) {
            Date now = new Date();
            return message.getParts() != null &&
                    message.getParts().stream()
                        .filter(p -> p.getEventDates() != null)
                        .flatMap(p -> p.getEventDates().stream())
                        .anyMatch(di -> di.containsDate(now));
        }
        return true;
    }



    /**
     * Periodically loads the published messages from the Niord server
     */
    @Schedule(second = "12", minute = "*/3", hour = "*")
    public void periodicFetchData() {

        // Load all area roots defined by the settings - once...
        if (this.areaRoots.isEmpty()) {
            List<RootArea> areaRoots = new ArrayList<>();
            for (RootArea rootArea : settings.getRootAreas()) {

                // Fetch the area from the server
                AreaVo area = executeNiordJsonRequest(
                        getAreaUrl(rootArea.getAreaId()),
                        json -> new ObjectMapper().readValue(json, AreaVo.class));

                if (area != null) {
                    areaRoots.add(rootArea.setArea(area));
                }
            }
            this.areaRoots = areaRoots;
            log.info("Loaded area roots: " + areaRoots);
        }


        // Load all active messages
        List<MessageVo> messages = executeNiordJsonRequest(
                getActiveMessagesUrl(),
                json -> new ObjectMapper().readValue(json, new TypeReference<List<MessageVo>>(){})
        );

        if (messages != null) {
            updatePublishedMessages(messages);
        }
    }


    /**
     * Called when a new list of messages has been fetched from the NW-NM service.
     * Updates local message list and computed data such as message geometries
     * @param messages the messages
     */
    private void updatePublishedMessages(List<MessageVo> messages) {

        // First, check if we need to rewrite the repository paths
        messages.forEach(this::checkRewriteRepoPath);

        // Convert the message geometries to JTS geometries
        // This is a fairly expensive operation, so we only want to do it once and cache the result
        Map<String, List<Geometry>> geometries = new HashMap<>();
        messages.forEach(m -> {
            List<Geometry> messageGeometries = new ArrayList<>();
            if (m.getParts() != null) {
                m.getParts().stream()
                        .filter(p -> p.getGeometry() != null && p.getGeometry().getFeatures() != null)
                        .flatMap(p -> Arrays.stream(p.getGeometry().getFeatures()))
                        .filter(f -> f.getGeometry() != null)
                        .forEach(f -> {
                            try {
                                messageGeometries.add(JtsConverter.toJts(f.getGeometry()));
                            } catch (Exception ignored) {
                            }
                        });
            }
            if (!messageGeometries.isEmpty()) {
                geometries.put(m.getId(), messageGeometries);
            }
        });


        // If there are any general messages present (messages without an area), add a virtual "General" area
        checkAddGeneralAreas(messages);

        // Ready to update our local fields
        this.messages = messages;
        this.geometries = geometries;
    }


    /**
     * If there are any general messages present (messages without an area), add a virtual "General"
     * area to each root area: Denmark -> General, Greenland -> General, etc.
     *
     * @param messages the messages to check
     */
    private void checkAddGeneralAreas(List<MessageVo> messages) {
        if (messages != null) {
            List<AreaVo> generalAreas = areaRoots.stream()
                    .map(a -> {
                        AreaVo generalArea = GENERAL_AREA.copy(DataFilter.get());
                        generalArea.setParent(a);
                        return generalArea;
                    })
                    .collect(Collectors.toList());

            messages.stream()
                    .filter(m -> m.getAreas() == null || m.getAreas().isEmpty())
                    .forEach(m -> m.setAreas(generalAreas));
        }
    }


    /**
     * Returns the area with the given ID, if the area is one of the cached area groups
     * @param areaId the ID of the area
     * @return the area with the given ID
     */
    public AreaVo getArea(Integer areaId) {
        return messages.stream()
                .filter(m -> m.getAreas() != null && !m.getAreas().isEmpty())
                .flatMap(m -> m.getAreas().stream())
                .map(a -> {
                    AreaVo area = a;
                    // Check the area or any of its parent areas
                    do {
                        if (Objects.equals(area.getId(), areaId)) {
                            return area;
                        }
                        area = area.getParent();
                    } while (area != null);
                    return null;
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }


    /**
     * Returns the url for fetching the list of active messages sorted by area
     * @return the list of active messages sorted by area
     */
    private String getActiveMessagesUrl() {
        return settings.getServer() + "/rest/public/v1/messages";
    }


    /**
     * Returns the url for fetching the public messages with the given ID
     * @return the public message with the given ID
     */
    private String getMessageUrl(String messageId) {
        return settings.getServer()
                + "/rest/public/v1/message/" + WebUtils.encodeURIComponent(messageId);
    }

    /**
     * Returns the url for fetching the area with the given ID
     * @return the area with the given ID
     */
    private String getAreaUrl(String areaId) {
        return settings.getServer()
                + "/rest/public/v1/area/" + WebUtils.encodeURIComponent(areaId);
    }

}
