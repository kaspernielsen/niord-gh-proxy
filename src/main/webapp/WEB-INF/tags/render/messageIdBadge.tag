<%@ tag body-content="empty" %>
<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn"  uri="http://java.sun.com/jsp/jstl/functions"%>

<%@ attribute name="msg" rtexprvalue="true" required="true" type="org.niord.model.message.MessageVo"  description="Message to render ID badge for" %>

<c:if test="${not empty msg.shortId}">
    <span class="label-message-${fn:toLowerCase(msg.mainType)}">${msg.shortId}</span>
    <c:if test="${msg.type == 'PRELIMINARY_NOTICE'}"><span class="label-message-suffix"> (P)</span></c:if>
    <c:if test="${msg.type == 'TEMPORARY_NOTICE'}"><span class="label-message-suffix"> (T)</span></c:if>
</c:if>

<c:if test="${empty msg.shortId}">
    <span class="label-message-${fn:toLowerCase(msg.mainType)}">
        <fmt:message key="${msg.type}"/>
        <fmt:message key="${msg.mainType}"/>
    </span>
</c:if>
