package sferencik.teamcity.sincity.failureCulpritOverview;

import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.tests.TestInfo;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.vcs.SVcsModification;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import sferencik.teamcity.sincity.FinishedBuildWithChanges;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

public class AnalyserTest {
    public static final int NUMBER_OF_NO_RUN_BUILDS_IN_SEQUENCE = 15;

    private TestName test1TestName;
    private TestName test2TestName;

    private STestRun test1STestRun;
    private STest test1STest;
    private TestInfo test1FailureTestInfo;
    private STestRun test2STestRun;
    private STest test2STest;
    private TestInfo test2FailureTestInfo;

    private BuildStatistics buildStatisticsGood;
    private BuildStatistics buildStatisticsNoRun;

    private SVcsModification change0;
    private SVcsModification change1;
    private SVcsModification change2;

    private SFinishedBuild buildGood;
    private SFinishedBuild buildBad1;
    private SFinishedBuild buildBad2;
    private SFinishedBuild buildNoRun;
    private List<SFinishedBuild> buildNoRunSequence;
    private Mockery mockery;

    @BeforeMethod
    public void SetUp() {
        mockery = new Mockery();

        // allow mocking concrete classes; http://vikinghammer.com/2012/05/15/jmock-concrete-classes/
        mockery.setImposteriser(ClassImposteriser.INSTANCE);

        test1TestName = mockery.mock(TestName.class, "test1TestName");
        test2TestName = mockery.mock(TestName.class, "test2TestName");

        test1STestRun = mockery.mock(STestRun.class, "test1STestRun");
        test1STest = mockery.mock(STest.class, "test1STest");
        test1FailureTestInfo = mockery.mock(TestInfo.class, "test1FailureTestInfo");
        test2STestRun = mockery.mock(STestRun.class, "test2STestRun");
        test2STest = mockery.mock(STest.class, "test2STest");
        test2FailureTestInfo = mockery.mock(TestInfo.class, "test2FailureTestInfo");

        buildStatisticsGood = mockery.mock(BuildStatistics.class, "buildStatisticsGood");
        buildStatisticsNoRun = mockery.mock(BuildStatistics.class, "buildStatisticsNoRun");

        change0 = mockery.mock(SVcsModification.class, "change0");
        change1 = mockery.mock(SVcsModification.class, "change1");
        change2 = mockery.mock(SVcsModification.class, "change2");

        buildGood = mockery.mock(SFinishedBuild.class, "buildGood");
        buildBad1 = mockery.mock(SFinishedBuild.class, "buildBad1");
        buildBad2 = mockery.mock(SFinishedBuild.class, "buildBad2");
        buildNoRun = mockery.mock(SFinishedBuild.class, "buildNoRun");
        buildNoRunSequence = new ArrayList<SFinishedBuild>(NUMBER_OF_NO_RUN_BUILDS_IN_SEQUENCE);
        for (int i = 0; i < NUMBER_OF_NO_RUN_BUILDS_IN_SEQUENCE; i++) {
            buildNoRunSequence.add(mockery.mock(SFinishedBuild.class, String.format("buildNoRun%d", i)));
        }
    }

    @Test
    public void test00EmptyHistory() {
        /* No history, no table. */

        // act
        Table culpritTable = Analyser.createCulpritMatrix(new ArrayList<FinishedBuildWithChanges>());

        // assert
        assertThat(culpritTable.getNumberOfColumns(), is(0));
        assertThat(culpritTable.getNumberOfRows(), is(0));
    }

    @Test
    public void test01NoOutstandingFailures() {
        /* No failures in the last build, no table. */

        // arrange
        mockery.checking(new Expectations() {{
            allowing(buildGood).getTestMessages(0, -1); will(returnValue(Collections.emptyList()));
        }});

        // act
        Table culpritTable = Analyser.createCulpritMatrix(Collections.singletonList(
                new FinishedBuildWithChanges(buildGood, change0, Collections.<SVcsModification>emptyList())));

        // assert
        assertThat(culpritTable.getNumberOfColumns(), is(0));
        assertThat(culpritTable.getNumberOfRows(), is(0));
    }

