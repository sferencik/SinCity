<%@include file="/include.jsp"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<table style="font-family: Tahoma, Verdana, sans-serif; font-size: small; border-collapse: collapse; ">
    <%--build row--%>
    <tr>
        <th style="border: 1px solid LightGray"></th>
        <c:forEach begin="0" end="${matrix.numberOfColumns - 1}" varStatus="column">
            <c:set var="cell" value="${matrix.getBuildRowCell(column.index)}" />
            <c:if test="${not empty cell}">
                <th colspan="${cell.colspan}" style="border: 1px solid LightGray">
                    <a href="${webLinks.getViewResultsUrl(cell.content.build)}">${cell.content.build.buildNumber}</a>
                </th>
            </c:if>
        </c:forEach>
    </tr>
    <%--change row--%>
    <tr>
        <th style="border: 1px solid LightGray"></th>
        <c:forEach begin="0" end="${matrix.numberOfColumns - 1}" varStatus="column">
            <c:set var="cell" value="${matrix.getChangeRowCell(column.index)}" />
            <td colspan="${cell.colspan}" style="border: 1px solid LightGray">
                <c:choose>
                    <c:when test="${empty cell.content}">
                        no change
                    </c:when>
                    <c:otherwise>
                        <a href="${webLinks.getChangeUrl(cell.content.id, false)}">${cell.content.version}</a><br />
                        ${cell.content.userName}
                    </c:otherwise>
                </c:choose>
            </td>
        </c:forEach>
    </tr>
    <%--non-header rows--%>
    <c:forEach begin="0" end="${matrix.numberOfRows - 1}" varStatus="row">
        <tr>
            <%--first column (testFailures)--%>
            <c:set var="failureCell" value="${matrix.getFailureColumnCell(row.index)}" />
            <td colspan="${failureCell.colspan}" style="border: 1px solid LightGray">${failureCell.content}</td>

            <%--table itself--%>
            <c:forEach begin="0" end="${matrix.numberOfColumns - 1}" varStatus="column">
                <c:set var="cell" value="${matrix.getCell(row.index, column.index)}" />
                <c:if test="${not empty cell}">
                    <c:choose>
                        <c:when test="${empty cell.content}">
                            <td colspan="${cell.colspan}" style="border: 1px solid LightGray;"/>
                        </c:when>
                        <c:otherwise>
                            <td colspan="${cell.colspan}" style="background-color: #eef4fa; border: 1px solid LightGray;">
                                <a href="${webLinks.getViewResultsUrl(cell.content.build)}">${cell.content.build.buildNumber}</a>
                            </td>
                        </c:otherwise>
                    </c:choose>
                </c:if>
            </c:forEach>
        </tr>
    </c:forEach>
</table>