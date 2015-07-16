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

import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.*;
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
                        if (build.getBuildStatus().isSuccessful())
                            return;
                        if (containingChanges.size() <= 1)
                            return;
                        triggerCulpritFinding(buildType, build, previousBuild, containingChanges);
                        break; // for now we only allow a single SinCity feature (see SinCityBuildFeature.isMultipleFeaturesPerBuildTypeAllowed())
                    }
                }
            }

            private void triggerCulpritFinding(
                    SBuildType buildType,
                    SRunningBuild thisBuild,
                    SFinishedBuild previousBuild,
                    List<SVcsModification> containingChanges)
            {
                // identify the build problems
                // TODO: we won't do anything about these as yet but later these could be used with a feature "only run SinCity if there are new failures"
                /*
                final List<BuildProblemData> failureReasons = thisBuild.getFailureReasons();
                for (BuildProblemData failureReason : failureReasons) {
                    Loggers.SERVER.warn("Failure [" + failureReason.getIdentity() + "]; " + failureReason.getDescription() + "|" + failureReason);
                }
                Loggers.SERVER.warn("Have " + thisBuild.getTestMessages(0, -1).size() + " messages.");
                for (TestInfo info : thisBuild.getTestMessages(0, -1)) {
                    Loggers.SERVER.warn("Test " + info.getName() + ", status " + info.getStatus());
                }
                */

                Loggers.SERVER.info("[SinCity] will look for culprit");
//                Loggers.SERVER.info(">>>>>>> Is historical: " + thisBuild.isOutdated());
//                Loggers.SERVER.info(">>>>>>> # changes " + containingChanges.size());
//                Loggers.SERVER.info(">>>>>>> Failed thisBuild after change " + containingChanges.get(0).getDescription());

                // create a list of suspects
                List<SVcsModification> suspectChanges = new ArrayList<SVcsModification>(containingChanges);
                suspectChanges.remove(0);
                Collections.reverse(suspectChanges);

                BuildCustomizer buildCustomizer = buildCustomizerFactory.createBuildCustomizer(buildType, null);
                for (SVcsModification change : suspectChanges) {
                    Loggers.SERVER.info("[SinCity] Queueing change " + change.getDescription() + " having failed build " + thisBuild.getBuildNumber());
                    buildCustomizer.setChangesUpTo(change);
                    Map<String, String> parameters = new HashMap<String, String>();
                    parameters.put("triggered.by.sin.city", thisBuild.getBuildNumber());
                    buildCustomizer.setParameters(parameters);
                    buildCustomizer.createPromotion().addToQueue("SinCity, rerun of " + thisBuild.getBuildDescription());
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

        });
    }

}
