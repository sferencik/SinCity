package sferencik.teamcity.sincity;

public class FileNames {
    ParameterNames parameterNames = new ParameterNames();

    public String getBuildProblemJsonFilename() {
        return parameterNames.getSincityBuildProblems();
    }

    public String getTestFailureJsonFilename() {
        return parameterNames.getSincityTestFailures();
    }

}
