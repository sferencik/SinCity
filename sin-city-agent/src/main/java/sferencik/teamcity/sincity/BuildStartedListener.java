package sferencik.teamcity.sincity;

import jetbrains.buildServer.agent.AgentLifeCycleAdapter;
import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.EventDispatcher;

public class BuildStartedListener {
    public BuildStartedListener(EventDispatcher<AgentLifeCycleListener> listener)
    {
        listener.addListener(new AgentLifeCycleAdapter()
        {
            AgentRunningBuild build = null;
            /**
             * When a build is starting, check if it has the SinCity JSON-string parameters defined. If so, copy these
             * to files in the agent's working directory.
             *
             * We do this in the sourcesUpdated() event rather than buildStarted(). This is because the build
             * lifecycle is as follows:
             * 1) buildStarted() - at this point, the build temporary directory still has the contents from the
             *    previous run
             * 2) the build temporary directory is emptied for this run
             * 3) sourcesUpdated()
             *
             * In other words, if we created the JSON files in buildStarted(), the current build would never see them.
             * @param build the starting build
             */
            @Override
            public void sourcesUpdated(AgentRunningBuild build) {

                this.build = build;

                final ParamsToFiles paramsToFiles = new ParamsToFiles(build);
                if (paramsToFiles.areSinCityParametersSet()) {
                    Loggers.AGENT.debug("[SinCity] the JSON parameters are set");
                    paramsToFiles.store();
                }
                else
                    Loggers.AGENT.debug("[SinCity] the JSON parameters are not set");
            }
        });
    }
}
