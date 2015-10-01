package sferencik.teamcity.sincity;

import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;

public class BuildStatusListener
{
    public BuildStatusListener(@NotNull final EventDispatcher<BuildServerListener> listener,
                               final BuildCustomizerFactory buildCustomizerFactory)
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

                SBuildFeatureDescriptor sinCityFeature = getSinCityFeature(build);
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

                SBuildFeatureDescriptor sinCityFeature = getSinCityFeature(build);
                if (sinCityFeature == null)
                    return;

                new CulpritFinder(build, buildCustomizerFactory, sinCityFeature.getParameters()).triggerCulpritFindingIfNeeded();
            }

            /*
            For a given running build, look through its build type's build features and return a feature descriptor for
            the SinCity feature if it's enabled. We only allow a single SinCity feature (see
            SinCityBuildFeature.isMultipleFeaturesPerBuildTypeAllowed()) so this either returns an instance or null.
             */
            private SBuildFeatureDescriptor getSinCityFeature(SRunningBuild build) {
                final SBuildType buildType = build.getBuildType();
                if (buildType == null)
                    return null;

                for (SBuildFeatureDescriptor feature : buildType.getBuildFeatures()) {
                    final Class<? extends BuildFeature> featureClass = feature.getBuildFeature().getClass();
                    Loggers.SERVER.debug("[SinCity] found plugin: " + featureClass);
                    if (!featureClass.equals(SinCityBuildFeature.class))
                        continue;

                    Loggers.SERVER.debug("[SinCity] the SinCity plugin is enabled");
                    return feature;
                }

                return null;
            }
        });
    }
}
