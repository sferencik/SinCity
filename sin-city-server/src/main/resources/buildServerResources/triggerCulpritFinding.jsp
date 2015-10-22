<%@include file="/include.jsp"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<jsp:useBean id="settingName" class="sferencik.teamcity.sincity.SettingNames"/>
<jsp:useBean id="parameterName" class="sferencik.teamcity.sincity.ParameterNames"/>
<jsp:useBean id="fileName" class="sferencik.teamcity.sincity.FileNames"/>
<jsp:useBean id="formTarget" class="sferencik.teamcity.sincity.manualTrigger.FormTarget"/>

<form action="<c:url value="${formTarget.url}"/>" id="triggerSinCity" method="post" class="clearfix">
    <table class="runnerFormTable featureDetails" style="width: 60%">
        <tr>
            <th>Triggering</th>
            <td>
                <table>
                    <tr>
                        <th>Build problems</th>
                        <th>Test failures</th>
                    </tr>
                    <tr>
                        <td>
                            <input type="radio" name="${settingName.rbTriggerOnBuildProblem}" value="${settingName.noTrigger}" ${sinCityParameters[settingName.rbTriggerOnBuildProblem] == settingName.noTrigger ? "checked" : ""} /> no<br/>
                            <input type="radio" name="${settingName.rbTriggerOnBuildProblem}" value="${settingName.triggerOnNew}" ${empty sinCityParameters or sinCityParameters[settingName.rbTriggerOnBuildProblem] == settingName.triggerOnNew ? "checked" : ""} /> new (default)<br/>
                            <input type="radio" name="${settingName.rbTriggerOnBuildProblem}" value="${settingName.triggerOnAll}" ${sinCityParameters[settingName.rbTriggerOnBuildProblem] == settingName.triggerOnAll ? "checked" : ""} /> all<br/>
                            <span class="smallNote">Should culprit-finding builds be kicked on build problems (e.g. non-zero exit code)?</span>
                        </td>
                        <td>
                            <input type="radio" name="${settingName.rbTriggerOnTestFailure}" value="${settingName.noTrigger}" ${sinCityParameters[settingName.rbTriggerOnTestFailure] == settingName.noTrigger ? "checked" : ""} /> no<br/>
                            <input type="radio" name="${settingName.rbTriggerOnTestFailure}" value="${settingName.triggerOnNew}" ${empty sinCityParameters or sinCityParameters[settingName.rbTriggerOnTestFailure] == settingName.triggerOnNew ? "checked" : ""} /> new (default)<br/>
                            <input type="radio" name="${settingName.rbTriggerOnTestFailure}" value="${settingName.triggerOnAll}" ${sinCityParameters[settingName.rbTriggerOnTestFailure] == settingName.triggerOnAll ? "checked" : ""} /> all<br/>
                            <span class="smallNote">Should culprit-finding builds be kicked on test failures?</span>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
        <tr>
            <th>Parameters</th>
            <td>
                <table>
                    <tr>
                        <th>Build problems</th>
                        <th>Test failures</th>
                    </tr>
                    <tr>
                        <td>
                            <input type="checkbox" name="${settingName.cbSetBuildProblemJsonParameter}" id="${settingName.cbSetBuildProblemJsonParameter}" ${sinCityParameters[settingName.cbSetBuildProblemJsonParameter] ? "checked" : ""} /> send as JSON<br/>
                            <span class="smallNote">If checked, the culprit-finding builds will receive a list of build
                            problems as a JSON structure. This has the form of a build parameter
                            (<code>${parameterName.sincityBuildProblems}</code>) and a JSON file (<code>&lt;build temp
                            dir&gt;/${fileName.buildProblemJsonFilename}</code>).</span>
                        </td>
                        <td>
                            <input type="checkbox" name="${settingName.cbSetTestFailureJsonParameter}" id="${settingName.cbSetTestFailureJsonParameter}" ${sinCityParameters[settingName.cbSetTestFailureJsonParameter] ? "checked" : ""} /> send as JSON<br/>
                            <span class="smallNote">If checked, the culprit-finding builds will receive a list of test
                            failures as a JSON structure. This has the form of a build parameter
                            (<code>${parameterName.sincityTestFailures}</code>) and a JSON file (<code>&lt;build temp
                            dir&gt;/${fileName.testFailureJsonFilename}</code>).</span>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
        <tr>
            <th style="width: 100px">Range</th>
            <td>
                <c:forEach items="${buildsWithChanges}" var="buildWithChange" varStatus="loop">
                    <c:set var="build" value="${buildWithChange.build}" />
                    <c:set var="change" value="${buildWithChange.change}" />

                    <!-- if there are more than 20 builds, start by showing only the top 20 plus a "Show all builds" button -->
                    <c:if test="${loop.index == 20}">
                        <!-- the button and its Javascript -->
                        <input type="button" id="fullHistory" class="btn btn_mini submitButton" value="Show all builds" />
                        <script type="text/javascript">
                            var fullHistoryButton = document.getElementById('fullHistory');
                            fullHistoryButton.onclick = function() {
                                document.getElementById('historyTail').style.display = 'block';
                                fullHistoryButton.style.display = 'none';
                            };
                        </script>

                        <!-- start the expandable DIV -->
                        <div id="historyTail" style="display: none">
                    </c:if>

                    <input type="radio" name="${settingName.rbRangeBottom}" value="${build.buildId}"
                           <c:if test="${loop.index == 1}">checked</c:if>
                            />
                    <input type="radio" name="${settingName.rbRangeTop}" value="${build.buildId}"
                           <c:if test="${loop.index == 0}">checked</c:if>
                            />

                    <%-- #123456 (started 18 Oct 15 17:08; success; last change 654321 by dylanbob, "Don’t criticize what you can’t understand.") --%>

                    <%-- ~ #123456 --%>
                    #${build.buildNumber}

                    <%-- ~ (started 18 Oct 15 17:08; --%>
                    (started <fmt:formatDate value="${build.startDate}" pattern="dd MMM yy HH:mm" />;

                    <%-- ~ success; --%>
                    ${fn:toLowerCase(build.buildStatus.text)};

                    <%-- ~ last change 654321 by dylanbob, "Don’t criticize what you can’t understand.") --%>
                    <c:set var="changeDesc" value="${change.description}" />
                    <c:set var="changeDescLimit" value="100" />
                    last change ${change.displayVersion} by ${change.userName}, "${changeDesc.length() > changeDescLimit ? changeDesc.substring(0, changeDescLimit - 3) : changeDesc}")

                    <br/>
                </c:forEach>

                <!-- finishe the expandable DIV -->
                <c:if test="${buildsWithChanges.size() > 20}"></div></c:if>
            </td>
        </tr>
        <tr>
            <th/>
            <td>
                <input type="submit" class="btn btn_mini submitButton" value="Run" />
            </td>
        </tr>
    </table>
</form>

