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

package sferencik.teamcity;

import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.tests.TestInfo;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.vcs.SVcsModification;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class BuildStatusListener
{
    public BuildStatusListener(@NotNull final EventDispatcher<BuildServerListener> listener,
                               final BuildCustomizerFactory buildCustomizerFactory)
    {
        listener.addListener(new BuildServerAdapter()
        {
            private String triggeredBySinCityParameterName = "triggered.by.sin.city";

            @Override
            public void buildFinished(@NotNull SRunningBuild build)
            {

                Loggers.SERVER.debug("[SinCity] build: " + build);

                final SBuildType buildType = build.getBuildType();
                if (buildType == null)
                    return;

                SFinishedBuild previousBuild = build.getPreviousFinished();
                Loggers.SERVER.debug("[SinCity] previous build: " + previousBuild);

                final List<SVcsModification> containingChanges = build.getContainingChanges();
                Loggers.SERVER.debug("[SinCity] changes (" + containingChanges.size() + "): " + containingChanges);

                for (SBuildFeatureDescriptor feature : buildType.getBuildFeatures()) {
                    final Class<? extends BuildFeature> featureClass = feature.getBuildFeature().getClass();
                    Loggers.SERVER.debug("[SinCity] found plugin: " + featureClass);
                    if (featureClass.equals(SinCityBuildFeature.class)) {
                        tagBuild(build, feature);
                        if (build.getBuildStatus().isSuccessful()) {
                            Loggers.SERVER.debug("[SinCity] the build succeeded; we're done.");
                            return;
                        }
                        if (containingChanges.size() <= 1) {
                            Loggers.SERVER.debug("[SinCity] no intermediate changes found; we're done.");
                            return;
                        }

                        if (getNewProblems(build, previousBuild).isEmpty()
                                && getRelevantTestFailures(build, previousBuild, feature).isEmpty()) {
                            Loggers.SERVER.debug("[SinCity] no new failures; we're done.");
                            return;
                        }

                        triggerCulpritFinding(buildType, build, containingChanges);

                        break; // for now we only allow a single SinCity feature (see SinCityBuildFeature.isMultipleFeaturesPerBuildTypeAllowed())
                    }
                }
            }

            private void tagBuild(SRunningBuild thisBuild, SBuildFeatureDescriptor sinCityFeature) {
                // tag the finished build
                final String triggeredBySinCityParameterValue = thisBuild.getParametersProvider().get(triggeredBySinCityParameterName);
                String tagParameterName = triggeredBySinCityParameterValue == null
                        ? "nonSinCityTag"
                        : "sinCityTag";
                final String tagName = sinCityFeature.getParameters().get(tagParameterName);
                if (tagName != null && !tagName.isEmpty()) {
                    Loggers.SERVER.debug("[SinCity] tagging build with '" + tagName + "'");
                    final List<String> resultingTags = new ArrayList<String>(thisBuild.getTags());
                    resultingTags.add(tagName);
                    thisBuild.setTags(resultingTags);
                }
            }

            private List<BuildProblemData> getNewProblems(
                    SRunningBuild thisBuild,
                    SFinishedBuild previousBuild)
            {
                final List<BuildProblemData> thisBuildProblems = thisBuild.getFailureReasons();
                Loggers.SERVER.debug("[SinCity] this build's problems: " + thisBuildProblems);

                final List<BuildProblemData> previousBuildProblems = previousBuild.getFailureReasons();
                Loggers.SERVER.debug("[SinCity] previous build's problems: " + previousBuildProblems);

                final List<BuildProblemData> newProblems = new ArrayList<BuildProblemData>(thisBuildProblems);
                newProblems.removeAll(previousBuildProblems);
                Loggers.SERVER.debug("[SinCity] new problems: " + newProblems);

                return newProblems;
            }

            private List<TestName> getRelevantTestFailures(
                    SRunningBuild thisBuild,
                    SFinishedBuild previousBuild,
                    SBuildFeatureDescriptor feature)
            {
                final List<TestName> thisBuildTestFailures = getTestNames(thisBuild.getTestMessages(0, -1));
                Loggers.SERVER.debug("[SinCity] this build's test failures: " + thisBuildTestFailures);

                final List<TestName> previousBuildTestFailures = getTestNames(previousBuild.getTestMessages(0, -1));
                Loggers.SERVER.debug("[SinCity] previous build's test failures: " + thisBuildTestFailures);

                final List<TestName> relevantTestFailures = new ArrayList<TestName>(thisBuildTestFailures);
                if (!isTriggerOnAnyTestFailure(feature))
                    relevantTestFailures.removeAll(previousBuildTestFailures);
                Loggers.SERVER.debug("[SinCity] relevant test failures: " + relevantTestFailures);

                return relevantTestFailures;
            }

            private boolean isTriggerOnAnyTestFailure(SBuildFeatureDescriptor feature) {
                final String isTriggerOnAnyTestFailureString = feature.getParameters().get("isTriggerOnAnyTestFailure");
                return isTriggerOnAnyTestFailureString == null
                        ? false
                        : Boolean.valueOf(isTriggerOnAnyTestFailureString);
            }

            private List<TestName> getTestNames(List<TestInfo> tests)
            {
                List<TestName> testNames = new ArrayList<TestName>();
                for (TestInfo test : tests) {
                    testNames.add(test.getTestName());
                }
                return testNames;
            }

            private void triggerCulpritFinding(
                    SBuildType buildType,
                    SRunningBuild thisBuild,
                    List<SVcsModification> containingChanges)
            {
                Loggers.SERVER.info("[SinCity] will look for culprit");

                List<SVcsModification> suspectChanges = new ArrayList<SVcsModification>(containingChanges);
                suspectChanges.remove(0);
                Collections.reverse(suspectChanges);

                BuildCustomizer buildCustomizer = buildCustomizerFactory.createBuildCustomizer(buildType, null);
                for (SVcsModification change : suspectChanges) {
                    Loggers.SERVER.info("[SinCity] Queueing change '" + change + "' having failed build " + thisBuild);
                    buildCustomizer.setChangesUpTo(change);
                    Map<String, String> parameters = new HashMap<String, String>();
                    parameters.put("triggered.by.sin.city", thisBuild.getBuildNumber());
                    buildCustomizer.setParameters(parameters);
                    buildCustomizer.createPromotion().addToQueue("SinCity, failures of " + thisBuild.getBuildNumber());
                }
            }
        });
    }
}
