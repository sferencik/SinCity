package sferencik.teamcity.sincity;

import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.messages.ErrorData;
import jetbrains.buildServer.version.ServerVersionHolder;

/**
 * Before v10, TeamCity never considered two "artifact dependency failed" problems as identical. If two successive
 * builds both failed due to the same missing artifact dependency, TeamCity would make it appear that the second
 * artifact dependency failure was a new problem. See https://youtrack.jetbrains.com/issue/TW-43503.
 *
 * This class makes up for this by making *all* the artifact dependency problems equal for TC versions under 10.
 * Obviously, that's not 100% correct; one build can fail with multiple different artifact dependency problems (e.g.
 * from multiple dependencies) and it would be good to distinguish between them. However, I think it's acceptable as a
 * quick workaround until JetBrains fixed this in TC 10.
 */
public class BuildProblemDataWithCustomComparator {
    private BuildProblemData buildProblemData;
    private int hashCode = 0;

    public BuildProblemDataWithCustomComparator(BuildProblemData buildProblemData) {
        this.buildProblemData = buildProblemData;
    }

    public BuildProblemData getBuildProblemData() {
        return buildProblemData;
    }

    private static boolean useNativeBuildProblemDataComparator() {
        // we can rely on the (fixed) native BuildProblemData comparator starting with Indore 10.0 EAP 1 (40941); see
        // the youtrack issue
        return Integer.parseInt(ServerVersionHolder.getVersion().getBuildNumber()) >= 40941;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BuildProblemDataWithCustomComparator that = (BuildProblemDataWithCustomComparator) o;

        if (getBuildProblemData().getType().equals(ErrorData.ARTIFACT_DEPENDENCY_ERROR_TYPE) && !useNativeBuildProblemDataComparator())
            return getBuildProblemData().getType().equals(that.getBuildProblemData().getType());
        else
            return getBuildProblemData().equals(that.getBuildProblemData());
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            // lazy init of hashCode for the immutable object; see
            // http://www.javapractices.com/topic/TopicAction.do?Id=34
            if (getBuildProblemData().getType().equals(ErrorData.ARTIFACT_DEPENDENCY_ERROR_TYPE) && !useNativeBuildProblemDataComparator())
                hashCode = ErrorData.ARTIFACT_DEPENDENCY_ERROR_TYPE.hashCode();
            else
                hashCode = getBuildProblemData().hashCode();
        }
        return hashCode;
    }
}
