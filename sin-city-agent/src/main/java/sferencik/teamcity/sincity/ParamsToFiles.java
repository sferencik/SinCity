package sferencik.teamcity.sincity;

import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.log.Loggers;

import java.io.*;
import java.util.Map;

public class ParamsToFiles {
    AgentRunningBuild build;

    public ParamsToFiles(AgentRunningBuild build) {
        this.build = build;
    }

    private void writeStringToFile(String string, String filePath) {
        if (string == null)
            return;

        Loggers.AGENT.info("[SinCity] writing to " + filePath);

        try {
            final BufferedWriter bufferedWriter = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(filePath), "UTF-8"));
            bufferedWriter.write(string);
            bufferedWriter.close();
        } catch (IOException e) {
            Loggers.AGENT.error("[SinCity] writing failed, " + e);
        }

    }

    void storeIfSet() {
        final ParameterNames parameterNames = new ParameterNames();
        final Map<String, String> configParameters = build.getSharedConfigParameters();

        if (!configParameters.containsKey(parameterNames.getSincityBuildProblems())
                && !configParameters.containsKey(parameterNames.getSincityTestFailures())) {
            Loggers.AGENT.debug("[SinCity] the JSON parameters are not set");
            return;
        }

        String buildTempDirectory = build.getBuildTempDirectory().getAbsolutePath();
        final FileNames fileNames = new FileNames();

        if (configParameters.containsKey(parameterNames.getSincityBuildProblems())) {
            Loggers.AGENT.debug("[SinCity] storing " + parameterNames.getSincityBuildProblems());
            writeStringToFile(
                    configParameters.get(parameterNames.getSincityBuildProblems()),
                    String.valueOf(new File(new File(buildTempDirectory), fileNames.getProblemDataJsonFilename())));
        }

        if (configParameters.containsKey(parameterNames.getSincityTestFailures())) {
            Loggers.AGENT.debug("[SinCity] storing " + parameterNames.getSincityTestFailures());
            writeStringToFile(
                    configParameters.get(parameterNames.getSincityTestFailures()),
                    String.valueOf(new File(new File(buildTempDirectory), fileNames.getTestFailureJsonFilename())));
        }
    }
}
