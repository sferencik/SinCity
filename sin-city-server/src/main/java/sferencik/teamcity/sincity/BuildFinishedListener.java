package sferencik.teamcity.sincity;

import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.tests.TestInfo;
import jetbrains.buildServer.tests.TestName;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.vcs.SVcsModification;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class BuildFinishedListener
{

    public static final String SINCITY_RANGE_TOP_BUILD_ID = "sincity.range.top.build.id";
    public static final String SINCITY_RANGE_TOP_BUILD_NUMBER = "sincity.range.top.build.number";
    public static final String SINCITY_RANGE_BOTTOM_BUILD_ID = "sincity.range.bottom.build.id";
    public static final String SINCITY_RANGE_BOTTOM_BUILD_NUMBER = "sincity.range.bottom.build.number";

    public BuildFinishedListener(@NotNull final EventDispatcher<BuildServerListener> listener,
                                 final BuildCustomizerFactory buildCustomizerFactory)
    {
        listener.addListener(new BuildServerAdapter()
        {
            @Override
            public void buildFinished(@NotNull SRunningBuild build)
            {
                Loggers.SERVER.debug("[SinCity] build: " + build);

                final SBuildType buildType = build.getBuildType();
                if (buildType == null)
                    return;

                SFinishedBuild previousBuild = build.getPreviousFinished();
                Loggers.SERVER.debug("[SinCity] previous build: " + previousBuild);

                final List<SVcsModification> containingChanges = build.getContainingChanges();
                Loggers.SERVER.debug("[SinCity] changes (" + containingChanges.size() + "): " + containingChanges);

                for (SBuildFeatureDescriptor feature : buildType.getBuildFeatures()) {
                    final Class<? extends BuildFeature> featureClass = feature.getBuildFeature().getClass();
                    Loggers.SERVER.debug("[SinCity] found plugin: " + featureClass);
                    if (!featureClass.equals(SinCityBuildFeature.class))
                        continue;

                    tagBuild(build, feature);
                    if (build.getBuildStatus().isSuccessful()) {
                        Loggers.SERVER.debug("[SinCity] the build succeeded; we're done.");
                        return;
                    }
                    if (containingChanges.size() <= 1) {
                        Loggers.SERVER.debug("[SinCity] no intermediate changes found; we're done.");
                        return;
                    }

                    if (getRelevantBuildProblems(build, previousBuild, feature).isEmpty()
                            && getRelevantTestFailures(build, previousBuild, feature).isEmpty()) {
                        Loggers.SERVER.debug("[SinCity] no relevant failures; we're done.");
                        return;
                    }

                    triggerCulpritFinding(buildType, build, previousBuild, containingChanges);

                    break; // for now we only allow a single SinCity feature (see SinCityBuildFeature.isMultipleFeaturesPerBuildTypeAllowed())
                }
            }

            private void tagBuild(SRunningBuild thisBuild, SBuildFeatureDescriptor sinCityFeature) {
                // tag the finished build
                final String triggeredBySinCityParameterValue = thisBuild.getParametersProvider().get(SINCITY_RANGE_TOP_BUILD_ID);
                SettingNames settingNames = new SettingNames();
                String tagParameterName = triggeredBySinCityParameterValue == null
                        ? settingNames.getTagNameForBuildsNotTriggeredBySinCity()
                        : settingNames.getTagNameForBuildsTriggeredBySinCity();
                final String tagName = sinCityFeature.getParameters().get(tagParameterName);

                if (tagName == null || tagName.isEmpty())
                    return;

                Loggers.SERVER.debug("[SinCity] tagging build with '" + tagName + "'");
                final List<String> resultingTags = new ArrayList<String>(thisBuild.getTags());
                resultingTags.add(tagName);
                thisBuild.setTags(resultingTags);
            }

            private List<BuildProblemData> getRelevantBuildProblems(
                    SRunningBuild thisBuild,
                    SFinishedBuild previousBuild,
                    SBuildFeatureDescriptor feature)
            {
                final String rbTriggerOnBuildProblem = feature.getParameters().get(new SettingNames().getRbTriggerOnBuildProblem());

                if (rbTriggerOnBuildProblem != null && rbTriggerOnBuildProblem.equals("No")) {
                    Loggers.SERVER.debug("[SinCity] build problems do not trigger");
                    return new ArrayList<BuildProblemData>();
                }

                final List<BuildProblemData> thisBuildProblems = thisBuild.getFailureReasons();
                Loggers.SERVER.debug("[SinCity] this build's problems: " + thisBuildProblems);

                if (rbTriggerOnBuildProblem != null && rbTriggerOnBuildProblem.equals("All")) {
                    Loggers.SERVER.debug("[SinCity] reporting all build problems");
                    return thisBuildProblems;
                }

                final List<BuildProblemData> previousBuildProblems = previousBuild == null
                        ? new ArrayList<BuildProblemData>()
                        : previousBuild.getFailureReasons();
                Loggers.SERVER.debug("[SinCity] previous build's problems: " + previousBuildProblems);

                final List<BuildProblemData> newProblems = new ArrayList<BuildProblemData>(thisBuildProblems);
                newProblems.removeAll(previousBuildProblems);
                Loggers.SERVER.debug("[SinCity] new build problems: " + newProblems);
                Loggers.SERVER.debug("[SinCity] reporting new build problems");

                return newProblems;
            }

            private List<TestName> getRelevantTestFailures(
                    SRunningBuild thisBuild,
                    SFinishedBuild previousBuild,
                    SBuildFeatureDescriptor feature)
            {
                String rbTriggerOnTestFailure = feature.getParameters().get(new SettingNames().getRbTriggerOnTestFailure());

                if (rbTriggerOnTestFailure != null && rbTriggerOnTestFailure.equals("No")) {
                    Loggers.SERVER.debug("[SinCity] test failures do not trigger");
                    return new ArrayList<TestName>();
                }

                final List<TestName> thisBuildTestFailures = getTestNames(thisBuild.getTestMessages(0, -1));
                Loggers.SERVER.debug("[SinCity] this build's test failures: " + thisBuildTestFailures);

                if (rbTriggerOnTestFailure != null && rbTriggerOnTestFailure.equals("All")) {
                    Loggers.SERVER.debug("[SinCity] reporting all test failures");
                    return thisBuildTestFailures;
                }

                final List<TestName> previousBuildTestFailures = previousBuild == null
                        ? new ArrayList<TestName>()
                        : getTestNames(previousBuild.getTestMessages(0, -1));
                Loggers.SERVER.debug("[SinCity] previous build's test failures: " + thisBuildTestFailures);

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

            private void triggerCulpritFinding(
                    SBuildType buildType,
                    SRunningBuild thisBuild,
                    SFinishedBuild previousBuild,
                    List<SVcsModification> containingChanges)
            {
                Loggers.SERVER.info("[SinCity] will look for culprit");

                List<SVcsModification> suspectChanges = new ArrayList<SVcsModification>(containingChanges);
                suspectChanges.remove(0);
                Collections.reverse(suspectChanges);

                BuildCustomizer buildCustomizer = buildCustomizerFactory.createBuildCustomizer(buildType, null);
                for (SVcsModification change : suspectChanges) {
                    Loggers.SERVER.info("[SinCity] Queueing change '" + change + "' having failed build " + thisBuild);

                    buildCustomizer.setChangesUpTo(change);

                    Map<String, String> parameters = new HashMap<String, String>();
                    parameters.put(SINCITY_RANGE_TOP_BUILD_ID, String.valueOf(thisBuild.getBuildId()));
                    parameters.put(SINCITY_RANGE_TOP_BUILD_NUMBER, thisBuild.getBuildNumber());
                    parameters.put(SINCITY_RANGE_BOTTOM_BUILD_ID, previousBuild == null
                            ? "n/a"
                            : String.valueOf(previousBuild.getBuildId()));
                    parameters.put(SINCITY_RANGE_BOTTOM_BUILD_NUMBER, previousBuild == null
                            ? "n/a"
                            : previousBuild.getBuildNumber());
                    buildCustomizer.setParameters(parameters);

                    buildCustomizer.createPromotion().addToQueue("SinCity, failures of " + thisBuild.getBuildNumber());
                }
            }
        });
    }
}
