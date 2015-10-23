package sferencik.teamcity.sincity;

import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.parameters.ValueResolver;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BuildTagger {
    private final SRunningBuild build;
    private final Map<String, String> sinCityParameters;

    public BuildTagger(SRunningBuild build, Map<String, String> sinCityParameters) {
        this.build = build;
        this.sinCityParameters = sinCityParameters;
    }

    /**
     * Tag the finishing build (if requested in the config) as either
     * 1) triggered by SinCity
     * 2) not triggered by SinCity
     */
    void tagBuild() {
        // tag the finished build
        SettingNames settingNames = new SettingNames();

        // if this is a SinCity-triggered build, we know for sure that the getSincityRangeTopBuildId() parameter is set;
        // if, on the other hand, it is a non-SinCity-triggered run, it may still be set (and hopefully empty) if the
        // user has set the parameter up in their build configuration; therefore, don't just test for null and accept
        // empty as a sign of a non-SinCity-triggered build
        final String sincityRangeTopBuildId = build.getParametersProvider().get(new ParameterNames().getSincityRangeTopBuildId());
        String tagParameterName = StringUtil.isEmpty(sincityRangeTopBuildId)
                ? settingNames.getTagNameForBuildsNotTriggeredBySinCity()
                : settingNames.getTagNameForBuildsTriggeredBySinCity();

        String unresolvedTagName = sinCityParameters.get(tagParameterName);
        if (StringUtil.isEmpty(unresolvedTagName))
            return;

        ValueResolver resolver = build.getValueResolver();
        final String resolvedTagName = resolver.resolve(unresolvedTagName).getResult();

        Loggers.SERVER.debug("[SinCity] tagging build with '" + resolvedTagName + "'");
        final List<String> resultingTags = new ArrayList<String>(build.getTags());
        resultingTags.add(resolvedTagName);
        build.setTags(resultingTags);
    }

}
