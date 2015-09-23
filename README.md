# SinCity: hunting down the gangstas

SinCity is a TeamCity plugin that helps you find out who broke a build.

Your build configuration has gone from green to red (or from red to redder), but the build that failed covered 7
changes. Which of the 7 changes is responsible? You can manually trigger the same build for the 6 intermediate changes.
If your build configuration has the SinCity build feature enabled, the intermediate builds will be triggered for you
automatically.

## Suitability: who SinCity is for

The build feature is most useful when change grouping for builds (as described above) is the norm rather than an
exception in your build configuration. Change grouping may happen if your builds and/or tests take too long so they
cannot possibly trigger for every change. Or your farm is simply understaffed with agents (or the build configuration is
understaffed, e.g. if for some reason it is confined to a single special agent). Or for some reason you prefer to have a
daily trigger rather than a VCS one.

In all these cases, change grouping is desirable and positive - until someone breaks the build. When that happens (which
is hopefully rare), you typically *do* want to build each suspect change individually. The plugin assumes that this is
possible (i.e. the culprit-finding builds are possible and not prohibitively expensive).

## Mechanics: how it works

When a build fails in a build configuration with the SinCity feature enabled, then if that build covers multiple
changes, the plugin triggers builds corresponding to the intermediate changes.

In the plugin settings, you can specify in more detail what counts as a build failure. See 'Configuration' below.

As of September 2015, SinCity does not perform a binary search. (For that, see the slightly different [Bisect
plugin](https://github.com/tkirill/tc-bisect).) Instead, all the builds are queued at the same time. In a sense, binary
search seems at odds with the basic premise set out above, namely that builds take too long to run. If that is the case,
and we have 6 suspect changes to build, it's faster to run them all in parallel, rather than run just one (the middle
one), then bisect and run another one and then another.

## Features and configuration

To enable the plugin for a build configuration, add it as a Build feature.

The build-feature dialog lets you specify the following settings:

#### Tagging: tell SinCity and non-SinCity builds apart

You can tag all the builds triggered by SinCity (the culprit-finding ones) as well as all the builds *not* triggered by
SinCity. Specify the tag names you want to use. If empty, no tagging is done.

One of the tags is useful if you want to show only the SinCity builds; the other if you want to *hide* them from the
view.

#### Triggering: what counts as failure worth hunting down

The plugin inspects a failing build for two kinds of failures:
1. high-level build problems (e.g. Powershell runner 2 returned a non-zero exit code, Command-line runner 1 timed out,
   There are failing tests)
2. individual test failures (e.g. *MySuite: my.test.package.MyClass.myTest* failed)

For either of these failure kinds, you can specify whether SinCity should trigger culprit-finding when they occur. The
options are:
* No (i.e. ignore this kind of errors)
* New (i.e. only trigger culprit finding if this is a new error)
* All (i.e. trigger culprit finding even if this error already occurred in the previous build)

Setting both options to "No" is effectively the same as disabling culprit-finding completely (except that completed
builds will still be tagged as per above).

#### Parameters: tell each culprit-finding build what it's investigating

You can tell each triggered culprit-finding build what failures it is "investigating." This is passed down in the form
of two build parameters, one for each kind of failure described above (high-level build problems, individual test
failures). Enable this feature by checking the respective checkboxes.

This can be useful to help your build focus on the failures. For example, your build may be able to run the failed tests
first. Or to run *only* the failed tests. To find what the failures are, the build configuration must do its own parsing
of the two JSON-string parameters.

## Future improvements

* binary search - maybe
* for every triggered build, set a parameter containing a JSON string with the list of failures which merited the
  culprit finding
* add a button to the build configuration overview (next to the Run button) which lets you manually trigger SinCity
  for any pair of builds