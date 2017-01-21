# SinCity: hunting down the gangstas [![Build Status](https://travis-ci.org/sferencik/SinCity.svg?branch=master)](https://travis-ci.org/sferencik/SinCity)

SinCity is a TeamCity plugin that helps you find out who broke a build.

Your build configuration has gone from green to red (or from red to redder), but the build that failed covered 6 commits. Which of the 6 commits is to blame?

![From green to red](/images/from-green-to-red.PNG)

You can manually trigger the same build for the 5 intermediate commits, but this can be a tedious process. SinCity lets you achieve this with a few clicks, and can even trigger the intermediate builds [automatically](#automatic-culprit-finding) if you choose to do so.

## Manually triggering the culprit finding

The plugin is most useful if your builds are long and/or expensive. In that situation your builds typically cover more than one commit, which is perfectly fine and probably even desirable - until someone breaks the build. When that happens (which is hopefully rare), you typically *do* want to build each suspect commit individually.

SinCity adds a new tab called *Trigger culprit finding* to every build configuration. In that tab, you pick two finished builds that will define your investigation range. For example, for the new test failure above, we want to investigate the range of 6 commits between builds #5 and #6.

<a name="manual-trigger"></a>
![Manual trigger](/images/manual-trigger-tab.PNG)

When you select the two builds and click *Run*, SinCity will trigger builds for the 5 intermediate commits.

You don't need to pick consecutive builds (like we did). For example, you could run the culprit finding between builds #3 and #6. In that range there have been 5 + 4 + 6 = 15 changes (see the screenshot at the top) so the culprit finding would result in 14 builds. 2 of these 14 should be effectively "reruns" of #4 and #5.

### What counts as a build failure

Same as TeamCity itself, SinCity recognises two types of build issues:

1. high-level build problems (e.g. "Powershell runner #2 returned a non-zero exit code", "Command-line runner #1 timed out", "Some tests have failed")
2. individual test failures (e.g. "*MySuite: my.test.package.MyClass.myTest* failed")

For either of these two issue types, you can specify whether SinCity should trigger culprit finding when they occur. You can choose from three options:
* No (i.e. ignore this kind of issues)
* New (i.e. only trigger culprit finding if there are new errors of this kind)
* All (i.e. trigger culprit finding even if all the error of this kind already occurred in the previous build)

![What counts as build failure](/images/two-types-of-issues.PNG)

The default behaviour is as shown above, i.e. investigate only if there are *new* build problems or new test failures.

### Example

Let's find which of the 6 commits shown above broke the test. We hit *Run* at the bottom of the *Trigger culprit finding* tab (see screenshot [above](#manual-trigger)) and SinCity queues 5 builds.

After they've completed, the situation looks as follows:

![overview-with-cf-builds](/images/overview-with-cf-builds.PNG)

As a reminder, the first failure was in build #6. Builds #7 .. #11 were triggered by SinCity. TeamCity is showing all the builds ordered by start time. In our case it's more useful to order them logically, i.e. by commit time. To do that, let's switch to the *Change Log* view:

![change-log-with-cf-builds](/images/change-log-with-cf-builds.PNG)

Notice how the builds #5 and #6 delimit the range which was originally opaque to us. SinCity has helped us identify that the test got broken in build #9, i.e. most likely by the change marked as "01:24:39".

## The *Failure Culprit Overview* tab

SinCity gives you another way to look at the test failures and identify the suspects. Each build configuration has a new tab called *Failure Culprit Overview*. This is a table showing, for each test failure, which range of commits is on the suspect list. (Ideally, we want to narrow down the list of suspects to one.)

Consider the following example: in the last three builds, a build configuration has gone from green (zero failures) to one failure (in build #24) and to two failures (in build #25):

![zero-one-two-failures](/images/zero-one-two-failures.PNG)

On closer inspection, we see that the first test failure was caused by one of four commits, and the second test failure also has four suspects:

![four-and-four-suspects](/images/four-and-four-suspects.PNG)

The new *Failure Culprit Overview* tab gives you a more visual representation of the situation:

![failure-culprit-overview](/images/failure-culprit-overview.PNG)

It tells you that:

* test `MySuite: baz` first failed in build #24, which covered four commits; it lists these commits
* test `MySuite: bar` first failed in build #25, which also covered four commits

It clearly highlights that the commits that broke `bar` are innocent when it comes to `baz`, and vice versa.

This is very useful in more complex situations, as it lets you quickly identify which failures may be related, whom to address, and it also shows you which test failures don't have a clear culprit (i.e. multiple commits are still suspect).

OK, so let's drill down by running the culprit finding. Switch to the *Trigger culprit finding* tab. We need to find culprit over two ranges, four commits each, i.e. we need to run 3 + 3 builds. If we don't mind running one extra build, we can trigger it as follows:

![manual-trigger-tab-in-fco-story](/images/manual-trigger-tab-in-fco-story.PNG)

This queues 7 builds. After they are complete, we can use the *Change Log* tab as previously:

![change-log-in-fco-story](/images/change-log-in-fco-story.PNG)

Note the following:

* the original builds are #23 (zero failures), #24 (one failure), #25 (two failures)
* by triggering culprit finding for this range, we ran 7 new builds; #29 was extra and was equivalent to #24
* TeamCity now clearly shows that the first test failure came in build #27, i.e. after commit `d152c3d4e4ec`; similarly, the second build failure appeared in #30, i.e. after commit `164404abc16c`.

This analysis, however, strains the eye. Look at how the same data is presented in the *Failure culprit overview* tab:

![failure-culprit-overview-in-fco-story](/images/failure-culprit-overview-in-fco-story.PNG)

This shows the exact same results as we just laboriously extracted from the *Change Log* above, in a much clearer fashion. Notice that the *Failure culprit overview* tab doesn't show *all* the changes. It only shows those which are relevant for identifying the culprit. Thus, for two test failures (two rows) it only shows two columns now. This is the ideal state: you can assign investigations and drive the build configuration to green again.

## Automatic culprit finding

You can set your build configuration to trigger the culprit finding automatically every time there are build failures and the failing build covered more than one commit. To do so, enable the "SinCity" build feature for your build configuration.

![build-feature](/images/build-feature.PNG)


![build-feature-details](/images/build-feature-details.PNG)

The bottom part of this screen should already look familiar. The settings in the top part (*Tagging*) are useful if you want to distinguish the builds triggered via SinCity from the other builds (since they all intermingle in the build configuration history). If you supply tag names here, SinCity will apply them to all the builds in the given build configuration. For the setting above, all SinCity-triggered builds would get the *`culprit-finding`* tag, while all the other builds would get the *`regular`* tag.

If you leave the tagging text fields empty, no tagging is done.

The culprit-finding builds triggered automatically are put to the queue as soon as the failing build completes. Given that they logically belong to the just-finished build, they are put to the *top* of the queue so they can run ASAP. The same is not true for manually triggered builds, since they are "merely" someone's personal initiative, just like when someone triggers a build manually.

## Configuration parameters of the triggered builds

Each build triggered by SinCity will have the following configuration parameters set:

* `%sincity.range.bottom.build.id%` and `%sincity.range.top.build.id%`: the build IDs (internal TeamCity IDs) of the two builds that define the culprit-finding range; these references allow your triggered builds to ask questions about the original builds (e.g. using the TeamCity REST API); in our example, these were 633 and 635 respectively
* `%sincity.range.bottom.build.number%` and `%sincity.range.top.build.number%`: the build numbers (display numbers) of the two builds that define the culprit-finding range; these are mostly for user convenience, so you can quickly see what range the build is investigating; in our example, these were 5 and 6 respectively
* `%sincity.suspect.change%`: the "version" of the suspect commit for which this build is running; this is the display name of the commit, not the internal TeamCity number; in our example, each of the 5 builds had a different value, e.g.  a296c2355c36
* `%sincity.build.problems.json%` and `%sincity.test.failures.json%`: JSON strings describing the build problems and/or test failures that triggered the build
    * in our example, the values were
        * `sincity.build.problems.json` -> `[{"identity":"TC_FAILED_TESTS","type":"TC_FAILED_TESTS","description":"Failed tests detected"}]`
        * `sincity.test.failures.json` -> `[{"fullName":"Foo: flapper","suite":"Foo: ","fullNameWithoutSuite":"flapper"}]`
    * each JSON string contains an array of JSON objects, one per build problem/test failure
    * only those issues are included which are responsible for the build being triggered, i.e. if the [triggering setting](#what-counts-as-build-failure) is "*no* triggering on build problems and triggering on *new* test failures", `%sincity.build.problems.json%` will be set to the empty array (`[]` in JSON) and `%sincity.test.failures.json%` will only contain the new test failures
    * the JSON strings can be useful to help your culprit-finding builds focus on the failures; for example, your build may be able to run the failed tests first or run the failed tests *only*; to find what the failures are, the build configuration must do its own parsing of the two JSON-string parameters
    * to further simplify the use of these JSON strings, SinCity also writes their values to JSON *files* at `%system.teamcity.build.tempDir%/sincity.build.problems.json` and `%system.teamcity.build.tempDir%/sincity.test.failures.json`

NB: All the builds triggered within one culprit-finding investigation have identical values of all the parameters above except for `%sincity.suspect.change%` (which is different for each build). This parameter is thus important in preventing TeamCity from thinking that all the builds are equivalent and removing some of them from the queue (as part of the build queue optimisation). Read more about the build queue optimisation [here](https://confluence.jetbrains.com/display/TCD9/Build+Queue#BuildQueue-BuildQueueOptimizationbyTeamCity).

## Triggered-by message

The intermediate builds triggered manually have a "Triggered by" set to "[USERNAME]; investigating failures between [BUILD NUMBER BOTTOM] and [BUILD NUMBER TOP]".

The intermediate builds triggered automatically have a "Triggered by" set to "SinCity; investigating failures between [BUILD NUMBER BOTTOM] and [BUILD NUMBER TOP]".

## Development notes
To build, test, and package the plugin, run `mvn package` from the root directory.

To deploy the plugin, copy `target/sin-city.zip` into the TeamCity plugin directory and restart the server.

## Ideas for improvement

As of November 2015, SinCity does not perform a binary search. (For that, see the slightly different [Bisect plugin](https://github.com/tkirill/tc-bisect).) Instead, all the builds are queued at the same time. In a sense, binary search seems at odds with the basic premise set out above, namely that builds take too long to run. If that is the case, and we have 6 suspect changes to verify, it's faster to run them all in parallel than it is to run just one (the middle one), then bisect and run another one and then another.

Sometimes the suspicion is bound to a certain build agent, or particular parameter setting. When triggering culprit finding manually, let the user specify these (e.g. trigger all the culprit-finding runs so that they are bound to a given agent).

TeamCity 10 is able to run historical builds using the historical set-up. Again, allow the user to run the culprit-finding builds using the historical set-up or the current one.
