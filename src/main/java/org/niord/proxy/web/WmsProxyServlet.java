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

import org.apache.commons.lang.StringUtils;
import org.niord.proxy.conf.Settings;
import org.niord.proxy.util.WebUtils;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Proxy WMS data
 *
 * This way, end users will not be able to see the wms user, login, password, etc.
 */
@WebServlet(value = "/wms/*")
public class WmsProxyServlet extends HttpServlet {

    // The color we want transparent
    final static int        CACHE_TIMEOUT   =  24 * 60 * 60; // 24 hours
    static final String     BLANK_IMAGE     = "/img/blank.png";

    @Inject
    Logger log;

    @Inject
    Settings settings;

    private String wmsServerUrl;

    /** {@inheritDoc} */
    @Override
    public void init() throws ServletException {
        super.init();

        wmsServerUrl = settings.getWmsServerUrl();
    }


    /**
     * Main GET method
     * @param request servlet request
     * @param response servlet response
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        // Cache for a day
        WebUtils.cache(response, CACHE_TIMEOUT);


        // Check that the WMS parameters have been specified
        if (StringUtils.isBlank(wmsServerUrl)) {
            response.sendRedirect(BLANK_IMAGE);
            return;
        }

        boolean layerParamDefined = wmsServerUrl.toLowerCase().contains("layers=");

        @SuppressWarnings("unchecked")
        Map<String, String[]> paramMap = request.getParameterMap();
        String params = paramMap
                .entrySet()
                .stream()
                .filter(p -> !layerParamDefined || !"layers".equalsIgnoreCase(p.getKey()))
                .map(p -> String.format("%s=%s", p.getKey(), p.getValue()[0]))
                .collect(Collectors.joining("&"));

        String url = wmsServerUrl + "&" + params;
        log.log(Level.FINEST, "Loading image " + url);

        try {
            BufferedImage image = ImageIO.read(new URL(url));
            if (image != null) {
                OutputStream out = response.getOutputStream();
                ImageIO.write(image, "png", out);
                image.flush();
                out.close();
                return;
            }
        } catch (Exception e) {
            log.log(Level.FINEST, "Failed loading WMS image for URL " + url + ": " + e);
        }

        // Fall back to return a blank image
        try {
            response.sendRedirect(BLANK_IMAGE);
        } catch (IOException e) {
            log.log(Level.FINEST, "Failed returning blank image for URL " + url + ": " + e);
        }
    }
}
