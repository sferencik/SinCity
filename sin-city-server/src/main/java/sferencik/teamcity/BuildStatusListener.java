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
            public void buildFinished(SRunningBuild build)
            {
                Loggers.SERVER.info("      *******************");
                Loggers.SERVER.info("      * build id: " + build.getBuildId());
                Loggers.SERVER.info("      * build successful: " + build.getBuildStatus().isSuccessful());
                Loggers.SERVER.info("      * build changes: " + build.getContainingChanges().size());
                Loggers.SERVER.info("      * build last change: " + build.getContainingChanges().get(0).getDescription());

                // does this build config use SinCity?
                final SBuildType buildType = build.getBuildType();
                if (buildType == null)
                    return;

                for (SBuildFeatureDescriptor feature : buildType.getBuildFeatures()) {
                    if (feature.getClass().equals(SinCityBuildFeature.class)) {
                        buildFinishedWithSinCity(build, buildType, feature);
                        break; // for now we only allow a single SinCity feature (see SinCityBuildFeature.isMultipleFeaturesPerBuildTypeAllowed())
                    }
                }
            }

            private void buildFinishedWithSinCity(SRunningBuild build, SBuildType buildType, SBuildFeatureDescriptor sinCityFeature)
            {
                // tag the finished build
                final String triggeredBySinCityParameterValue = build.getParametersProvider().get(triggeredBySinCityParameterName);
                String tagParameterName = triggeredBySinCityParameterValue == null
                        ? "nonSinCityTag"
                        : "sinCityTag";
                Loggers.SERVER.info(">>> will get '" + tagParameterName + "'");
                final String tagName = sinCityFeature.getParameters().get(tagParameterName);
                Loggers.SERVER.info(">>> value: '" + tagName + "'");
                if (tagName != null && !tagName.isEmpty()) {
                    final List<String> resultingTags = new ArrayList<String>(build.getTags());
                    resultingTags.add(tagName);
                    build.setTags(resultingTags);
                    Loggers.SERVER.info(">>> set tags: '" + build.getTags() + "'");
                }

                // nothing to investigate unless we've failed
                if (build.getBuildStatus().isSuccessful())
                    return;

                Loggers.SERVER.info(">>>> SinCity is on!");

                // identify the build problems
                // TODO: we won't do anything about these as yet but later these could be used with a feature "only run SinCity if there are new failures"
                /*
                final List<BuildProblemData> failureReasons = build.getFailureReasons();
                for (BuildProblemData failureReason : failureReasons) {
                    Loggers.SERVER.warn("Failure [" + failureReason.getIdentity() + "]; " + failureReason.getDescription() + "|" + failureReason);
                }
                Loggers.SERVER.warn("Have " + build.getTestMessages(0, -1).size() + " messages.");
                for (TestInfo info : build.getTestMessages(0, -1)) {
                    Loggers.SERVER.warn("Test " + info.getName() + ", status " + info.getStatus());
                }
                */

                // identify the culprits (the intermediate changes)
                final List<SVcsModification> containingChanges = build.getContainingChanges();

                Loggers.SERVER.info(">>>>>>> Is historical: " + build.isOutdated());
                Loggers.SERVER.info(">>>>>>> # changes " + containingChanges.size());
                Loggers.SERVER.info(">>>>>>> Failed build after change " + containingChanges.get(0).getDescription());


                if (containingChanges.size() <= 1)
                    return;

                // do not repeat the run for the last revision
                containingChanges.remove(0);

                Collections.reverse(containingChanges);

                BuildCustomizer buildCustomizer = buildCustomizerFactory.createBuildCustomizer(buildType, null);
                for (SVcsModification change : containingChanges) {
                    Loggers.SERVER.info(">>> Queueing change " + change.getDescription() + " having failed build " + build.getBuildNumber());
                    buildCustomizer.setChangesUpTo(change);
                    Map<String, String> parameters = new HashMap<String, String>();
                    parameters.put("triggered.by.sin.city", build.getBuildNumber());
                    buildCustomizer.setParameters(parameters);
                    buildCustomizer.createPromotion().addToQueue("SinCity, rerun of " + build.getBuildNumber());
                }
            }

        });
    }

}
