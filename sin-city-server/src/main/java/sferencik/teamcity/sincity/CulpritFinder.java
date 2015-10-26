package sferencik.teamcity.sincity;

import com.intellij.openapi.util.text.StringUtil;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.tests.TestInfo;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.vcs.SVcsModification;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sferencik.teamcity.sincity.json.Encoder;

import java.util.*;

public class CulpritFinder {

    @NotNull private final SBuild newBuild;
    @Nullable private final SFinishedBuild oldBuild;
    @NotNull private final String triggerOnBuildProblem;
    @NotNull private final String triggerOnTestFailure;
    private final boolean setBuildProblemJsonParameter;
    private final boolean setTestFailureJsonParameter;
    @NotNull private final BuildQueue buildQueue;
    @NotNull private final BuildCustomizerFactory buildCustomizerFactory;

    /**
     * Run culprit finding between oldBuild and newBuild.
     * @param newBuild the top of the culprit-finding range. Can be an SFinishedBuild (for manually triggered builds) or
     *                 an SRunningBuild (the finishing build for automatically triggered builds).
     * @param oldBuild the bottom of the culprit-finding range. Can be null if newBuild is the only build so far in the
     *                 build configuration.
     * @param triggerOnBuildProblem
     * @param triggerOnTestFailure
     * @param setBuildProblemJsonParameter
     * @param setTestFailureJsonParameter
     * @param buildCustomizerFactory
     */
    public CulpritFinder(@NotNull SBuild newBuild,
                         @Nullable SFinishedBuild oldBuild,
                         @NotNull String triggerOnBuildProblem,
                         @NotNull String triggerOnTestFailure,
                         boolean setBuildProblemJsonParameter,
                         boolean setTestFailureJsonParameter,
                         @NotNull BuildCustomizerFactory buildCustomizerFactory,
                         @NotNull BuildQueue buildQueue
                         ) {

        this.newBuild = newBuild;
        this.oldBuild = oldBuild;
        this.buildCustomizerFactory = buildCustomizerFactory;
        this.triggerOnBuildProblem = triggerOnBuildProblem;
        this.triggerOnTestFailure = triggerOnTestFailure;
        this.setBuildProblemJsonParameter = setBuildProblemJsonParameter;
        this.setTestFailureJsonParameter = setTestFailureJsonParameter;
        this.buildQueue = buildQueue;

        Loggers.SERVER.debug("[SinCity] culprit finding " +
                (oldBuild == null
                        ? "for"
                        : "between [" + oldBuild + "] and") +
                " [" + newBuild + "]");
    }

