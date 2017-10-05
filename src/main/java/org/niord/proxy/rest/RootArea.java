package org.niord.proxy.rest;

import org.apache.commons.lang.StringUtils;
import org.niord.model.message.AreaVo;

/**
 * Extends AreaVo with position and zoom level.
 *
 * Initialized with an area spec with the format "areaId|latitude|longitude|zoomLevel"
 * with the last three components being optional.
 */
@SuppressWarnings("unused")
public class RootArea extends AreaVo {

    String areaId;
    Float latitude;
    Float longitude;
    Integer zoomLevel;

    /** No-arg constructor **/
    public RootArea() {
    }

    /**
     * Initializes the root area from an area specification
     * @param areaSpec the area specification
     */
    public RootArea(String areaSpec) {
        if (StringUtils.isNotBlank(areaSpec)) {
            String[] parts = areaSpec.trim().split("\\|");
            if (parts.length > 0) {
                areaId = parts[0];
                if (parts.length == 4) {
                    latitude = Float.valueOf(parts[1].trim());
                    longitude = Float.valueOf(parts[2].trim());
                    zoomLevel = Integer.valueOf(parts[3].trim());
                }
            }
        }
    }

    /** Sets the actual area for this root area **/
    public RootArea setArea(AreaVo area) {
        setId(area.getId());
        setMrn(area.getMrn());
        setDescs(area.getDescs());
        setActive(area.isActive());
        return this;
    }

    public String getAreaId() {
        return areaId;
    }

    public Float getLatitude() {
        return latitude;
    }

    public Float getLongitude() {
        return longitude;
    }

    public Integer getZoomLevel() {
        return zoomLevel;
    }
}
