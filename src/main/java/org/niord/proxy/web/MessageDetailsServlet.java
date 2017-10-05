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

import com.itextpdf.text.DocumentException;
import org.apache.commons.lang.StringUtils;
import org.niord.model.message.AreaDescVo;
import org.niord.model.message.MainType;
import org.niord.model.message.MessageVo;
import org.niord.proxy.conf.Settings;
import org.niord.proxy.rest.MessageService;
import org.niord.proxy.util.WebUtils;
import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;
import org.xhtmlrenderer.pdf.ITextRenderer;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.niord.proxy.rest.MessageService.GENERAL_AREA;

/**
 * Servlet used for generating either a HTML details page
 * or a PDF for the MSI details defined by the provider and language
 * specified using request parameters.
 */
@WebServlet(urlPatterns = {"/details.pdf", "/details.html"}, asyncSupported = true)
public class MessageDetailsServlet extends HttpServlet {

    private static final String DETAILS_JSP_FILE = "/WEB-INF/jsp/details.jsp";

    @Inject
    Logger log;

    @Inject
    MessageService messageService;


    @Inject
    Settings settings;

    /**
     * Main GET method
     * @param request servlet request
     * @param response servlet response
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        // Determine whether to return HTML or PDF
        boolean pdf = request.getServletPath().endsWith("pdf");

        // Never cache the response
        response = WebUtils.nocache(response);

        // Read the request parameters
        String language = settings.language(request.getParameter("language"));


        // Force the encoding and the locale based on the lang parameter
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        final Locale locale = new Locale(language);
        request = new HttpServletRequestWrapper(request) {
            @Override public Locale getLocale() { return locale; }
        };

        try {
            // Get the messages in the given language for the requested provider
            List<MessageVo> messages = getMessages(request, language);
            String searchText = getSearchText(request, language, messages);

            // Register the attributes to be used on the JSP page
            request.setAttribute("messages", messages);
            request.setAttribute("searchText", searchText);
            request.setAttribute("lang", language);
            request.setAttribute("languages", Arrays.asList(settings.getLanguages()));
            request.setAttribute("language", language);
            request.setAttribute("locale", locale);
            request.setAttribute("timeZone", settings.getTimeZone());
            request.setAttribute("now", new Date());
            request.setAttribute("pdf", pdf);

            if (pdf) {
                generatePdfFile(request, response);
            } else {
                generateHtmlPage(request, response);
            }

        } catch (Exception e) {
            log.log(Level.SEVERE, "Error generating file " + request.getServletPath(), e);
            throw new ServletException("Error generating file " + request.getServletPath(), e);
        }
    }

    /** Gets the messages for the given search criteria **/
    List<MessageVo> getMessages(HttpServletRequest request, String language) throws Exception {

        // A specific message was requested
        if (StringUtils.isNotBlank(request.getParameter("messageId"))) {
            MessageVo message = messageService.getMessageDetails(language, request.getParameter("messageId"));
            return message == null
                    ? Collections.emptyList()
                    : Collections.singletonList(message);
        }

        // Search message based on the request parameters

        Set<MainType> mainTypes = null;
        if (StringUtils.isNotBlank(request.getParameter("mainType"))) {
            mainTypes = Arrays.stream(request.getParameterValues("mainType"))
                    .map(MainType::valueOf)
                    .collect(Collectors.toSet());
        }

        Set<Integer> areaIds = null;
        if (StringUtils.isNotBlank(request.getParameter("areaId"))) {
            areaIds = Arrays.stream(request.getParameterValues("areaId"))
                    .map(Integer::valueOf)
                    .collect(Collectors.toSet());
        }

        String wkt = request.getParameter("wkt");
        boolean active = false;
        if (StringUtils.isNotBlank(request.getParameter("active"))) {
            active = Boolean.valueOf(request.getParameter("active"));
        }

        return messageService.getMessages(language, mainTypes, areaIds, wkt, active);
    }


