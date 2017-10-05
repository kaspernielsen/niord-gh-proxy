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

import org.apache.commons.lang.StringUtils;
import org.niord.proxy.conf.Settings;
import org.niord.proxy.util.WebUtils;

import javax.annotation.PostConstruct;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * If the Niord Proxy has been initialized with a valid path to the Niord message repository,
 * then the proxy will rewrite messages fetched from Niord and handle streaming of files.
 */
@javax.ws.rs.Path("/repo")
@Singleton
@Startup
@Lock(LockType.READ)
@SuppressWarnings("unused")
public class RepositoryRestService extends AbstractNiordService {

    @Context
    ServletContext servletContext;

    @Inject
    Settings settings;

    @Inject
    FileTypes fileTypes;

    @Inject
    Logger log;

    Path repoRoot = null;

    int cacheTimeout = 5;


    /**
     * Initializes the repository
     */
    @PostConstruct
    public void init() {

        repoRoot = Paths.get(settings.getRepoRoot());

        // Create the repo root directory
        if (!Files.exists(repoRoot)) {
            try {
                Files.createDirectories(getRepoRoot());
            } catch (IOException e) {
                log.log(Level.SEVERE, "Error creating repository dir: " + repoRoot, e);
            }
        }
    }


    /**
     * Returns the repository root
     * @return the repository root
     */
    public Path getRepoRoot() {
        return repoRoot;
    }


    /**
     * Returns the repository file with the given path
     * @param path the path
     * @return the repository file with the given path
     */
    public Path getRepoFile(String path) {
        if (StringUtils.isBlank(path)) {
            return null;
        }

        // Check that the specified file is indeed under the repository root
        Path file = repoRoot.resolve(path);
        if (!file.toAbsolutePath().startsWith(repoRoot.toAbsolutePath())) {
            log.log(Level.WARNING, "Illegal file path not in repo root: " + file);
            return null;
        }
        return file;
    }


    /**
     * Streams the file specified by the path
     * @param path the path
     * @param request the servlet request
     * @return the response
     */
    @GET
    @javax.ws.rs.Path("/file/{file:.+}")
    public Response streamFile(@PathParam("file") String path,
                               @Context Request request) throws IOException {

        Path f = getRepoFile(path);

        if (f == null || Files.isDirectory(f)) {
            log.log(Level.WARNING, "Failed streaming file: " + f);
            return Response
                    .status(HttpServletResponse.SC_NOT_FOUND)
                    .entity("File not found: " + path)
                    .build();
        }

        // When a locally maintained repository is used, fetch the file from Niord
        if (settings.getRepoType() == Settings.RepoType.LOCAL && Files.notExists(f)) {
            f = fetchNiordFile(settings.getServer() + "/rest/repo/file/" + WebUtils.encodeURIComponent(path), f);
        }


        // Check if the file exits
        if (f == null || Files.notExists(f) ) {
            log.log(Level.WARNING, "Failed streaming file: " + f);
            return Response
                    .status(HttpServletResponse.SC_NOT_FOUND)
                    .entity("File not found: " + path)
                    .build();
        }

        // Set expiry to cacheTimeout minutes
        Date expirationDate = new Date(System.currentTimeMillis() + 1000L * 60L * cacheTimeout);

        String mt = fileTypes.getContentType(f);

        // Check for an ETag match
        EntityTag etag = new EntityTag("" + Files.getLastModifiedTime(f).toMillis() + "_" + Files.size(f), true);
        Response.ResponseBuilder responseBuilder = request.evaluatePreconditions(etag);
        if (responseBuilder != null) {
            // Etag match
            log.log(Level.FINE, "File unchanged. Return code 304");
            return responseBuilder
                    .expires(expirationDate)
                    .build();
        }

        log.log(Level.FINE, "Streaming file: " + f);
        return Response
                .ok(f.toFile(), mt)
                .expires(expirationDate)
                .tag(etag)
                .build();
    }

}