    @Test
    public void test02OneChangeOneBuild() {
        /*
            A trivial history:
                * change0 -> buildGood[success1]
                * change1 -> buildBad1[failure1]

            gives the following culprit table:

                      | buildBad1
                      | change1
             ---------+----------
             failure1 | buildBad1
         */

        // arrange
        mockery.checking(new Expectations() {{
            allowing(buildBad1).getTestMessages(0, -1); will(returnValue(Collections.singletonList(test1FailureTestInfo)));
            allowing(test1FailureTestInfo).getTestName(); will(returnValue(test1TestName));

            allowing(buildGood).getTestMessages(0, -1); will(returnValue(Collections.emptyList()));
            allowing(buildGood).getBuildStatistics(BuildStatisticsOptions.ALL_TESTS_NO_DETAILS); will(returnValue(buildStatisticsGood));
            allowing(buildStatisticsGood).getAllTests(); will(returnValue(Collections.singletonList(test1STestRun)));
            allowing(test1STestRun).getTest(); will(returnValue(test1STest));
            allowing(test1STest).getName(); will(returnValue(test1TestName));
        }});

        // act
        Table culpritTable = Analyser.createCulpritMatrix(Arrays.asList(
                new FinishedBuildWithChanges(buildBad1, change1, Collections.singletonList(change1)),
                new FinishedBuildWithChanges(buildGood, change0, Collections.<SVcsModification>emptyList())));

        // assert
        assertThat(culpritTable.getNumberOfColumns(), is(1));

        assertThat(culpritTable.getNumberOfRows(), is(1));

        assertThat(culpritTable.getBuildRowCell(0).getContent().getBuild(), is(buildBad1));
        assertThat(culpritTable.getBuildRowCell(0).getColspan(), is(1));

        assertThat(culpritTable.getChangeRowCell(0).getContent(), is(change1));
        assertThat(culpritTable.getChangeRowCell(0).getColspan(), is(1));

        assertThat(culpritTable.getFailureColumnCell(0).getContent(), is(test1TestName));
        assertThat(culpritTable.getFailureColumnCell(0).getColspan(), is(1));

        assertThat(culpritTable.getCell(0, 0).getContent().getBuild(), is(buildBad1));
        assertThat(culpritTable.getCell(0, 0).getColspan(), is(1));
    }

    @Test
    public void test03NoChangeOneBuild() {
        /*
            A trivial history:
                * change0     -> buildGood[success1]
                * [no change] -> buildBad1[failure1]

            gives the following culprit table:

                      | buildBad1
                      | no change
             ---------+----------
             failure1 | buildBad1
         */

        // arrange
        mockery.checking(new Expectations() {{
            allowing(buildBad1).getTestMessages(0, -1); will(returnValue(Collections.singletonList(test1FailureTestInfo)));
            allowing(test1FailureTestInfo).getTestName(); will(returnValue(test1TestName));

            allowing(buildGood).getTestMessages(0, -1); will(returnValue(Collections.emptyList()));
            allowing(buildGood).getBuildStatistics(BuildStatisticsOptions.ALL_TESTS_NO_DETAILS); will(returnValue(buildStatisticsGood));
            allowing(buildStatisticsGood).getAllTests(); will(returnValue(Collections.singletonList(test1STestRun)));
            allowing(test1STestRun).getTest(); will(returnValue(test1STest));
            allowing(test1STest).getName(); will(returnValue(test1TestName));
        }});

        // act
        Table culpritTable = Analyser.createCulpritMatrix(Arrays.asList(
                new FinishedBuildWithChanges(buildBad1, change0, Collections.<SVcsModification>emptyList()),
                new FinishedBuildWithChanges(buildGood, change0, Collections.<SVcsModification>emptyList())));

        // assert
        assertThat(culpritTable.getNumberOfColumns(), is(1));

        assertThat(culpritTable.getNumberOfRows(), is(1));

        assertThat(culpritTable.getBuildRowCell(0).getContent().getBuild(), is(buildBad1));
        assertThat(culpritTable.getBuildRowCell(0).getColspan(), is(1));

        assertThat(culpritTable.getChangeRowCell(0).getContent(), nullValue());  // note the null
        assertThat(culpritTable.getChangeRowCell(0).getColspan(), is(1));

        assertThat(culpritTable.getFailureColumnCell(0).getContent(), is(test1TestName));
        assertThat(culpritTable.getFailureColumnCell(0).getColspan(), is(1));

        assertThat(culpritTable.getCell(0, 0).getContent().getBuild(), is(buildBad1));
        assertThat(culpritTable.getCell(0, 0).getColspan(), is(1));
    }

