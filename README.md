# SinCity: hunting down the gangstas

SinCity is a TeamCity plugin that helps you find out who broke a build.

Your build configuration has gone from green to red (or from red to redder), but the build that failed covered 6 commits.
Which of the 6 commits is to blame?

![From green to red](/images/from-green-to-red.PNG)

You can manually trigger the same build for the 5 intermediate commits, but this can be a tedious process. SinCity lets you
achieve this with a few clicks, and can even trigger the intermediate builds [automatically](#automatic-culprit-finding) if
you choose to do so.

## Manually triggering the culprit finding

The plugin is most useful if your builds are long and/or expensive. In that situation your builds typically cover more than
one commit, which is perfectly fine and probably even desirable - until someone breaks the build. When that happens (which
is hopefully rare), you typically *do* want to build each suspect commit individually.

SinCity adds a new tab called *Trigger culprit finding* to every build configuration. In that tab, you pick two finished
builds that will define your investigation range. For example, for the new test failure above, we want to investigate the
range of 6 commits between builds #5 and #6.

![Manual trigger](/images/manual-trigger-tab.PNG)

When you select the two builds and click *Run*, SinCity will trigger builds for the 5 intermediate commits.

You don't need to pick consecutive builds (like we did). For example, you could run the culprit finding between builds #3
and #6. In that range there have been 5 + 4 + 6 = 15 changes (see the screenshot at the top) so the culprit finding would
result in 14 builds. 2 of these 14 should be effectively "reruns" of #4 and #5.

## What counts as build failure

Same as TeamCity itself, SinCity recognises two types of build issues:

1. high-level build problems (e.g. "Powershell runner #2 returned a non-zero exit code", "Command-line runner #1 timed
   out", "Some tests have failed")
2. individual test failures (e.g. "*MySuite: my.test.package.MyClass.myTest* failed")

For either of these two issue types, you can specify whether SinCity should trigger culprit finding when they occur. You can
choose from three options:
* No (i.e. ignore this kind of issues)
* New (i.e. only trigger culprit finding if this is a new error)
* All (i.e. trigger culprit finding even if this error already occurred in the previous build)

![What counts as build failure](/images/two-types-of-issues.PNG)

The default behaviour is as shown above, i.e. investigate only if there are *new* build problems or new test failures.

## Example

Let's find which of the 6 commits shown above broke the test. We hit *Run* at the bottom of the *Trigger culprit finding*
tab and SinCity queues 5 builds. After they've completed, the situation looks as follows:

![overview-with-cf-builds](/images/overview-with-cf-builds.PNG)

TeamCity is showing the builds ordered by start time. Let's consult the *Change Log* view to see their logical ordering (by
commit):

![change-log-with-cf-builds](/images/change-log-with-cf-builds.PNG)

Notice how the builds #5 and #6 delimit the range which was originally opaque to us. SinCity has helped us identify that the
test was broken by build #9, i.e. most likely by the change marked as "01:24:39".

## Configuration parameters of the triggered builds

Each build triggered by SinCity will have the following configuration parameters set:

* `%sincity.range.bottom.build.id%` and `%sincity.range.top.build.id%`: the build IDs (internal TeamCity IDs) of the two
  builds that define the culprit-finding range; these references allow your triggered builds to ask questions about the
  original builds (e.g. using the TeamCity REST API); in our example, these were 633 and 635 respectively
* `%sincity.range.bottom.build.number%` and `%sincity.range.top.build.number%`: the build numbers (display numbers) of the
  two builds that define the culprit-finding range; these are mostly for user convenience, so you can quickly see what range
  the build is investigating; in our example, these were 5 and 6 respectively
* `%sincity.suspect.change%`: the "version" of the suspect commit for which this build is running; this is the display name
  of the commit, not the internal TeamCity number; in our example, each of the 5 builds had a different value, e.g.
  a296c2355c36
* `%sincity.build.problems.json%` and `%sincity.test.failures.json%`: JSON strings describing the build problems and/or test
  failures that triggered the build
    * in our example, the values were
        * `sincity.build.problems.json` -> `[{"identity":"TC_FAILED_TESTS","type":"TC_FAILED_TESTS","description":"Failed tests detected"}]`
        * `sincity.test.failures.json` -> `[{"fullName":"Foo: flapper","suite":"Foo: ","fullNameWithoutSuite":"flapper"}]`
    * each JSON string contains an array of JSON objects, one per build problem/test failure
    * only those issues are included which are responsible for the build being triggered, i.e. if the [triggering
      setting](#what-counts-as-build-failure) is "*no* triggering on build problems and triggering on *new* test
      failures", `%sincity.build.problems.json%` will be set to the empty array (`[]` in JSON) and
      `%sincity.test.failures.json%` will only contain the new test failures
    * the JSON strings can be useful to help your culprit-finding builds focus on the failures; for example, your build
      may be able to run the failed tests first or run the failed tests *only*; to find what the failures are, the build
      configuration must do its own parsing of the two JSON-string parameters
    * to further simplify the use of these JSON strings, SinCity also writes their values to JSON *files* at
      `%system.teamcity.build.tempDir%/sincity.build.problems.json` and
      `%system.teamcity.build.tempDir%/sincity.test.failures.json`

NB: All the builds triggered within one culprit-finding investigation have identical values of all the parameters above
except for `%sincity.suspect.change%` (which is different for each build). This parameter is thus important in preventing
TeamCity from thinking that all the builds are equivalent and removing some of them from the queue (as part of the build
queue optimisation). Read more about the build queue optimisation
[here](https://confluence.jetbrains.com/display/TCD9/Build+Queue#BuildQueue-BuildQueueOptimizationbyTeamCity).

## Automatic culprit finding

You can set your build configuration to trigger the culprit finding automatically every time there are build failures
and the failing build covered more than one commit. To do so, enable the "SinCity" build feature for your build
configuration.

![build-feature](/images/build-feature.PNG)


![build-feature-details](/images/build-feature-details.PNG)

The bottom part of this screen should already look familiar. The settings in the top part (*Tagging*) are useful if you want
to distinguish the builds triggered by SinCity from the other builds (since they all intermingle in the build configuration
history). If you supply the desired tag names, SinCity will apply these tag names to all the builds in the given build
configuration.

![Tagging](/images/tagging.PNG)

If you leave the tagging text fields empty, no tagging is done.

The culprit-finding builds triggered automatically are put to the queue as soon as the failing build completes. Given that
they logically belong to the just-finished build, they are put to the *top* of the queue so they can run ASAP. The same is
not true for manually triggered builds, since they are "merely" someone's personal initiative, just like when someone
triggers a build manually.

## Triggered-by message

The intermediate builds triggered manually have a "Triggered by" set to "[USERNAME]; investigating failures between
[BUILD NUMBER BOTTOM] and [BUILD NUMBER TOP]".

The intermediate builds triggered automatically have a "Triggered by" set to "SinCity; investigating failures between
[BUILD NUMBER BOTTOM] and [BUILD NUMBER TOP]".

## Development notes
To build, test, and package the plugin, run `mvn package` from the root directory.

To deploy the plugin, copy `target/sin-city.zip` into the TeamCity plugin directory and restart the server.

## Future improvements

As of November 2015, SinCity does not perform a binary search. (For that, see the slightly different [Bisect
plugin](https://github.com/tkirill/tc-bisect).) Instead, all the builds are queued at the same time. In a sense, binary
search seems at odds with the basic premise set out above, namely that builds take too long to run. If that is the case,
and we have 6 suspect changes to verify, it's faster to run them all in parallel than it is to run just one (the middle
one), then bisect and run another one and then another.
