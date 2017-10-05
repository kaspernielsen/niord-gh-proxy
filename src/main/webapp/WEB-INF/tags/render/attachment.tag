<%@ tag body-content="empty" %>
<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn"  uri="http://java.sun.com/jsp/jstl/functions"%>

<%@ attribute name="att" rtexprvalue="true" required="true" type="org.niord.model.message.AttachmentVo"  description="Attachment to render" %>

<c:choose>
    <c:when test="${not empty att.width && not empty att.height}">
        <c:set var="imageStyle" value="max-width: 100%; width: ${att.width}; height: ${att.height};"/>
    </c:when>
    <c:when test="${not empty att.width}">
        <c:set var="imageStyle" value="max-width: 100%; width: ${att.width};"/>
    </c:when>
    <c:when test="${not empty att.height}">
        <c:set var="imageStyle" value="max-width: 100%; height: ${att.height};"/>
    </c:when>
    <c:otherwise>
        <c:set var="imageStyle" value="max-width: 100%;"/>
    </c:otherwise>
</c:choose>

<div class="attachment">
    <div>
        <img src="${att.path}" style="${imageStyle}">
    </div>
    <c:if test="${not empty att.descs && not empty att.descs[0].caption}">
        <div class="attachment-label">${att.descs[0].caption}</div>
    </c:if>
</div>