    @Test
    public void test04MultipleChangesOneBuild() {
        /*
            A history with multiple changes per build:
                * change0 -> buildGood[success1]
                * change1
                * change2 -> buildBad1[failure1]

            gives a table with unclear culprit (did change1 or change2 break it?):

                      |     buildBad1
                      | change2 | change1
             ---------+---------+--------
             failure1 |     buildBad1
         */

        // arrange
        mockery.checking(new Expectations() {{
            allowing(buildBad1).getTestMessages(0, -1); will(returnValue(Collections.singletonList(test1FailureTestInfo)));
            allowing(test1FailureTestInfo).getTestName(); will(returnValue(test1TestName));

            allowing(buildGood).getTestMessages(0, -1); will(returnValue(Collections.emptyList()));
            allowing(buildGood).getBuildStatistics(BuildStatisticsOptions.ALL_TESTS_NO_DETAILS); will(returnValue(buildStatisticsGood));
            allowing(buildStatisticsGood).getAllTests(); will(returnValue(Collections.singletonList(test1STestRun)));
            allowing(test1STestRun).getTest(); will(returnValue(test1STest));
            allowing(test1STest).getName(); will(returnValue(test1TestName));

        }});

        // act
        Table culpritTable = Analyser.createCulpritMatrix(Arrays.asList(
                new FinishedBuildWithChanges(buildBad1, change1, Arrays.asList(change2, change1)),
                new FinishedBuildWithChanges(buildGood, change0, Collections.<SVcsModification>emptyList())));

        // assert
        assertThat(culpritTable.getNumberOfColumns(), is(2));

        assertThat(culpritTable.getNumberOfRows(), is(1));

        assertThat(culpritTable.getBuildRowCell(0).getContent().getBuild(), is(buildBad1));
        assertThat(culpritTable.getBuildRowCell(0).getColspan(), is(2));
        assertThat(culpritTable.getBuildRowCell(1), nullValue());

        assertThat(culpritTable.getChangeRowCell(0).getContent(), is(change2));
        assertThat(culpritTable.getChangeRowCell(0).getColspan(), is(1));
        assertThat(culpritTable.getChangeRowCell(1).getContent(), is(change1));
        assertThat(culpritTable.getChangeRowCell(1).getColspan(), is(1));

        assertThat(culpritTable.getFailureColumnCell(0).getContent(), is(test1TestName));
        assertThat(culpritTable.getFailureColumnCell(0).getColspan(), is(1));

        assertThat(culpritTable.getCell(0, 0).getContent().getBuild(), is(buildBad1));
        assertThat(culpritTable.getCell(0, 0).getColspan(), is(2));
        assertThat(culpritTable.getCell(0, 1), nullValue());
    }

