package sferencik.teamcity.sincity.autoTrigger;

import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;
import sferencik.teamcity.sincity.CulpritFinder;
import sferencik.teamcity.sincity.SettingNames;
import sferencik.teamcity.sincity.SinCityBuildFeature;
import sferencik.teamcity.sincity.SinCityUtils;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class BuildStatusListener
{
    public BuildStatusListener(@NotNull final EventDispatcher<BuildServerListener> listener,
                               final BuildCustomizerFactory buildCustomizerFactory,
                               final BuildQueue buildQueue,
                               final BuildsManager buildsManager)
    {
        listener.addListener(new BuildServerAdapter()
        {
            /**
             * When a build is starting, check if its build configuration has the SinCity build feature enabled. If so,
             * tag the build as appropriate.
             * @param build the starting build
             */
            @Override
            public void buildStarted(@NotNull SRunningBuild build)
            {
                Loggers.SERVER.debug("[SinCity] build starting: " + build);
                super.buildStarted(build);

                final SBuildType buildType = build.getBuildType();
                if (buildType == null)
                    return;

                SBuildFeatureDescriptor sinCityFeature = SinCityBuildFeature.getSinCityFeature(buildType);
                if (sinCityFeature == null)
                    return;

                new BuildTagger(build, sinCityFeature.getParameters()).tagBuild();
            }

            /**
             * When a build is finishing, check if its build configuration has the SinCity build feature enabled. If so,
             * trigger culprit finding if needed.
             * @param build the finishing build
             */
            @Override
            public void buildFinished(@NotNull SRunningBuild build)
            {
                Loggers.SERVER.debug("[SinCity] build finishing: " + build);
                super.buildFinished(build);

                final SBuildType buildType = build.getBuildType();
                if (buildType == null)
                    return;

                SBuildFeatureDescriptor sinCityFeature = SinCityBuildFeature.getSinCityFeature(buildType);
                if (sinCityFeature == null)
                    return;

                if (build.isOutdated()) {
                    Loggers.SERVER.info("[SinCity] the build is historical; will not trigger culprit finding");
                    return;
                }

                Map<String, String> parameters = sinCityFeature.getParameters();
                SettingNames settingNames = new SettingNames();
                String triggerOnNew = settingNames.getTriggerOnNew();

                String rbTriggerOnBuildProblems = parameters.get(settingNames.getRbTriggerOnBuildProblem());
                String rbTriggerOnTestFailures = parameters.get(settingNames.getRbTriggerOnTestFailure());

                // find the previous finished build
                List<SFinishedBuild> buildTypeHistory = SinCityUtils.getFullHistory(buildType);

                // remove the current build, which is now already in the history (presumably at position zero)
                // ~ first get hold of the SFinishedBuild representation of "build" (the build *is* finished by now,
                //   which is kind-of confirmed by the "TODO" comment in the OpenAPI documentation, see
                //   http://javadoc.jetbrains.net/teamcity/openapi/current/jetbrains/buildServer/serverSide/BuildServerAdapter.html#buildFinished(jetbrains.buildServer.serverSide.SRunningBuild)
                //   which says that in the future the buildFinished() event will accept an instance of SFinishedBuild
                //   rather than an instance of SRunningBuild
                SFinishedBuild buildAsFinished = (SFinishedBuild) buildsManager.findBuildInstanceById(build.getBuildId());
                // ~ remove it from the history
                buildTypeHistory.remove(buildAsFinished);

                new CulpritFinder(
                        build,
                        buildTypeHistory.isEmpty() ? null : buildTypeHistory.get(0),
                        rbTriggerOnBuildProblems == null ? triggerOnNew : rbTriggerOnBuildProblems,
                        rbTriggerOnTestFailures == null ? triggerOnNew : rbTriggerOnTestFailures,
                        buildCustomizerFactory,
                        buildQueue,
                        true,
                        "SinCity"
                ).triggerCulpritFindingIfNeeded();
            }
        });
    }
}
