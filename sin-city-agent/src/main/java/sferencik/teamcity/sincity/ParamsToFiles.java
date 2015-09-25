package sferencik.teamcity.sincity;

import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.log.Loggers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class ParamsToFiles {
    AgentRunningBuild build;

    public ParamsToFiles(AgentRunningBuild build) {
        this.build = build;
    }

    boolean areSinCityParametersSet() {
        final ParameterNames parameterNames = new ParameterNames();
        final Map<String, String> configParameters = build.getSharedConfigParameters();
        return configParameters.containsKey(parameterNames.getSincityBuildProblems())
                || configParameters.containsKey(parameterNames.getSincityTestFailures());
    }

    void store() {
        final ParameterNames parameterNames = new ParameterNames();
        final FileNames fileNames = new FileNames();

        final Map<String, String> configParameters = build.getSharedConfigParameters();
        String buildTempDirectory = build.getBuildTempDirectory().getAbsolutePath();

        writeStringToFile(configParameters.get(parameterNames.getSincityBuildProblems()), Paths.get(buildTempDirectory, fileNames.getProblemDataJsonFilename()));
        writeStringToFile(configParameters.get(parameterNames.getSincityTestFailures()), Paths.get(buildTempDirectory, fileNames.getTestFailureJsonFilename()));

    }

    private void writeStringToFile(String string, Path filePath) {
        if (string == null)
            return;

        Loggers.AGENT.info("[SinCity] writing to " + filePath);

        try {
            final BufferedWriter bufferedWriter = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8);
            bufferedWriter.write(string);
            bufferedWriter.close();
        } catch (IOException e) {
            Loggers.AGENT.error("[SinCity] writing failed, " + e);
        }

    }
}
