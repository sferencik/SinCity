<%@ include file="/include-internal.jsp"%>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>

<jsp:useBean id="settingName" class="sferencik.teamcity.sincity.SettingNames"/>

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
                        <props:radioButtonProperty name="${settingName.rbTriggerOnBuildProblem}" value="${settingName.noTrigger}" /> no<br/>
                        <props:radioButtonProperty name="${settingName.rbTriggerOnBuildProblem}" value="${settingName.triggerOnNew}" /> new (default)<br/>
                        <props:radioButtonProperty name="${settingName.rbTriggerOnBuildProblem}" value="${settingName.triggerOnAll}" /> all
                        <span class="smallNote">Should culprit-finding builds be kicked on build problems (e.g. non-zero exit code)?</span>
                    </td>
                    <td>
                        <props:radioButtonProperty name="${settingName.rbTriggerOnTestFailure}" value="${settingName.noTrigger}" /> no<br/>
                        <props:radioButtonProperty name="${settingName.rbTriggerOnTestFailure}" value="${settingName.triggerOnNew}" /> new (default)<br/>
                        <props:radioButtonProperty name="${settingName.rbTriggerOnTestFailure}" value="${settingName.triggerOnAll}" /> all
                        <span class="smallNote">Should culprit-finding builds be kicked on test failures?</span>
                    </td>
                </tr>
            </table>
        </td>
    </tr>
</l:settingsGroup>
