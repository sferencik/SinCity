package sferencik.teamcity.sincity.failureCulpritOverview;

import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.WebLinks;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.openapi.buildType.BuildTypeTab;
import org.jetbrains.annotations.NotNull;
import sferencik.teamcity.sincity.FinishedBuildWithChanges;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class FailureCulpritOverviewTab extends BuildTypeTab {

    private final WebLinks webLinks;

    public FailureCulpritOverviewTab(@NotNull WebControllerManager manager,
                                     @NotNull ProjectManager projectManager,
                                     @NotNull PluginDescriptor pluginDescriptor,
                                     @NotNull WebLinks webLinks) {
        super(getTabCode(), "Failure culprit overview", manager, projectManager);
        this.webLinks = webLinks;

        // specify path relative to the plugin; avoid TC assuming the JSP is under /plugins/[TAB CODE], which is what it
        // would do if we used this relative path in build-server-plugin-sin-city.xml's
        // bean/property[@name='includeUrl']; this is needed since getTabCode() returns a value different than the
        // plugin name; see https://devnet.jetbrains.com/message/5564684
        setIncludeUrl(pluginDescriptor.getPluginResourcesPath("failureCulpritOverview.jsp"));
    }

    @NotNull
    static String getTabCode() {
        return "sin-city-overview";
    }

    @Override
    public boolean isAvailable(@NotNull HttpServletRequest request) {
        SBuildType buildType = getBuildType(request);
        return buildType != null && super.isAvailable(request);
    }

    @Override
    protected void fillModel(@NotNull Map<String, Object> map,
                             @NotNull HttpServletRequest httpServletRequest,
                             @NotNull SBuildType sBuildType,
                             @NotNull SUser sUser) {

        Table culpritTable = Analyser.createCulpritMatrix(FinishedBuildWithChanges.getListFromBuildType(sBuildType));
        map.put("matrix", culpritTable);
        map.put("webLinks", webLinks);
    }
}
