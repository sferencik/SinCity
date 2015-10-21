package sferencik.teamcity.sincity;

import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.BuildCustomizer;
import jetbrains.buildServer.serverSide.BuildCustomizerFactory;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.tests.TestInfo;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.vcs.SVcsModification;
import org.jetbrains.annotations.NotNull;
import sferencik.teamcity.sincity.json.Encoder;

import java.util.*;

public class CulpritFinder {

    private final SRunningBuild newBuild;
    private final SFinishedBuild oldBuild;
    private final BuildCustomizerFactory buildCustomizerFactory;
    private final Map<String, String> sinCityParameters;

    public CulpritFinder(SRunningBuild build,
                         BuildCustomizerFactory buildCustomizerFactory,
                         Map<String, String> sinCityParameters) {

        this.newBuild = build;
        this.buildCustomizerFactory = buildCustomizerFactory;
        this.sinCityParameters = sinCityParameters;

        this.oldBuild = newBuild.getPreviousFinished();
        Loggers.SERVER.debug("[SinCity] previous build: " + oldBuild);
    }

    /**
     * Investigate if culprit finding is needed. This is so if there are relevant failures and the finishing build has
     * covered  multiple changes.
     */
    void triggerCulpritFindingIfNeeded() {
        if (newBuild.getBuildStatus().isSuccessful()) {
            Loggers.SERVER.debug("[SinCity] the build succeeded; we're done.");
            return;
        }

        if (newBuild.getContainingChanges().size() <= 1) {
            Loggers.SERVER.debug("[SinCity] no intermediate changes found; we're done.");
            return;
        }

        if (getRelevantBuildProblems().isEmpty()
                && getRelevantTestFailures().isEmpty()) {
            Loggers.SERVER.debug("[SinCity] no relevant failures; we're done.");
            return;
        }

        Loggers.SERVER.info("[SinCity] will look for culprit");
        triggerCulpritFinding();
    }

    private List<BuildProblemData> getRelevantBuildProblems()
    {
        SettingNames settingNames = new SettingNames();

        final String rbTriggerOnBuildProblem = sinCityParameters.get(new SettingNames().getRbTriggerOnBuildProblem());

        if (rbTriggerOnBuildProblem != null && rbTriggerOnBuildProblem.equals(settingNames.getNoTrigger())) {
            Loggers.SERVER.debug("[SinCity] build problems do not trigger");
            return new ArrayList<BuildProblemData>();
        }

        final List<BuildProblemData> thisBuildProblems = newBuild.getFailureReasons();
        Loggers.SERVER.debug("[SinCity] this build's problems: " + thisBuildProblems);

        if (rbTriggerOnBuildProblem != null && rbTriggerOnBuildProblem.equals(settingNames.getTriggerOnAll())) {
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

        String rbTriggerOnTestFailure = sinCityParameters.get(new SettingNames().getRbTriggerOnTestFailure());

        if (rbTriggerOnTestFailure != null && rbTriggerOnTestFailure.equals(settingNames.getNoTrigger())) {
            Loggers.SERVER.debug("[SinCity] test failures do not trigger");
            return new ArrayList<TestName>();
        }

        final List<TestName> thisBuildTestFailures = getTestNames(newBuild.getTestMessages(0, -1));
        Loggers.SERVER.debug("[SinCity] this build's test failures: " + thisBuildTestFailures);

        if (rbTriggerOnTestFailure != null && rbTriggerOnTestFailure.equals(settingNames.getTriggerOnAll())) {
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

    private void triggerCulpritFinding()
    {
        List<SVcsModification> suspectChanges = new ArrayList<SVcsModification>(newBuild.getContainingChanges());
        suspectChanges.remove(0);
        Collections.reverse(suspectChanges);

        BuildCustomizer buildCustomizer = buildCustomizerFactory.createBuildCustomizer(newBuild.getBuildType(), null);

        for (SVcsModification change : suspectChanges) {
            Loggers.SERVER.info("[SinCity] Queueing change '" + change + "' having failed build " + newBuild);

            Map<String, String> buildParameters = getCommonBuildParameters();
            buildParameters.put(new ParameterNames().getSincitySuspectChange(), change.getVersion());
            buildCustomizer.setParameters(buildParameters);

            buildCustomizer.setChangesUpTo(change);

            buildCustomizer.createPromotion().addToQueue(
                    "SinCity; investigating failures between " + oldBuild.getBuildNumber() + " and " + newBuild.getBuildNumber());
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
        if (isSetBuildProblemJsonParameter())
            parameters.put(parameterNames.getSincityBuildProblems(), Encoder.encodeBuildProblems(getRelevantBuildProblems()));
        if (isSetTestFailureJsonParameter())
            parameters.put(parameterNames.getSincityTestFailures(), Encoder.encodeTestNames(getRelevantTestFailures()));
        return parameters;
    }

    private boolean isSetBuildProblemJsonParameter() {
        final String setBuildProblemJsonParameterAsString = sinCityParameters.get(new SettingNames().getCbSetBuildProblemJsonParameter());
        return setBuildProblemJsonParameterAsString != null && Boolean.valueOf(setBuildProblemJsonParameterAsString);
    }

    private boolean isSetTestFailureJsonParameter() {
        final String setTestFailureJsonParameterAsString = sinCityParameters.get(new SettingNames().getCbSetTestFailureJsonParameter());
        return setTestFailureJsonParameterAsString != null && Boolean.valueOf(setTestFailureJsonParameterAsString);
    }
}
