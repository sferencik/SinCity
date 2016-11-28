package sferencik.teamcity.sincity;

import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.tests.TestInfo;
import jetbrains.buildServer.tests.TestName;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SinCityUtils {

    public static List<TestName> getNamesOfFailingTests(SBuild build) {
        List<TestName> testNames = new ArrayList<TestName>();
        for (TestInfo test : build.getTestMessages(0, -1)) {
            testNames.add(test.getTestName());
        }
        return testNames;
    }

    /**
     * Return the history of a build configuration, including builds that failed with "artifact dependency failed." To
     * do this, we must ask for the history including the cancelled builds (obviously JetBrains think of a build with an
     * artifact dependency failure as a kind of cancelled build) and then filter out the chaff (the builds that actually
     * got cancelled, e.g. by a user pressing "Stop").
     *
     * From SinCity's point of view, builds with artifact dependencies are not "cancelled" or "failed to start" because
     * they *do* trigger buildFinished and hence they trigger culprit finding. If such culprit finding looks back at the
     * previous runs, it should be able to see that e.g. the previous changelist also produced an artifact dependency
     * failure. Example: changelists a, b, c produce builds A (success), B (artifact dependency failure), C (artifact
     * dependency failure); as C is finishing, it triggers culprit finding; however, looking at getPreviousFinished() or
     * at getHistory(..., false, ...) would ignore B and return A as the last finished build, which would make SinCity
     * think that there was no build for b and trigger a rebuild as of b (where instead we have B to inform us).
     *
     * @param buildType the build configuration for which we want the history
     * @return the build history
     */
    public static List<SFinishedBuild> getFullHistory(@NotNull SBuildType buildType) {
        List<SFinishedBuild> buildTypeHistoryWithCancelledBuilds = buildType.getHistory(null, true, true);

        // now filter the history, excluding the cancelled builds (these got included since we asked for the full
        // history)
        List<SFinishedBuild> buildTypeHistory = new ArrayList<SFinishedBuild>();
        for (SFinishedBuild possiblyCancelledBuild : buildTypeHistoryWithCancelledBuilds) {
            if (possiblyCancelledBuild.getCanceledInfo() == null)
                buildTypeHistory.add(possiblyCancelledBuild);
        }

        return buildTypeHistory;
    }
}