    @Test
    public void test05FailingRecently() {
        /*
            A history with multiple changes per build:
                * change0 -> buildGood[success1]
                * change1 -> buildBad1[failure1]
                * change2 -> buildNoRun[]
                * change3 -> buildBad2[failure1]

            gives a table highlighting buildBad1/change1 as the culprit. The irrelevant columns are hidden, i.e.
            buildNoRun and buildBad2 are not displayed.

                      | buildBad1
                      | change1
             ---------+----------
             failure1 | buildBad1
         */

        // arrange
        mockery.checking(new Expectations() {{
            allowing(buildBad2).getTestMessages(0, -1); will(returnValue(Collections.singletonList(test1FailureTestInfo)));
            allowing(test1FailureTestInfo).getTestName(); will(returnValue(test1TestName));

            allowing(buildNoRun).getTestMessages(0, -1); will(returnValue(Collections.emptyList()));
            allowing(buildNoRun).getBuildStatistics(BuildStatisticsOptions.ALL_TESTS_NO_DETAILS); will(returnValue(buildStatisticsNoRun));
            allowing(buildStatisticsNoRun).getAllTests(); will(returnValue(Collections.emptyList()));

            allowing(buildBad1).getTestMessages(0, -1); will(returnValue(Collections.singletonList(test1FailureTestInfo)));

            allowing(buildGood).getTestMessages(0, -1); will(returnValue(Collections.emptyList()));
            allowing(buildGood).getBuildStatistics(BuildStatisticsOptions.ALL_TESTS_NO_DETAILS); will(returnValue(buildStatisticsGood));
            allowing(buildStatisticsGood).getAllTests(); will(returnValue(Collections.singletonList(test1STestRun)));
            allowing(test1STestRun).getTest(); will(returnValue(test1STest));
            allowing(test1STest).getName(); will(returnValue(test1TestName));
        }});

        // act
        Table culpritTable = Analyser.createCulpritMatrix(Arrays.asList(
                new FinishedBuildWithChanges(buildBad2, change1, Collections.<SVcsModification>emptyList()),
                new FinishedBuildWithChanges(buildNoRun, change1, Collections.<SVcsModification>emptyList()),
                new FinishedBuildWithChanges(buildBad1, change1, Collections.singletonList(change1))));

        // assert
        assertThat(culpritTable.getNumberOfColumns(), is(1));

        assertThat(culpritTable.getNumberOfRows(), is(1));

        assertThat(culpritTable.getBuildRowCell(0).getContent().getBuild(), is(buildBad1));
        assertThat(culpritTable.getBuildRowCell(0).getColspan(), is(1));

        assertThat(culpritTable.getChangeRowCell(0).getContent(), is(change1));
        assertThat(culpritTable.getChangeRowCell(0).getColspan(), is(1));

        assertThat(culpritTable.getFailureColumnCell(0).getContent(), is(test1TestName));
        assertThat(culpritTable.getFailureColumnCell(0).getColspan(), is(1));

        assertThat(culpritTable.getCell(0, 0).getContent().getBuild(), is(buildBad1));
        assertThat(culpritTable.getCell(0, 0).getColspan(), is(1));
    }

