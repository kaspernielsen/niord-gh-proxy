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
import org.niord.model.message.AreaVo;
import org.niord.model.message.AttachmentVo;
import org.niord.model.message.AttachmentVo.AttachmentDisplayType;
import org.niord.model.message.MessageVo;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * Defines a set of TLD functions that may be used on a JSP page
 */
@SuppressWarnings("unused")
public class TldFunctions {

    /**
     * Returns the area heading to display for a message
     * @param msg the message
     * @return the area heading to display for a message
     */
    public static AreaVo getAreaHeading(MessageVo msg) {
        AreaVo area = msg.getAreas() != null && !msg.getAreas().isEmpty()
                ? msg.getAreas().get(0)
                : null;
        while (area != null && area.getParent() != null && area.getParent().getParent() != null) {
            area = area.getParent();
        }
        return area;
    }


    /**
     * Returns the area to display for a message. If lineage is set to true,
     * the area lineage is emitted
     * @param area the area
     * @param lineage if the area lineage should be emitted
     * @return the area display for a message
     */
    public static String renderMessageArea(AreaVo area, boolean lineage) {
        String result = "";
        while (area != null) {
            if (area.getDescs() != null && !area.getDescs().isEmpty()) {
                if (result.length() > 0) {
                    result = " - " + result;
                }
                result = area.getDescs().get(0).getName() + result;
            }

            area = lineage ? area.getParent() : null;
        }
        return result;
    }


    /**
     * Returns the source to display for a message.
     * @param msg the message
     * @return the source display for a message
     */
    public static String renderMessageSource(MessageVo msg, Locale locale, String timeZoneId) {
        String result = "";
        if (msg != null) {
            if (msg.getDescs() != null && !msg.getDescs().isEmpty()
                    && StringUtils.isNotBlank(msg.getDescs().get(0).getSource())) {
                result += msg.getDescs().get(0).getSource();
            }
            if (msg.getPublishDateFrom() != null) {
                TimeZone timeZone = TimeZone.getTimeZone(timeZoneId);
                ResourceBundle bundle = ResourceBundle.getBundle("MessageDetails", locale);
                SimpleDateFormat format = new SimpleDateFormat(bundle.getString("source_date_format"), locale);
                format.setTimeZone(timeZone);

                if (StringUtils.isNotBlank(result)) {
                    if (!result.endsWith(".")) {
                        result += ".";
                    }
                    result += " ";
                }
                result += bundle.getString("source_published") + " " + format.format(msg.getPublishDateFrom());
            }
        }
        return result;
    }


    /**
     * Returns the attachments with the given display type.
     * @param msg the message
     * @param displayType the attachment display type
     * @return the area lineage to display for an area
     */
    public static List<AttachmentVo> getAttachmentsWithDisplayType(MessageVo msg, String displayType) {
        AttachmentDisplayType display = AttachmentDisplayType.valueOf(displayType);
        return msg.getAttachments() == null
                ? Collections.emptyList()
                : msg.getAttachments().stream()
                    .filter(a -> Objects.equals(display, a.getDisplay()))
                    .collect(Collectors.toList());
    }

}
