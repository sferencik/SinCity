package sferencik.teamcity.sincity;

import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.BuildFeature;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.SBuildType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SinCityUtils {
    /**
     * Look through the build type's build features and return a feature descriptor for the SinCity feature if it's
     * enabled.
     *
     * We only allow a single SinCity feature (see SinCityBuildFeature.isMultipleFeaturesPerBuildTypeAllowed()) so this
     * either returns an instance or null.
     *
     * @param buildType the build configuration to inspect
     * @return an SBuildFeatureDescriptor corresponding to SinCity, or null
     */
    @Nullable
    public static SBuildFeatureDescriptor getSinCityFeature(@NotNull SBuildType buildType) {
        for (SBuildFeatureDescriptor feature : buildType.getBuildFeatures()) {
            final Class<? extends BuildFeature> featureClass = feature.getBuildFeature().getClass();
            Loggers.SERVER.debug("[SinCity] found plugin: " + featureClass);
            if (!featureClass.equals(SinCityBuildFeature.class))
                continue;

            Loggers.SERVER.debug("[SinCity] found SinCity");
            if (!buildType.isEnabled(feature.getId()))
                return null;

            Loggers.SERVER.debug("[SinCity] SinCity enabled");
            return feature;
        }
        return null;
    }
}
