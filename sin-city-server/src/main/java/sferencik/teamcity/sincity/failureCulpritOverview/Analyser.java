package sferencik.teamcity.sincity.failureCulpritOverview;

import jetbrains.buildServer.tests.TestName;
import org.jetbrains.annotations.NotNull;
import sferencik.teamcity.sincity.FinishedBuildWithChanges;
import sferencik.teamcity.sincity.SinCityUtils;

import java.util.*;

public class Analyser {
    /**
     * Create an instance of Table for use in rendering the overview tab.
     *
     * @param buildsWithContainingChanges a list of FinishedBuildWithChanges objects describing the history
     * @return the Table
     */
    @NotNull
    public static Table createCulpritMatrix(List<FinishedBuildWithChanges> buildsWithContainingChanges) {
        if (buildsWithContainingChanges.size() == 0) {
            // there are no builds and thus no outstanding failuresInMostRecentBuild
            return new Table(
                    new ArrayList<TestName>(),
                    new ArrayList<FinishedBuildWithChanges>(),
                    new HashMap<TestName, FinishedBuildWithChanges>(),
                    new HashMap<TestName, Set<FinishedBuildWithChanges>>());
        }

        // the outstanding test failures that the overview tab will help diagnose; get them from the last build
        List<TestName> failuresInMostRecentBuild = new ArrayList<TestName>();
        for (TestName testFailure : SinCityUtils.getNamesOfFailingTests(buildsWithContainingChanges.get(0).getBuild())) {
            failuresInMostRecentBuild.add(testFailure);
        }

        // the build(s) suspected of causing each test failure
        Map<TestName, Set<FinishedBuildWithChanges>> buildsSuspectedOfCausingFailure = new HashMap<TestName, Set<FinishedBuildWithChanges>>();

        // the build in which the test failure first occurred
        Map<TestName, FinishedBuildWithChanges> buildInWhichFailureFirstOccurred = new HashMap<TestName, FinishedBuildWithChanges>();

        // the test failures which the build is suspected of causing
        Map<FinishedBuildWithChanges, Set<TestName>> failuresSuspectedToBeCausedByBuild = new LinkedHashMap<FinishedBuildWithChanges, Set<TestName>>();

        HashSet<TestName> issuesNotYetClarified = new HashSet<TestName>(failuresInMostRecentBuild);
        int consecutiveBuildsWithoutNewInfo = 0;   // how many builds have we gone without seeing any new info?

        for (FinishedBuildWithChanges build : buildsWithContainingChanges) {
            FinishedBuildTestInfo buildTestInfo = new FinishedBuildTestInfo(build.getBuild());

            consecutiveBuildsWithoutNewInfo++;

            // use an explicit iterator so I can remove from the set while iterating it
            Iterator<TestName> issuesNotYetClarifiedIterator = issuesNotYetClarified.iterator();
            while (issuesNotYetClarifiedIterator.hasNext()) {
                TestName testFailure = issuesNotYetClarifiedIterator.next();

                if (buildTestInfo.testFailed(testFailure)) {
                    // the test failure occurred in this build (the build problem occurred/the test failed)

                    // absolve the previous suspects
                    if (buildsSuspectedOfCausingFailure.containsKey(testFailure)) {
                        for (FinishedBuildWithChanges previousSuspectBuild : buildsSuspectedOfCausingFailure.get(testFailure)) {
                            failuresSuspectedToBeCausedByBuild.get(previousSuspectBuild).remove(testFailure);
                            if (failuresSuspectedToBeCausedByBuild.get(previousSuspectBuild).isEmpty()) {
                                // this build has been absolved of all blame
                                failuresSuspectedToBeCausedByBuild.remove(previousSuspectBuild);
                            }
                        }
                    }

                    // take note of this build where the test failure occurred
                    buildInWhichFailureFirstOccurred.put(testFailure, build);

                    // add this test failure to the blame list for this build
                    if (!failuresSuspectedToBeCausedByBuild.containsKey(build)) {
                        failuresSuspectedToBeCausedByBuild.put(build, new HashSet<TestName>());
                    }
                    failuresSuspectedToBeCausedByBuild.get(build).add(testFailure);
                    buildsSuspectedOfCausingFailure.put(testFailure, new HashSet<FinishedBuildWithChanges>(Collections.singletonList(build)));

                    consecutiveBuildsWithoutNewInfo = 0;
                }
                else if (buildTestInfo.testSucceeded(testFailure)) {
                    // the test failure didn't occur in this build (the test passed); the investigation is now complete;
                    // the list of suspects is final
                    issuesNotYetClarifiedIterator.remove();

                    consecutiveBuildsWithoutNewInfo = 0;
                }
                else {
                    // the test failure didn't exhibit itself but it seems the test never ran either; this may be due to
                    // several reasons:
                    // i)   the test is new and it's been failing from the start; we're now going down the history of
                    //      builds where the test didn't even exist yet; we could go all the way to the Big Bang and
                    //      find nothing
                    // ii)  the build may have failed before running the test; again, in this case we should find the
                    //      test (passing or failing) further back in the history and decide based on that
                    // iii) the test existed as of this version but we didn't run it, possibly because someone had
                    //      somehow skipped it (we actually suggest running only a subset of tests in README.md when
                    //      explaining the meaning of the %sincity.test.failure.json% parameter; if this is implemented
                    //      and the culprit-finding builds only rerun the new failures, the old failures won't make an
                    //      appearance in the culprit-finding builds at all - and yet they predate these builds); in
                    //      this case we should find the test (passing or failing) further back in the history and
                    //      decide based on that
                    // at any rate, this build's changes must be treated as suspect

                    // add this testFailure to the blame list for this build
                    if (!failuresSuspectedToBeCausedByBuild.containsKey(build)) {
                        failuresSuspectedToBeCausedByBuild.put(build, new HashSet<TestName>());
                    }
                    failuresSuspectedToBeCausedByBuild.get(build).add(testFailure);
                    buildsSuspectedOfCausingFailure.get(testFailure).add(build);
                }
            }

            if (consecutiveBuildsWithoutNewInfo >= getMaxNumberOfConsecutiveBuildsWithoutNewInfo())
                break;
        }

        return new Table(
                failuresInMostRecentBuild,
                new ArrayList<FinishedBuildWithChanges>(failuresSuspectedToBeCausedByBuild.keySet()),
                buildInWhichFailureFirstOccurred,
                buildsSuspectedOfCausingFailure
        );
    }

    static int getMaxNumberOfConsecutiveBuildsWithoutNewInfo() {
        return 10;
    }
}
