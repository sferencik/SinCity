package sferencik.teamcity.sincity.json;

import jetbrains.buildServer.BuildProblemData;

public class BuildProblem {
    private String identity;
    private String type;
    private String description;
    private String additionalData;

    public BuildProblem(BuildProblemData tcBuildProblem) {
        identity = Util.nullifyEmptyString(tcBuildProblem.getIdentity());
        type = Util.nullifyEmptyString(tcBuildProblem.getType());
        description = Util.nullifyEmptyString(tcBuildProblem.getDescription());
        additionalData = Util.nullifyEmptyString(tcBuildProblem.getAdditionalData());
    }
}
