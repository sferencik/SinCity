package sferencik.teamcity.sincity.manualTrigger;

import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.openapi.buildType.BuildTypeTab;
import org.jetbrains.annotations.NotNull;
import sferencik.teamcity.sincity.FinishedBuildWithChanges;
import sferencik.teamcity.sincity.SinCityBuildFeature;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

public class ManualTriggerTab extends BuildTypeTab {

    public ManualTriggerTab(@NotNull WebControllerManager manager,
                            @NotNull ProjectManager projectManager,
                            @NotNull PluginDescriptor pluginDescriptor) {
        super(getTabCode(), "Trigger culprit finding", manager, projectManager);

        // specify path relative to the plugin; avoid TC assuming the JSP is under /plugins/[TAB CODE], which is what it
        // would do if we used this relative path in build-server-plugin-sin-city.xml's
        // bean/property[@name='includeUrl']; this is needed since getTabCode() returns a value different than the
        // plugin name; see https://devnet.jetbrains.com/message/5564684
        setIncludeUrl(pluginDescriptor.getPluginResourcesPath("triggerCulpritFinding.jsp"));
    }

    @NotNull
    static String getTabCode() {
        return "sin-city-trigger";
    }

    @Override
    public boolean isAvailable(@NotNull HttpServletRequest request) {
        SBuildType buildType = getBuildType(request);
        return buildType != null
                && !FinishedBuildWithChanges.getListFromBuildType(buildType).isEmpty()
                && super.isAvailable(request);
    }

    @Override
    protected void fillModel(@NotNull Map<String, Object> map,
                             @NotNull HttpServletRequest httpServletRequest,
                             @NotNull SBuildType sBuildType,
                             @NotNull SUser sUser) {

        /*
         * We need to send two things to the tab:
         * 1) the list of historical builds
         * 2) the parameters of the SinCity feature (if enabled)
         */
        List<FinishedBuildWithChanges> buildsWithChanges = FinishedBuildWithChanges.getListFromBuildType(sBuildType);
        map.put("buildsWithChanges", buildsWithChanges);

        SBuildFeatureDescriptor sinCityFeature = SinCityBuildFeature.getSinCityFeature(sBuildType);
        map.put("sinCityParameters", sinCityFeature == null ? null : sinCityFeature.getParameters());
    }
}
