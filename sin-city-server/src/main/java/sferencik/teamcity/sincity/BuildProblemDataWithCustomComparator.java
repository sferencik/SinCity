package sferencik.teamcity.sincity;

import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.messages.ErrorData;

/**
 * TeamCity never considers two "artifact dependency failed" problems as identical. If two successive builds both fail
 * due to the same missing artifact dependency, TeamCity makes it appear that the second artifact dependency failure is
 * a new problem. See https://youtrack.jetbrains.com/issue/TW-43503.
 *
 * This class makes up for this by making *all* the artifact dependency problems equal. Maybe that's not 100% correct;
 * maybe one build can fail with multiple different artifact dependency problems (e.g. from multiple dependencies) and
 * it would be good to distinguish between them. However, I prefer JetBrains to fix this, hence the YouTrack.
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BuildProblemDataWithCustomComparator that = (BuildProblemDataWithCustomComparator) o;

        if (getBuildProblemData().getType().equals(ErrorData.ARTIFACT_DEPENDENCY_ERROR_TYPE))
            return getBuildProblemData().getType().equals(that.getBuildProblemData().getType());
        else
            return getBuildProblemData().equals(that.getBuildProblemData());
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            // lazy init of hashCode for the immutable object; see
            // http://www.javapractices.com/topic/TopicAction.do?Id=34
            if (getBuildProblemData().getType().equals(ErrorData.ARTIFACT_DEPENDENCY_ERROR_TYPE))
                hashCode = ErrorData.ARTIFACT_DEPENDENCY_ERROR_TYPE.hashCode();
            else
                hashCode = getBuildProblemData().hashCode();
        }
        return hashCode;
    }
}
