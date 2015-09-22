<%@ include file="/include-internal.jsp"%>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%--
    /*
    *    This file is part of TeamCity Graphite.
    *
    *    TeamCity Graphite is free software: you can redistribute it and/or modify
    *    it under the terms of the GNU General Public License as published by
    *    the Free Software Foundation, either version 3 of the License, or
    *    (at your option) any later version.
    *
    *    TeamCity Graphite is distributed in the hope that it will be useful,
    *    but WITHOUT ANY WARRANTY; without even the implied warranty of
    *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    *    GNU General Public License for more details.
    *
    *    You should have received a copy of the GNU General Public License
    *    along with TeamCity Graphite.  If not, see <http://www.gnu.org/licenses/>.
    */
  --%>

<jsp:useBean id="keys" class="sferencik.teamcity.sincity.SettingNames"/>

<tr>
    <td colspan="2">Specify SinCity details</td>
</tr>
<l:settingsGroup title="SinCity parameters">
    <tr>
        <th>Tag for SinCity builds<l:star/></th>
        <td>
            <props:textProperty name="${keys.tagNameForBuildsTriggeredBySinCity}"/>
            <span class="smallNote">If specified, builds triggered by SinCity will be tagged with this</span>
        </td>
    </tr>
    <tr>
        <th>Tag for non-SinCity builds<l:star/></th>
        <td>
            <props:textProperty name="${keys.tagNameForBuildsNotTriggeredBySinCity}"/>
            <span class="smallNote">If specified, builds <em>not</em> triggered by SinCity will be tagged with this</span>
        </td>
    </tr>
    <tr>
        <th>Trigger on build problems</th>
        <td>
            <props:radioButtonProperty name="${keys.rbTriggerOnBuildProblem}" value="No" /> no
            <props:radioButtonProperty name="${keys.rbTriggerOnBuildProblem}" value="New" /> new (default)
            <props:radioButtonProperty name="${keys.rbTriggerOnBuildProblem}" value="All" /> all
            <span class="smallNote">Should culprit finding builds be kicked on build problems (e.g. non-zero exit code)?</span>
        </td>
    </tr>
    <tr>
        <th>Trigger on test failures</th>
        <td>
            <props:radioButtonProperty name="${keys.rbTriggerOnTestFailure}" value="No" /> no
            <props:radioButtonProperty name="${keys.rbTriggerOnTestFailure}" value="New" /> new (default)
            <props:radioButtonProperty name="${keys.rbTriggerOnTestFailure}" value="All" /> all
            <span class="smallNote">Should culprit finding builds be kicked on test failures?</span>
        </td>
    </tr>
</l:settingsGroup>
