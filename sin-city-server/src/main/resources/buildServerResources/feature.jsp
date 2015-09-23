<%@ include file="/include-internal.jsp"%>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>

<jsp:useBean id="settingName" class="sferencik.teamcity.sincity.SettingNames"/>
<jsp:useBean id="parameterName" class="sferencik.teamcity.sincity.ParameterNames"/>

<l:settingsGroup title="SinCity parameters">
    <tr>
        <th>Tagging</th>
        <td>
            <props:textProperty name="${settingName.tagNameForBuildsTriggeredBySinCity}"/> SinCity builds
            <span class="smallNote">If specified, builds triggered by SinCity will be tagged with this</span>
            <props:textProperty name="${settingName.tagNameForBuildsNotTriggeredBySinCity}"/> non-SinCity builds
            <span class="smallNote">If specified, builds <em>not</em> triggered by SinCity will be tagged with this</span>
        </td>
    </tr>
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
                        <props:radioButtonProperty name="${settingName.rbTriggerOnBuildProblem}" value="No" /> no<br/>
                        <props:radioButtonProperty name="${settingName.rbTriggerOnBuildProblem}" value="New" /> new (default)<br/>
                        <props:radioButtonProperty name="${settingName.rbTriggerOnBuildProblem}" value="All" /> all
                        <span class="smallNote">Should culprit-finding builds be kicked on build problems (e.g. non-zero exit code)?</span>
                    </td>
                    <td>
                        <props:radioButtonProperty name="${settingName.rbTriggerOnTestFailure}" value="No" /> no<br/>
                        <props:radioButtonProperty name="${settingName.rbTriggerOnTestFailure}" value="New" /> new (default)<br/>
                        <props:radioButtonProperty name="${settingName.rbTriggerOnTestFailure}" value="All" /> all
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
                        <props:checkboxProperty name="${settingName.cbSetBuildProblemJsonParameter}" />
                        <code>${parameterName.sincityBuildProblems}</code><br /> <span class="smallNote">If checked, the
                        culprit-finding builds will receive a JSON-string parameter containing a list of build problems,
                        filtered as per the triggering rules above.</span>
                    </td>
                    <td>
                        <props:checkboxProperty name="${settingName.cbSetTestFailureJsonParameter}" />
                        <code>${parameterName.sincityTestFailures}</code><br /> <span class="smallNote">If checked, the
                        culprit-finding builds will receive a JSON-string parameter containing a list of test failures,
                        filtered as per the triggering rules above.</span>
                    </td>
                </tr>
            </table>
        </td>
    </tr>
</l:settingsGroup>
