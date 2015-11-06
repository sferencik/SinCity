package sferencik.teamcity.sincity;

import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class BuildStatusListener
{
    public BuildStatusListener(@NotNull final EventDispatcher<BuildServerListener> listener,
                               final BuildCustomizerFactory buildCustomizerFactory,
                               final BuildQueue buildQueue)
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

                SBuildFeatureDescriptor sinCityFeature = SinCityUtils.getSinCityFeature(buildType);
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

                SBuildFeatureDescriptor sinCityFeature = SinCityUtils.getSinCityFeature(buildType);
                if (sinCityFeature == null)
                    return;

                Map<String, String> parameters = sinCityFeature.getParameters();
                SettingNames settingNames = new SettingNames();
                String triggerOnNew = settingNames.getTriggerOnNew();

                String rbTriggerOnBuildProblems = parameters.get(settingNames.getRbTriggerOnBuildProblem());
                String rbTriggerOnTestFailures = parameters.get(settingNames.getRbTriggerOnTestFailure());

                new CulpritFinder(
                        build,
                        build.getPreviousFinished(),
                        rbTriggerOnBuildProblems == null ? triggerOnNew : rbTriggerOnBuildProblems,
                        rbTriggerOnTestFailures == null ? triggerOnNew : rbTriggerOnTestFailures,
                        buildCustomizerFactory,
                        buildQueue,
                        true
                )
                    .triggerCulpritFindingIfNeeded();
            }
        });
    }
}
