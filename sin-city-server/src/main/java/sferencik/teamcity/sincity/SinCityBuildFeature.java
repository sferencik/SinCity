package sferencik.teamcity.sincity;

import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SinCityBuildFeature extends BuildFeature {
    private final PluginDescriptor descriptor;

    public SinCityBuildFeature(@NotNull final PluginDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    @NotNull
    @Override
    public String getType() {
        return "SinCity";
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "Culprit finder (SinCity)";
    }

    @Nullable
    @Override
    public String getEditParametersUrl() {
        return descriptor.getPluginResourcesPath("feature.jsp");
    }

    @NotNull
    @Override
    public String describeParameters(@NotNull Map<String, String> params) {
        return "Find the culprit of a broken build " +
                "(" +
                params.get(new SettingNames().getRbTriggerOnBuildProblem()).toLowerCase() + " build problems, " +
                params.get(new SettingNames().getRbTriggerOnTestFailure()).toLowerCase() + " test failures" +
                ")";
    }

    @Nullable
    @Override
    public Map<String, String> getDefaultParameters() {
        HashMap<String, String> defaultParameters = new HashMap<String, String>();
        defaultParameters.put(new SettingNames().getRbTriggerOnBuildProblem(), "New");
        defaultParameters.put(new SettingNames().getRbTriggerOnTestFailure(), "New");
        return defaultParameters;
    }

    @Override
    public boolean isMultipleFeaturesPerBuildTypeAllowed() {
        return false;
    }

    /**
     * Look through the build type's build features and return a feature descriptor for the SinCity feature if it's
     * enabled.
     * <p/>
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