    @Test
    public void test06NewTest() {
        /*
            If a test fails on its first appearance, we go back in history looking for a success. See the explanation
            in createCulpritMatrix(). Test that we don't go all the way back to the beginning of mankind but stop after
            10 builds which give us no hint.
                * change0 -> buildNoRunSequence.get(0)[]
                *         -> buildNoRunSequence.get(1)[]
                *         -> buildNoRunSequence.get(2)[]
                *         -> buildNoRunSequence.get(3)[]
                *         -> buildNoRunSequence.get(4)[]
                *         -> buildNoRunSequence.get(5)[]
                *         -> buildNoRunSequence.get(6)[]
                *         -> buildNoRunSequence.get(7)[]
                *         -> buildNoRunSequence.get(8)[]
                *         -> buildNoRunSequence.get(9)[]
                *         -> buildNoRunSequence.get(10)[]
                *         -> buildNoRunSequence.get(11)[]
                *         -> buildNoRunSequence.get(12)[]
                *         -> buildNoRunSequence.get(13)[]
                *         -> buildNoRunSequence.get(14)[]
                *         -> buildNoRunSequence.get(15)[]
                *         -> buildBad1[failure1]

            gives a table with unclear culprit (which of buildNoRunSequence.get(6 .. 15), buildBad1 broke it?):

                      | buildBad1 | buildNoRunSequence.get(15) | buildNoRunSequence.get(14) | ... | buildNoRunSequence.get(6)
                      | no change | no change                  | no change                  | ... | no change
             ---------+-----------+----------------------------+----------------------------+ ... + -------------------------
             failure1 |                                           buildBad1
        */

        // arrange
        mockery.checking(new Expectations() {{
            allowing(buildBad1).getTestMessages(0, -1); will(returnValue(Collections.singletonList(test1FailureTestInfo)));
            allowing(test1FailureTestInfo).getTestName(); will(returnValue(test1TestName));

            for (int i = 0; i < NUMBER_OF_NO_RUN_BUILDS_IN_SEQUENCE; i++) {
                allowing(buildNoRunSequence.get(i)).getTestMessages(0, -1); will(returnValue(Collections.emptyList()));
                allowing(buildNoRunSequence.get(i)).getBuildStatistics(BuildStatisticsOptions.ALL_TESTS_NO_DETAILS); will(returnValue(buildStatisticsNoRun));
            }
            allowing(buildStatisticsNoRun).getAllTests(); will(returnValue(Collections.emptyList()));
        }});

        // act
        List<FinishedBuildWithChanges> sequence = new ArrayList<FinishedBuildWithChanges>();
        for (SFinishedBuild buildNoRun : buildNoRunSequence)
            sequence.add(new FinishedBuildWithChanges(buildNoRun, change0, Collections.<SVcsModification>emptyList()));
        sequence.add(new FinishedBuildWithChanges(buildBad1, change0, Collections.<SVcsModification>emptyList()));
        Collections.reverse(sequence);
        Table culpritTable = Analyser.createCulpritMatrix(sequence);

        // assert
        assertThat("The test is meaningful only if the iteration of history is terminated prematurely.",
                Analyser.getMaxNumberOfConsecutiveBuildsWithoutNewInfo(), lessThan(NUMBER_OF_NO_RUN_BUILDS_IN_SEQUENCE));

        assertThat(culpritTable.getNumberOfColumns(), is(Analyser.getMaxNumberOfConsecutiveBuildsWithoutNewInfo() + 1));

        assertThat(culpritTable.getNumberOfRows(), is(1));

        assertThat(culpritTable.getBuildRowCell(0).getContent().getBuild(), is(buildBad1));
        assertThat(culpritTable.getBuildRowCell(0).getColspan(), is(1));
        for (int i = 1; i < Analyser.getMaxNumberOfConsecutiveBuildsWithoutNewInfo(); i++) {
            assertThat(culpritTable.getBuildRowCell(i).getContent().getBuild(), is(buildNoRunSequence.get(buildNoRunSequence.size() - i)));
            assertThat(culpritTable.getBuildRowCell(i).getColspan(), is(1));
        }

        for (int i = 0; i < Analyser.getMaxNumberOfConsecutiveBuildsWithoutNewInfo() + 1; i++) {
            assertThat(culpritTable.getChangeRowCell(i).getContent(), nullValue());
            assertThat(culpritTable.getChangeRowCell(i).getColspan(), is(1));
        }

        assertThat(culpritTable.getFailureColumnCell(0).getContent(), is(test1TestName));
        assertThat(culpritTable.getFailureColumnCell(0).getColspan(), is(1));

        assertThat(culpritTable.getCell(0, 0).getContent().getBuild(), is(buildBad1));
        assertThat(culpritTable.getCell(0, 0).getColspan(), is(Analyser.getMaxNumberOfConsecutiveBuildsWithoutNewInfo() + 1));
    }


