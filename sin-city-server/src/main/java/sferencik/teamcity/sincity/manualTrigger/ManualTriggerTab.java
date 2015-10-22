package sferencik.teamcity.sincity.manualTrigger;

import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.openapi.buildType.BuildTypeTab;
import org.jetbrains.annotations.NotNull;
import sferencik.teamcity.sincity.SinCityUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

public class ManualTriggerTab extends BuildTypeTab {

    public ManualTriggerTab(@NotNull WebControllerManager manager,
                            @NotNull ProjectManager projectManager) {
        super(getTabCode(), "Trigger culprit finding", manager, projectManager);
    }

    @NotNull
    static String getTabCode() {
        return "sin-city";
    }

    @Override
    public boolean isAvailable(@NotNull HttpServletRequest request) {
        SBuildType buildType = getBuildType(request);
        return buildType != null
                && !FinishedBuildWithChange.getListFromBuildType(buildType).isEmpty()
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
        List<FinishedBuildWithChange> buildsWithChanges = FinishedBuildWithChange.getListFromBuildType(sBuildType);
        map.put("buildsWithChanges", buildsWithChanges);

        SBuildFeatureDescriptor sinCityFeature = SinCityUtils.getSinCityFeature(sBuildType);
        map.put("sinCityParameters", sinCityFeature == null ? null : sinCityFeature.getParameters());
    }
}