    /**
     * Investigate if culprit finding is needed. This is so if there are relevant failures and the finishing build has
     * covered  multiple changes.
     */
    public void triggerCulpritFindingIfNeeded() {
        if (newBuild.getBuildStatus().isSuccessful()) {
            Loggers.SERVER.debug("[SinCity] the build succeeded; we're done.");
            return;
        }

        List<SVcsModification> changesBetweenBuilds = null;
        try {
            changesBetweenBuilds = getChanges();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        if (changesBetweenBuilds.size() <= 1) {
            Loggers.SERVER.debug("[SinCity] no intermediate changes found; we're done.");
            return;
        }

        if (getRelevantBuildProblems().isEmpty()
                && getRelevantTestFailures().isEmpty()) {
            Loggers.SERVER.debug("[SinCity] no relevant failures; we're done.");
            return;
        }

        Loggers.SERVER.info("[SinCity] will look for culprit");
        triggerCulpritFinding(changesBetweenBuilds);
    }

    private List<BuildProblemData> getRelevantBuildProblems()
    {
        SettingNames settingNames = new SettingNames();


        if (triggerOnBuildProblem.equals(settingNames.getNoTrigger())) {
            Loggers.SERVER.debug("[SinCity] build problems do not trigger");
            return new ArrayList<BuildProblemData>();
        }

        final List<BuildProblemData> thisBuildProblems = newBuild.getFailureReasons();
        Loggers.SERVER.debug("[SinCity] this build's problems: " + thisBuildProblems);

        if (triggerOnBuildProblem.equals(settingNames.getTriggerOnAll())) {
            Loggers.SERVER.debug("[SinCity] reporting all build problems");
            return thisBuildProblems;
        }

        final List<BuildProblemData> previousBuildProblems = oldBuild == null
                ? new ArrayList<BuildProblemData>()
                : oldBuild.getFailureReasons();
        Loggers.SERVER.debug("[SinCity] previous build's problems: " + previousBuildProblems);

        final List<BuildProblemData> newProblems = new ArrayList<BuildProblemData>(thisBuildProblems);
        newProblems.removeAll(previousBuildProblems);
        Loggers.SERVER.debug("[SinCity] new build problems: " + newProblems);
        Loggers.SERVER.debug("[SinCity] reporting new build problems");

        return newProblems;
    }

    private List<TestName> getRelevantTestFailures()
    {
        SettingNames settingNames = new SettingNames();


        if (triggerOnTestFailure.equals(settingNames.getNoTrigger())) {
            Loggers.SERVER.debug("[SinCity] test failures do not trigger");
            return new ArrayList<TestName>();
        }

        final List<TestName> thisBuildTestFailures = getTestNames(newBuild.getTestMessages(0, -1));
        Loggers.SERVER.debug("[SinCity] this build's test failures: " + thisBuildTestFailures);

        if (triggerOnTestFailure.equals(settingNames.getTriggerOnAll())) {
            Loggers.SERVER.debug("[SinCity] reporting all test failures");
            return thisBuildTestFailures;
        }

        final List<TestName> previousBuildTestFailures = oldBuild == null
                ? new ArrayList<TestName>()
                : getTestNames(oldBuild.getTestMessages(0, -1));
        Loggers.SERVER.debug("[SinCity] previous build's test failures: " + previousBuildTestFailures);

        final List<TestName> relevantTestFailures = new ArrayList<TestName>(thisBuildTestFailures);
        relevantTestFailures.removeAll(previousBuildTestFailures);
        Loggers.SERVER.debug("[SinCity] relevant test failures: " + relevantTestFailures);
        Loggers.SERVER.debug("[SinCity] reporting new test failures");

        return relevantTestFailures;
    }

    private List<TestName> getTestNames(List<TestInfo> tests)
    {
        List<TestName> testNames = new ArrayList<TestName>();
        for (TestInfo test : tests) {
            testNames.add(test.getTestName());
        }
        return testNames;
    }

    private void triggerCulpritFinding(List<SVcsModification> changesBetweenBuilds)
    {
        List<SVcsModification> suspectChanges = new ArrayList<SVcsModification>(changesBetweenBuilds);
        suspectChanges.remove(0);
        Collections.reverse(suspectChanges);

        BuildCustomizer buildCustomizer = buildCustomizerFactory.createBuildCustomizer(newBuild.getBuildType(), null);

        for (SVcsModification change : suspectChanges) {
            Loggers.SERVER.info("[SinCity] Queueing change '" + change + "' having failed build " + newBuild);

            Map<String, String> buildParameters = getCommonBuildParameters();
            buildParameters.put(new ParameterNames().getSincitySuspectChange(), change.getVersion());
            buildCustomizer.setParameters(buildParameters);

            buildCustomizer.setChangesUpTo(change);

            SQueuedBuild queuedBuild = buildCustomizer.createPromotion().addToQueue(
                    "SinCity; investigating failures between " + oldBuild.getBuildNumber() + " and " + newBuild.getBuildNumber());
            moveBuildBeyondAllCulpritFindingBuilds(queuedBuild);
        }
    }

    @NotNull
    private Map<String, String> getCommonBuildParameters() {
        Map<String, String> parameters = new HashMap<String, String>();
        final ParameterNames parameterNames = new ParameterNames();
        parameters.put(parameterNames.getSincityRangeTopBuildId(), String.valueOf(newBuild.getBuildId()));
        parameters.put(parameterNames.getSincityRangeTopBuildNumber(), newBuild.getBuildNumber());
        parameters.put(parameterNames.getSincityRangeBottomBuildId(), oldBuild == null
                ? "n/a"
                : String.valueOf(oldBuild.getBuildId()));
        parameters.put(parameterNames.getSincityRangeBottomBuildNumber(), oldBuild == null
                ? "n/a"
                : oldBuild.getBuildNumber());
        if (setBuildProblemJsonParameter)
            parameters.put(parameterNames.getSincityBuildProblems(), Encoder.encodeBuildProblems(getRelevantBuildProblems()));
        if (setTestFailureJsonParameter)
            parameters.put(parameterNames.getSincityTestFailures(), Encoder.encodeTestNames(getRelevantTestFailures()));
        return parameters;
    }

    // see https://devnet.jetbrains.com/message/5561038 for the explanation of why we need this method, and the
    // algorithm used
    private List<SVcsModification> getChanges() throws Exception {
        // use a linked set to avoid duplicates and to keep the changes in descending order
        Set<SVcsModification> changeList = new LinkedHashSet<SVcsModification>();
        BuildPromotion buildPromotion = newBuild.getBuildPromotion();

        // find oldBuildchange, the changelist of oldBuild; in the while loop below, reaching oldBuildChange will be our
        // terminating condition
        // NB: oldBuildChange will be null if oldBuild was executed before any VCS was attached to the build
        // configuration; otherwise oldBuild really should have a change associated with it
        SVcsModification oldBuildChange = null;
        if (oldBuild != null) {
            FinishedBuildWithChange oldBuildWithChange = FinishedBuildWithChange.fromSFinishedBuild(oldBuild);
            if (oldBuildWithChange != null)
                oldBuildChange = oldBuildWithChange.getChange();
        }

        while (true) {
            if (oldBuildChange == null) {
                // we are supposed to search all the way to Big Bang
                if (buildPromotion == null) {
                    // we have reached Big Bang
                    return new ArrayList<SVcsModification>(changeList);
                }
                else {
                    // keep going; we haven't reached Big Bang yet
                }
            }
            else {
                // we should only go as far as oldBuildChange
                if (buildPromotion == null) {
                    // we have reached Big Bang and never found oldBuildChange
                    throw new Exception("Could not find changes between " + oldBuild + " and " + newBuild);
                }
                else if (buildPromotion.getContainingChanges().contains(oldBuildChange)) {
                    // we have reached oldBuildChange
                    changeList.addAll(buildPromotion.getContainingChanges().subList(0, buildPromotion.getContainingChanges().indexOf(oldBuildChange)));
                    return new ArrayList<SVcsModification>(changeList);
                }
                else {
                    // keep going; we haven't seen oldBuildChange yet
                }
            }

            Loggers.SERVER.debug("[SinCity] build promotion " + buildPromotion);
            Loggers.SERVER.debug("[SinCity] changes " + buildPromotion.getContainingChanges());
            changeList.addAll(buildPromotion.getContainingChanges());
            buildPromotion = buildPromotion.getPreviousBuildPromotion(SelectPrevBuildPolicy.SINCE_LAST_BUILD);
        }
    }

    private void moveBuildBeyondAllCulpritFindingBuilds(SQueuedBuild buildToMove) {

        List<SQueuedBuild> queuedBuilds = buildQueue.getItems();
        ListIterator<SQueuedBuild> queueIterator = queuedBuilds.listIterator(queuedBuilds.size());
        boolean isBuildToMoveSeen = false;

        // iterate the queue bottom-to-top, until we find the bottom-most culprit-finding build in it
        while (queueIterator.hasPrevious()) {
            int iteratedBuildIndex = queueIterator.previousIndex();
            SQueuedBuild iteratedBuild = queueIterator.previous();

            if (iteratedBuild.equals(buildToMove)) {
                // this is the build we're trying to move up (presumably somewhere near the end of the queue); make a
                // note of it but otherwise leave it where it is for now
                Loggers.SERVER.debug("[SinCity] build seen at position " + iteratedBuildIndex);
                isBuildToMoveSeen = true;
                continue;
            }

            if (!StringUtil.isEmpty(iteratedBuild.getBuildPromotion().getCustomParameters().get(new ParameterNames().getSincityRangeTopBuildId()))) {
                // we have found a culprit-finding build in the queue (since we're iterating bottom-up, this is the
                // bottom-most culprit-finding build)
                if (isBuildToMoveSeen) {
                    // ... and our build is below it; move it up
                    moveBuildToPosition(buildToMove, queuedBuilds, iteratedBuildIndex + 1);
                }
                else {
                    // ... and our build is nowhere to be seen; do nothing
                }
                return;
            }
        }

        if (isBuildToMoveSeen) {
            // we haven't found any culprit-finding build in the queue, so let's move ours all the way to the top
            moveBuildToPosition(buildToMove, queuedBuilds, 0);
        }
    }

    private void moveBuildToPosition(SQueuedBuild buildToMove, List<SQueuedBuild> queuedBuilds, int newBuildPosition) {

        queuedBuilds.remove(buildToMove);
        queuedBuilds.add(newBuildPosition, buildToMove);
        Loggers.SERVER.debug("[SinCity] build moved to position " + newBuildPosition);

        List<String> queuedItemIds = new ArrayList<String>();
        for (SQueuedBuild build : queuedBuilds)
            queuedItemIds.add(build.getItemId());
        buildQueue.applyOrder(queuedItemIds.toArray(new String[queuedItemIds.size()]));
    }
}
