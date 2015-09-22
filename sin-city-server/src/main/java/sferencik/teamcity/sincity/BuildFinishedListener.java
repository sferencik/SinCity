package sferencik.teamcity.sincity;

import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;

public class BuildFinishedListener
{

    public BuildFinishedListener(@NotNull final EventDispatcher<BuildServerListener> listener,
                                 final BuildCustomizerFactory buildCustomizerFactory)
    {
        listener.addListener(new BuildServerAdapter()
        {
            @Override
            public void buildFinished(@NotNull SRunningBuild build)
            {
                Loggers.SERVER.debug("[SinCity] build: " + build);

                final SBuildType buildType = build.getBuildType();
                if (buildType == null)
                    return;

                for (SBuildFeatureDescriptor feature : buildType.getBuildFeatures()) {
                    final Class<? extends BuildFeature> featureClass = feature.getBuildFeature().getClass();
                    Loggers.SERVER.debug("[SinCity] found plugin: " + featureClass);
                    if (!featureClass.equals(SinCityBuildFeature.class))
                        continue;

                    final SinCity sinCity = new SinCity(build, buildCustomizerFactory, feature.getParameters());
                    sinCity.tagBuild();
                    sinCity.triggerCulpritFindingIfNeeded();

                    // we only allow a single SinCity feature (see
                    // SinCityBuildFeature.isMultipleFeaturesPerBuildTypeAllowed()) so there's no point continuing
                    break;
                }
            }

        });
    }
}
