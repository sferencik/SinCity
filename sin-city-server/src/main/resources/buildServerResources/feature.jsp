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

<%--
jsp:useBean id="keys" class="sferencik.teamcity.sincity.BuildStatusListener"/
--%>

<tr>
    <td colspan="2">Specify SinCity details</td>
</tr>
<l:settingsGroup title="SinCity parameters">
    <tr>
        <th>Tag for SinCity builds<l:star/></th>
        <td>
            <props:textProperty name="sinCityTag"/>
            <span class="smallNote">If specified, SinCity builds will be tagged with this</span>
        </td>
    </tr>
    <tr>
        <th>Tag for non-SinCity builds<l:star/></th>
        <td>
            <props:textProperty name="nonSinCityTag"/>
            <span class="smallNote">If specified, non-SinCity builds will be tagged with this</span>
        </td>
    </tr>
    <tr>
        <th>Trigger on any test failure</th>
        <td>
            <props:checkboxProperty name="isTriggerOnAnyTestFailure"/>
            <span class="smallNote">If unchecked, culprit-finding builds are triggered only if there is a new build problem or a <em>new</em> test failure. If checked, culprit-finding builds is triggered if there is a new build problem or <em>any</em> test failure.</span>
        </td>
    </tr>
</l:settingsGroup>