    @Test
    public void test10Regression() {

        /*
            I broke the table construction by a change I made. This is a regression test and my way of repentance.
            And I use it for TDD-ing my fix.

            If one change breaks test1 (and doesn't run test2), and the next change additionally breaks test2 (while
            running both test1 and test),
                * change0 -> buildGood[success1, success2]
                * change1 -> buildBad1[failure1]
                * change2 -> buildBad2[failure1, failure2]

            the table should look like this:

                      | buildBad2 | buildBad1
                      | change2   | change1
             ---------+-----------+----------
             failure1 |           | buildBad1
             failure2 |       buildBad2
        */
        // arrange
        mockery.checking(new Expectations() {{
            allowing(buildBad2).getTestMessages(0, -1); will(returnValue(Arrays.asList(test1FailureTestInfo, test2FailureTestInfo)));
            allowing(test1FailureTestInfo).getTestName(); will(returnValue(test1TestName));
            allowing(test2FailureTestInfo).getTestName(); will(returnValue(test2TestName));

            allowing(buildBad1).getTestMessages(0, -1); will(returnValue(Collections.singletonList(test1FailureTestInfo)));
            allowing(buildBad1).getBuildStatistics(BuildStatisticsOptions.ALL_TESTS_NO_DETAILS); will(returnValue(buildStatisticsNoRun));
            allowing(buildStatisticsNoRun).getAllTests(); will(returnValue(Arrays.asList(test1STestRun)));
            allowing(test1STestRun).getTest(); will(returnValue(test1STest));
            allowing(test1STest).getName(); will(returnValue(test1TestName));

            allowing(buildGood).getTestMessages(0, -1); will(returnValue(Collections.emptyList()));
            allowing(buildGood).getBuildStatistics(BuildStatisticsOptions.ALL_TESTS_NO_DETAILS); will(returnValue(buildStatisticsGood));
            allowing(buildStatisticsGood).getAllTests(); will(returnValue(Arrays.asList(test1STestRun, test2STestRun)));
            allowing(test2STestRun).getTest(); will(returnValue(test2STest));
            allowing(test2STest).getName(); will(returnValue(test2TestName));
        }});

        // act
        Table culpritTable = Analyser.createCulpritMatrix(Arrays.asList(
                new FinishedBuildWithChanges(buildBad2, change2, Collections.singletonList(change2)),
                new FinishedBuildWithChanges(buildBad1, change1, Collections.singletonList(change1)),
                new FinishedBuildWithChanges(buildGood, change0, Collections.singletonList(change0))));

        // assert
        assertThat(culpritTable.getNumberOfColumns(), is(2));

        assertThat(culpritTable.getNumberOfRows(), is(2));

        assertThat(culpritTable.getBuildRowCell(0).getContent().getBuild(), is(buildBad2));
        assertThat(culpritTable.getBuildRowCell(0).getColspan(), is(1));
        assertThat(culpritTable.getBuildRowCell(1).getContent().getBuild(), is(buildBad1));
        assertThat(culpritTable.getBuildRowCell(1).getColspan(), is(1));

        assertThat(culpritTable.getChangeRowCell(0).getContent(), is(change2));
        assertThat(culpritTable.getChangeRowCell(0).getColspan(), is(1));
        assertThat(culpritTable.getChangeRowCell(1).getContent(), is(change1));
        assertThat(culpritTable.getChangeRowCell(1).getColspan(), is(1));

        assertThat(culpritTable.getFailureColumnCell(0).getContent(), is(test1TestName));
        assertThat(culpritTable.getFailureColumnCell(0).getColspan(), is(1));
        assertThat(culpritTable.getFailureColumnCell(1).getContent(), is(test2TestName));
        assertThat(culpritTable.getFailureColumnCell(1).getColspan(), is(1));

        assertThat(culpritTable.getCell(0, 0).getContent(), nullValue());
        assertThat(culpritTable.getCell(0, 0).getColspan(), is(1));
        assertThat(culpritTable.getCell(0, 1).getContent().getBuild(), is(buildBad1));
        assertThat(culpritTable.getCell(0, 1).getColspan(), is(1));
        assertThat(culpritTable.getCell(1, 0).getContent().getBuild(), is(buildBad2));
        assertThat(culpritTable.getCell(1, 0).getColspan(), is(2));
        assertThat(culpritTable.getCell(1, 1), nullValue());
    }
}
