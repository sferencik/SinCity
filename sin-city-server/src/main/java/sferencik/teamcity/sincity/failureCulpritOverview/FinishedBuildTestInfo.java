package sferencik.teamcity.sincity.failureCulpritOverview;

import jetbrains.buildServer.serverSide.BuildStatisticsOptions;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.tests.TestName;
import sferencik.teamcity.sincity.SinCityUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class describes a finished build from the perspective of the tests it ran. Specifically, it can tell you if a
 * given test
 * (a) passed (boolean testSucceeded(TestName))
 * (b) failed (boolean testFailed(TestName))
 * (c) didn't even run (!testFailed && !testSucceeded)
 * The last option is quite common in the context of culprit-finding (see README.md where we propose running subsets of
 * tests in the culprit-finding builds).
 */
public class FinishedBuildTestInfo {
    private final SFinishedBuild build;

    private Set<TestName> allTestsCache = null;
    private List<TestName> failingTestsCache = null;

    public FinishedBuildTestInfo(SFinishedBuild build) {
        this.build = build;
    }

    public boolean testFailed(TestName testFailure) {

        if (failingTestsCache == null) {
            // lazy-initialise the cache (list of failing tests)
            failingTestsCache = SinCityUtils.getNamesOfFailingTests(build);
        }

        return failingTestsCache.contains(testFailure);
    }

    public boolean testSucceeded(TestName testFailure) {
        if (testFailed(testFailure))
            return false;

        if (allTestsCache == null) {
            // lazy-initialise the cache (list of all tests)
            allTestsCache = new HashSet<TestName>();
            for (STestRun testRun : build.getBuildStatistics(BuildStatisticsOptions.ALL_TESTS_NO_DETAILS).getAllTests()) {
                allTestsCache.add(testRun.getTest().getName());
            }
        }

        return allTestsCache.contains(testFailure);
    }
}
