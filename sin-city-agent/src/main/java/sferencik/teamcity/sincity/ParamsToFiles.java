package sferencik.teamcity.sincity;

import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.StringUtil;

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

    private void writeParameterToFileIfSet(String parameterName, String filePath) {
        final Map<String, String> configParameters = build.getSharedConfigParameters();
        if (!configParameters.containsKey(parameterName)) {
            Loggers.AGENT.debug("[SinCity] the " + parameterName + " parameter is not set");
            return;
        }

        String parameterValue = configParameters.get(parameterName);

        if (StringUtil.isEmpty(parameterValue)) {
            Loggers.AGENT.debug("[SinCity] the " + parameterName + " is empty");
            return;
        }

        writeStringToFile(parameterValue, filePath);
    }

    void storeIfSet() {
        final ParameterNames parameterNames = new ParameterNames();
        String buildTempDirectory = build.getBuildTempDirectory().getAbsolutePath();

        File buildProblemJsonFile = new File(new File(buildTempDirectory), getSincityBuildProblemsJsonFilename());
        File testFailureJsonFile = new File(new File(buildTempDirectory), getSincityTestFailuresJsonFilename());

        Loggers.AGENT.debug("[SinCity] " + buildProblemJsonFile + " " + (buildProblemJsonFile.exists() ? "exists" : "does not exist"));
        Loggers.AGENT.debug("[SinCity] " + testFailureJsonFile + " " + (testFailureJsonFile.exists() ? "exists" : "does not exist"));

        writeParameterToFileIfSet(parameterNames.getSincityBuildProblems(),
                buildProblemJsonFile.toString());
        writeParameterToFileIfSet(parameterNames.getSincityTestFailures(),
                testFailureJsonFile.toString());
    }

    private static String getSincityTestFailuresJsonFilename() {
        return new ParameterNames().getSincityTestFailures();
    }

    private static String getSincityBuildProblemsJsonFilename() {
        return new ParameterNames().getSincityBuildProblems();
    }
}
