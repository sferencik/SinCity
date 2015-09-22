package sferencik.teamcity.sincity;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SettingsPageController extends BaseController
{
    @NotNull
    private final PluginDescriptor descriptor;

    public SettingsPageController(@NotNull final PluginDescriptor descriptor,
                                  @NotNull final WebControllerManager web)
    {
        this.descriptor = descriptor;
    }

    @Nullable
    @Override
    protected ModelAndView doHandle(@NotNull final HttpServletRequest request,
                                    @NotNull final HttpServletResponse response) throws Exception
    {
        return  new ModelAndView(descriptor.getPluginResourcesPath("feature.jsp"));
    }
}
