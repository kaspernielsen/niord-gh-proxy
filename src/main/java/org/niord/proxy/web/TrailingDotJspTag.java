package org.niord.proxy.web;

import org.apache.commons.lang.StringUtils;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;
import java.io.StringWriter;

/**
 * JSP tag directive. Ensures that the contents end with a trailing dot character
 */
@SuppressWarnings("unused")
public class TrailingDotJspTag extends SimpleTagSupport {
    @Override public void doTag() throws JspException, IOException {
        final JspWriter jspWriter = getJspContext().getOut();
        final StringWriter stringWriter = new StringWriter();
        final StringBuilder bodyContent = new StringBuilder();

        // Execute the tag's body into an internal writer
        getJspBody().invoke(stringWriter);

        String body = stringWriter.toString();
        if (StringUtils.isNotBlank(body) && !body.trim().endsWith(".")) {
            body = body.trim() + ".";

            // Output to the JSP writer
            jspWriter.write(body);
        }
    }
}
