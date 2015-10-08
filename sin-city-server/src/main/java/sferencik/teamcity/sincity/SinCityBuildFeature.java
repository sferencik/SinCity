package sferencik.teamcity.sincity;

import jetbrains.buildServer.serverSide.BuildFeature;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
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
    public String describeParameters(@NotNull Map<String, String> params)
    {
        return "Find the culprit of a broken build " +
                "(" +
                params.get(new SettingNames().getRbTriggerOnBuildProblem()).toLowerCase() + " build problems, " +
                params.get(new SettingNames().getRbTriggerOnTestFailure()).toLowerCase() + " test failures" +
                ")";
    }

    @Nullable
    @Override
    public PropertiesProcessor getParametersProcessor()
    {
        return new PropertiesProcessor()
        {
            @NotNull
            public Collection<InvalidProperty> process(@Nullable final Map<String, String> propertiesMap)
            {
                // anything goes
                return new ArrayList<InvalidProperty>();
            }
        };
    }

    @Nullable
    @Override
    public Map<String, String> getDefaultParameters()
    {
        HashMap<String, String> defaultParameters = new HashMap<String, String>();
        defaultParameters.put(new SettingNames().getRbTriggerOnBuildProblem(), "New");
        defaultParameters.put(new SettingNames().getRbTriggerOnTestFailure(), "New");
        return defaultParameters;
    }

    @Override
    public boolean isMultipleFeaturesPerBuildTypeAllowed() {
        return false;
    }
}
