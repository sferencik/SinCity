package sferencik.teamcity.sincity;

import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.messages.ErrorData;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class BuildProblemDataWithCustomComparatorTest {

    private BuildProblemData snapshotDependencyFailure1;
    private BuildProblemData snapshotDependencyFailure2;
    private BuildProblemData artifactDependencyFailure1;
    private BuildProblemData artifactDependencyFailure2;

    @BeforeMethod
    public void SetUp() {
        snapshotDependencyFailure1 = BuildProblemData.createBuildProblem("bt251", ErrorData.SNAPSHOT_DEPENDENCY_ERROR_BUILD_PROCEEDS_TYPE, "Snapshot dependency \"... Dependency\" failed");
        snapshotDependencyFailure2 = BuildProblemData.createBuildProblem("bt251", ErrorData.SNAPSHOT_DEPENDENCY_ERROR_BUILD_PROCEEDS_TYPE, "Snapshot dependency \"... Dependency\" failed");
        // below, I create a new String(ErrorData.ARTIFACT_DEPENDENCY_ERROR_TYPE) on purpose; this is a regression test
        // against an earlier error where I used == to compare this BuildProblemData type against
        // ErrorData.ARTIFACT_DEPENDENCY_ERROR_TYPE
        artifactDependencyFailure1 = BuildProblemData.createBuildProblem("909103612", new String(ErrorData.ARTIFACT_DEPENDENCY_ERROR_TYPE), "Failed to resolve artifacts from <Sam :: SinCity Build, build #61 [id 21768]>");
        artifactDependencyFailure2 = BuildProblemData.createBuildProblem("-639166500", ErrorData.ARTIFACT_DEPENDENCY_ERROR_TYPE, "Failed to resolve artifacts from <Sam :: SinCity Build, build #58 [id 21760]>");
    }

    @Test
    public void testTwoSnapshotDependencyErrorsAreEqual() {
        assertThat(new BuildProblemDataWithCustomComparator(snapshotDependencyFailure1), is(new BuildProblemDataWithCustomComparator(snapshotDependencyFailure2)));
    }

    @Test
    public void testTwoArtifactDependencyErrorsAreEqual() {
        assertThat(new BuildProblemDataWithCustomComparator(artifactDependencyFailure1), is(new BuildProblemDataWithCustomComparator(artifactDependencyFailure2)));
    }

    @Test
    public void testSnapshotDependencyErrorAndArtifactDependencyErrorAreNotEqual() {
        assertThat(new BuildProblemDataWithCustomComparator(snapshotDependencyFailure1), not(new BuildProblemDataWithCustomComparator(artifactDependencyFailure1)));
    }


}
