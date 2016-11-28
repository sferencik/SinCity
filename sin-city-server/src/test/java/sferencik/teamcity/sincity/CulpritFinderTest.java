package sferencik.teamcity.sincity;

import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.BuildProblemTypes;
import jetbrains.buildServer.messages.ErrorData;
import jetbrains.buildServer.serverSide.BuildCustomizerFactory;
import jetbrains.buildServer.serverSide.BuildQueue;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class CulpritFinderTest {
    private Mockery mockery;
    private SFinishedBuild thisBuild;
    private SFinishedBuild previousBuild;
    private BuildCustomizerFactory buildCustomizerFactory;
    private BuildQueue buildQueue;

    @BeforeMethod
    public void SetUp() {
        mockery = new Mockery();

        thisBuild = mockery.mock(SFinishedBuild.class, "thisBuild");
        previousBuild = mockery.mock(SFinishedBuild.class, "previousBuild");
        buildCustomizerFactory = mockery.mock(BuildCustomizerFactory.class);
        buildQueue = mockery.mock(BuildQueue.class);
    }

    @Test
    public void testGetRelevantBuildProblems() {
        /*
            Test that an artifact-dependency-error is removed from the set of build problems.

            thisBuild has three problems:
            1) an artifact dependency problem
            2) an exit code 1
            3) a snapshot dependency problem

            previousBuild has three problems:
            1) an artifact dependency problem (different than above, as TeamCity puts instance-specific info into these)
            2) an exit code 1 (same as above)
            3) a time-out

            The difference between these two tests is the snapshot dependency problem (only).
         */

        //arrange
        mockery.checking(new Expectations() {{
            oneOf(thisBuild).getFailureReasons(); will(returnValue(Arrays.asList(
                    BuildProblemData.createBuildProblem("909103612", ErrorData.ARTIFACT_DEPENDENCY_ERROR_TYPE, "Failed to resolve artifacts from <Sam :: SinCity Build, build #61 [id 21768]>"),
                    BuildProblemData.createBuildProblem("simpleRunner1", BuildProblemTypes.TC_EXIT_CODE_TYPE, "Process exited with code 1"),
                    BuildProblemData.createBuildProblem("bt251", ErrorData.SNAPSHOT_DEPENDENCY_ERROR_BUILD_PROCEEDS_TYPE, "Snapshot dependency \"... Dependency\" failed")
            )));
            oneOf(previousBuild).getFailureReasons(); will(returnValue(Arrays.asList(
                    BuildProblemData.createBuildProblem("simpleRunner1", BuildProblemTypes.TC_EXIT_CODE_TYPE, "Process exited with code 1"),
                    BuildProblemData.createBuildProblem("-639166500", ErrorData.ARTIFACT_DEPENDENCY_ERROR_TYPE, "Failed to resolve artifacts from <Sam :: SinCity Build, build #58 [id 21760]>"),
                    BuildProblemData.createBuildProblem("TC_EXECUTION_TIMEOUT", BuildProblemTypes.TC_EXECUTION_TIMEOUT_TYPE, "Execution timeout")
            )));
        }});

        SettingNames settingNames = new SettingNames();
        CulpritFinder culpritFinder = new CulpritFinder(thisBuild, previousBuild, settingNames.getTriggerOnNew(), settingNames.getNoTrigger(), buildCustomizerFactory, buildQueue, false, "");

        // act
        List<BuildProblemData> relevantBuildProblems = culpritFinder.getRelevantBuildProblems();

        // assert
        assertThat(relevantBuildProblems.size(), is(1));
        assertThat(relevantBuildProblems.get(0).getIdentity(), is("bt251"));
        assertThat(relevantBuildProblems.get(0).getType(), is(ErrorData.SNAPSHOT_DEPENDENCY_ERROR_BUILD_PROCEEDS_TYPE));
        assertThat(relevantBuildProblems.get(0).getDescription(), is("Snapshot dependency \"... Dependency\" failed"));
    }
}
