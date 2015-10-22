package sferencik.teamcity.sincity.manualTrigger;

import jetbrains.buildServer.controllers.BaseActionController;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.BuildCustomizerFactory;
import jetbrains.buildServer.serverSide.BuildsManager;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.web.openapi.ControllerAction;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import sferencik.teamcity.sincity.CulpritFinder;
import sferencik.teamcity.sincity.SettingNames;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.regex.Pattern;

public class ManualTriggerController extends BaseActionController {

    public ManualTriggerController(WebControllerManager controllerManager, final BuildCustomizerFactory buildCustomizerFactory, final BuildsManager buildsManager) {
        super(controllerManager);

        final SettingNames settingNames = new SettingNames();

        controllerManager.registerController(new FormTarget().getUrl(), this);

        registerAction(new ControllerAction() {
            public boolean canProcess(HttpServletRequest httpServletRequest) {
                return httpServletRequest.getParameter(settingNames.getRbRangeTop()) != null;
            }

            public void process(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, @Nullable Element element) {
                Loggers.SERVER.info("[SinCity] triggered manually");

                // find the new build
                Long newBuildId = Long.valueOf(httpServletRequest.getParameter(settingNames.getRbRangeTop()));
                SFinishedBuild newBuild = (SFinishedBuild) buildsManager.findBuildInstanceById(newBuildId);
                if (newBuild == null) {
                    Loggers.SERVER.error("[SinCity] new build with ID=" + newBuildId + " not found");
                    return;
                }
                Loggers.SERVER.debug("[SinCity] found new build: " + newBuild);

                // find the old build (if any)
                SFinishedBuild oldBuild = null;
                String oldBuildIdString = httpServletRequest.getParameter(settingNames.getRbRangeBottom());
                if (StringUtil.isEmpty(oldBuildIdString)) {
                    // no old Build selected, most likely because the history only has a single build
                    Loggers.SERVER.debug("[SinCity] no old ID selected");
                }
                else {
                    Long oldBuildId = Long.valueOf(httpServletRequest.getParameter(settingNames.getRbRangeBottom()));
                    oldBuild = (SFinishedBuild) buildsManager.findBuildInstanceById(oldBuildId);
                    if (oldBuild == null) {
                        Loggers.SERVER.error("[SinCity] old build with ID=" + oldBuildId + " not found");
                        return;
                    }

                    Loggers.SERVER.debug("[SinCity] found old build: " + oldBuild);
                }

                String triggerOnNew = settingNames.getTriggerOnNew();

                String rbTriggerOnBuildProblems = httpServletRequest.getParameter(settingNames.getRbTriggerOnBuildProblem());
                String rbTriggerOnTestFailures = httpServletRequest.getParameter(settingNames.getRbTriggerOnTestFailure());
                String cbSetBuildProblemJsonParameterString = httpServletRequest.getParameter(settingNames.getCbSetBuildProblemJsonParameter());
                String cbSetTestFailureJsonParameterString = httpServletRequest.getParameter(settingNames.getCbSetTestFailureJsonParameter());

                new CulpritFinder(newBuild,
                        oldBuild,
                        rbTriggerOnBuildProblems == null ? triggerOnNew : rbTriggerOnBuildProblems,
                        rbTriggerOnTestFailures == null ? triggerOnNew : rbTriggerOnTestFailures,
                        cbSetBuildProblemJsonParameterString != null && Boolean.valueOf(cbSetBuildProblemJsonParameterString),
                        cbSetTestFailureJsonParameterString != null && Boolean.valueOf(cbSetTestFailureJsonParameterString),
                        buildCustomizerFactory)
                    .triggerCulpritFindingIfNeeded();
            }
        });
    }

    @Nullable
    @Override
    protected ModelAndView doHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
        doAction(httpServletRequest, httpServletResponse, null);

        String redirectTo = httpServletRequest.getParameter("redirectTo");
        if (StringUtil.isEmpty(redirectTo)) {
            redirectTo = httpServletRequest.getHeader("Referer");
            if (redirectTo == null)
                return null;

            redirectTo = redirectTo.replaceAll("(?<=[?&])tab=" + Pattern.quote(ManualTriggerTab.getTabCode()) + "\\b", "tab=buildTypeStatusDiv");
        }
        return new ModelAndView(new RedirectView(redirectTo));
    }
}
