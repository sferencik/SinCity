package sferencik.teamcity.sincity;

import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;

/**
 * When a build is starting, check if it has the SinCity JSON-string parameters defined. If so, copy these to files in
 * the agent's working directory.
 *
 * A "build starting" is best expressed by the buildStarted() event. However, writing the files at buildStarted() is
 * too early because that kicks in before the
 * jetbrains.buildServer.agent.impl.buildStages.startStages.CleanBuildTempDirectoryStage stage. Hence, anything we put
 * to the build temporary directory at buildStarted() gets cleaned up a split second later.
 *
 * Instead, we write the JSON files on sourcesUpdated() and/or buildRunnerStart(). This gets the job done although
 * this has some disadvantages:
 * - sourcesUpdated() never kicks in if the build is set not to check out code
 * - beforeRunnerStart() only kicks in if there are build steps, and then kicks in once for each build step
 *
 * Writing the JSON files at both events should cover most cases: it's hard to imagine a build configuration that
 * would neither check out sources, nor have build steps, and yet would do culprit finding and rely on the JSON files
 * while doing that.
 */
public class BuildStartedListener {
    public BuildStartedListener(EventDispatcher<AgentLifeCycleListener> listener)
    {
        listener.addListener(new AgentLifeCycleAdapter()
        {
            /**
             * Store the JSON-string parameters (if defined) to the corresponding JSON files.
             *
             * @param build the build whose sources are being updated
             */
            @Override
            public void sourcesUpdated(@NotNull AgentRunningBuild build) {
                Loggers.AGENT.debug("[SinCity] sourcesUpdated(" + build + ") triggered");
                super.sourcesUpdated(build);
                new ParamsToFiles(build).storeIfSet();
            }

            /**
             * Store the JSON-string parameters (if defined) to the corresponding JSON files.
             *
             * @param runner the runner that is about to start
             */
            @Override
            public void beforeRunnerStart(@NotNull BuildRunnerContext runner) {
                Loggers.AGENT.debug("[SinCity] beforeRunnerStart(" + runner.getBuild() + ", " + runner.getName() + ") triggered");
                super.beforeRunnerStart(runner);
                new ParamsToFiles(runner.getBuild()).storeIfSet();
            }
        });
    }
}