    /** Formats the search criteria textually */
    private String getSearchText(HttpServletRequest request, String language, List<MessageVo> messages) {

        // Look up the resource bundle
        Locale locale = new Locale(language);
        TimeZone timeZone = TimeZone.getTimeZone(settings.getTimeZone());
        ResourceBundle bundle = ResourceBundle.getBundle("MessageDetails", locale);
        StringBuilder txt = new StringBuilder();

        // A specific message was requested
        if (StringUtils.isNotBlank(request.getParameter("messageId"))) {
            MessageVo msg = messages.isEmpty() ? null : messages.get(0);
            if (msg != null) {
                txt.append(bundle.getString("filter_type_" + msg.getMainType().toString().toLowerCase()))
                    .append(" ");
            }
            SimpleDateFormat format = new SimpleDateFormat(bundle.getString("filter_date_format"), locale);
            format.setTimeZone(timeZone);
            space(txt).append("<span style='float: right'>")
                    .append(format.format(new Date()))
                    .append("</span>");
            return txt.toString();
        }


        // Compose filter for list of messages
        String and = bundle.getString("filter_and");

        if (StringUtils.isNotBlank(request.getParameter("active")) && Boolean.valueOf(request.getParameter("active"))) {
            txt.append(bundle.getString("filter_active")).append(" ");
        }

        // If only the virtual "General" area has been selected, prepend it to the main types
        List<Integer> areaIds = StringUtils.isNotBlank(request.getParameter("areaId"))
                ? Arrays.stream(request.getParameterValues("areaId")).map(Integer::valueOf).collect(Collectors.toList())
                : new ArrayList<>();
        if (areaIds.contains(GENERAL_AREA.getId()) && areaIds.size() == 1) {
            space(txt).append(bundle.getString("filter_general")).append(" ");
            areaIds.remove(GENERAL_AREA.getId());
        }

        // Add the selected main types
        String[] mainTypes =  request.getParameterValues("mainType");
        if (mainTypes != null && mainTypes.length == 1) {
            txt.append(bundle.getString("filter_type_" + mainTypes[0].toLowerCase()));
        } else {
            txt.append(bundle.getString("filter_type_nm")).append(and).append(bundle.getString("filter_type_nw"));
        }

        if (!areaIds.isEmpty()) {

            // Treat general area separately
            boolean generalArea = areaIds.remove(GENERAL_AREA.getId());

            List<String> areaNames = areaIds.stream()
                    .map(id -> messageService.getArea(id))
                    .filter(area -> area != null && area.getDescs() != null)
                    .map(area -> {
                        AreaDescVo desc = area.getDesc(language) != null
                                ? area.getDesc(language)
                                : area.getDescs().get(0);
                        return desc.getName();
                    })
                    .collect(Collectors.toList());

            if (!areaNames.isEmpty()) {
                space(txt).append(bundle.getString("filter_in_areas")).append(" ");
                for (int x = 0; x < areaNames.size(); x++) {
                    if (x > 0 && x == areaNames.size() - 1) {
                        txt.append(and);
                    } else if (x > 0) {
                        txt.append(", ");
                    }
                    txt.append(areaNames.get(x));
                }
            }

            if (generalArea) {
                space(txt).append(bundle.getString("filter_plus"))
                        .append(" ")
                        .append(bundle.getString("general_msgs").toLowerCase());
            }
        }

        SimpleDateFormat format = new SimpleDateFormat(bundle.getString("filter_date_format"), locale);
        format.setTimeZone(timeZone);
        space(txt).append(bundle.getString("filter_date_at"))
                .append(" ").append(format.format(new Date()));


        // NB: WKT is actually not currently used by the client - skip it for now



        return txt.toString();
    }


    /** Utility function that appends a space to non-empty strings **/
    private StringBuilder space(StringBuilder str) {
        if (str.length() > 0) {
            str.append(" ");
        }
        return str;
    }


    /**
     * Generates a HTML page containing the MSI message details
     * @param request the HTTP servlet request
     * @param response the HTTP servlet response
     */
    private void generateHtmlPage(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // Normal processing
        request.getRequestDispatcher(DETAILS_JSP_FILE).include(request, response);
        response.flushBuffer();
    }


    /**
     * Generates a PDF file containing the MSI message details
     * @param request the HTTP servlet request
     * @param response the HTTP servlet response
     */
    private void generatePdfFile(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        //Capture the content for this request
        ContentCaptureServletResponse capContent = new ContentCaptureServletResponse(response);
        request.getRequestDispatcher(DETAILS_JSP_FILE).include(request, capContent);

        // Check if there is content. Could be a redirect...
        if (!capContent.hasContent()) {
            return;
        }

        try {
            // Clean up the response HTML to a document that is readable by the XHTML renderer.
            String content = capContent.getContent();
            Document xhtmlContent = cleanHtml(content);

            long t0 = System.currentTimeMillis();
            String baseUri = "http://localhost:" + System.getProperty("swarm.http.port", "8080");
            log.info("Generating PDF for " + baseUri);

            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocument(xhtmlContent, baseUri);
            renderer.layout();

            response.setContentType("application/pdf");
            if (StringUtils.isNotBlank(request.getParameter("attachment"))) {
                response.setHeader("Content-Disposition", "attachment; filename=" + request.getParameter("attachment"));
            }
            OutputStream browserStream = response.getOutputStream();
            renderer.createPDF(browserStream);

            log.info("Completed PDF generation in " + (System.currentTimeMillis() - t0) + " ms");
        } catch (DocumentException e) {
            throw new ServletException(e);
        }
    }


    /**
     * Use JTidy to clean up the HTML
     * @param html the HTML to clean up
     * @return the resulting XHTML
     */
    public Document cleanHtml(String html) {
        Tidy tidy = new Tidy();

        tidy.setShowWarnings(false); //to hide errors
        tidy.setQuiet(true); //to hide warning

        tidy.setXHTML(true);
        return tidy.parseDOM(new StringReader(html), new StringWriter());
    }


    /**
     * Response wrapper
     * Collects all contents
     */
    public static class ContentCaptureServletResponse extends HttpServletResponseWrapper {

        private StringWriter contentWriter;
        private PrintWriter writer;

        /**
         * Constructor
         * @param originalResponse the original response
         */
        public ContentCaptureServletResponse(HttpServletResponse originalResponse) {
            super(originalResponse);
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            throw new IllegalStateException("Call getWriter()");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public PrintWriter getWriter() throws IOException {
            if(writer == null){
                contentWriter = new StringWriter();
                writer = new PrintWriter(contentWriter);
            }
            return writer;
        }

        /**
         * Returns if the response contains content
         * @return if the response contains content
         */
        public boolean hasContent() {
            return (writer != null);
        }

        /**
         * Returns the contents of the response as a string
         * @return the contents of the response as a string
         */
        public String getContent(){
            if (writer == null) {
                return "<html/>";
            }
            writer.flush();
            return contentWriter.toString();
        }
    }
}
