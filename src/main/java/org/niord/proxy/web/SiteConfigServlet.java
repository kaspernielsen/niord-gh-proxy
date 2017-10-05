package org.niord.proxy.web;

import org.apache.commons.lang.StringUtils;
import org.niord.proxy.conf.Settings;
import org.niord.proxy.util.WebUtils;

import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.niord.proxy.web.SiteConfigServlet.SITE_CONFIG_JS;

/**
 * Servlet that generates site configuration
 */
@WebServlet(urlPatterns = SITE_CONFIG_JS, asyncSupported = true)
public class SiteConfigServlet  extends HttpServlet {

    public final static String SITE_CONFIG_JS = "/conf/site-config.js";
    final static String SETTINGS_START  = "/** SETTINGS START **/";
    final static String SETTINGS_END    = "/** SETTINGS END **/";


    @Inject
    Logger log;

    @Inject
    Settings settings;

    private String siteConfigContent;


    /**
     * Initializes the servlet
     * @param config the servlet configuration
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        String basePath = config.getServletContext().getRealPath(File.separator);
        log.info("Initialized with base path " + basePath);


        try {
            Path file = Paths.get(basePath, URLDecoder.decode(SITE_CONFIG_JS, "UTF-8"));

            // Ensure that this is a valid file
            if (!Files.isRegularFile(file)) {
                throw new ServletException("Configuration file " + SITE_CONFIG_JS + " not found");
            }

            siteConfigContent = new String(Files.readAllBytes(file), "UTF-8");

            // Replace the settings
            int startIndex = siteConfigContent.indexOf(SETTINGS_START);
            int endIndex = siteConfigContent.indexOf(SETTINGS_END);
            if (startIndex != -1 && endIndex != -1) {
                endIndex += SETTINGS_END.length();
                siteConfigContent = siteConfigContent.substring(0, startIndex)
                        + getWebSettings()
                        + siteConfigContent.substring(endIndex);
            }

        } catch (IOException e) {
            throw new ServletException("Error reading " + SITE_CONFIG_JS, e);
        }
    }


    /**
     * Main GET method
     *
     * @param request  servlet request
     * @param response servlet response
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        // Write the response
        WebUtils.cache(response, 60 * 60); // Cache 1 hour
        response.setContentType("application/javascript;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(siteConfigContent);

    }


    /**
     * Returns the web settings as a javascript snippet sets the settings as $rootScope variables.
     */
    @SuppressWarnings("all")
    private String getWebSettings() {

        String languages = Arrays.stream(settings.getLanguages())
                .map(l -> String.format("\"%s\"", l))
                .collect(Collectors.joining(", "));

        StringBuilder str = new StringBuilder("\n");

        str.append("    $rootScope.languages = [")
                .append(languages)
                .append("];\n");

        str.append("    $rootScope.timeZone = \"")
                .append(settings.getTimeZone())
                .append("\";\n");

        str.append("    $rootScope.analyticsTrackingId = \"")
                .append(settings.getAnalyticsTrackingId())
                .append("\";\n");

        str.append("    $rootScope.executionMode = \"")
                .append(settings.getExecutionMode().toString())
                .append("\";\n");

        str.append("    $rootScope.wmsLayer = ")
                .append(StringUtils.isNotBlank(settings.getWmsServerUrl()))
                .append(";\n");

        return str.toString();
    }

}
