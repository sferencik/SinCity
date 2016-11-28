package sferencik.teamcity.sincity;

import jetbrains.buildServer.serverSide.BuildPromotion;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.vcs.SVcsModification;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * A FinishedBuildWithChanges object is a SFinishedBuild decorated with information about the related changes. There are
 * two such pieces of information:
 * 1) the last change to have gone into the build
 * 2) the list of changes since the (logically) previous build
 * Both of these are represented by SVcsModification objects (or a list thereof).
 *
 * The extra information about changes is needed because TeamCity's API doesn't deal well with non-linear history.
 *
 * 1) The last change gone into a build.
 *    This addresses an issue with the TeamCity API, which is that when you rerun a build without any changes, the
 *    finished build has getContainingChanges().isEmpty(). While this is logical (there was no new change in this build)
 *    it is not very helpful if you need to get at the last change that went into the build. Currently, there is no
 *    direct way in the API to identify such a change (read https://devnet.jetbrains.com/message/5561032). (You can get
 *    the build's build type -> its VCS roots -> their revisions but that doesn't get you any closer to an
 *    SVcsModification instance.) This is frustrating, since even historical builds have !getContainingChanges.isEmpty()
 *    (though strictly speaking their set should have a "negative" number of elements).
 *
 * 2) The list of changes since the (logically) previous build.
 *    This addresses an issue with the TeamCity API, which is that getContainingChanges() returns the "change delta"
 *    which was valid *when the build finished* but may already be out of date by the time you ask. The "change delta"
 *    typically changes due to non-linear history of builds. For example, if there are changes a, b, c, d, and builds
 *    run in the order A (as of a), D (as of d), then C (as of c) -- note that D ran before C -- the SFinishedBuild
 *    object for D will report getContainingChanges() == (b, c, d) even after C has completed. What we need instead is
 *    the actual list of changes that D contained, which is merely (d).
 *
 * Please note that
 * * multiple builds in the history can share the same last change (e.g. if you rerun a build without any changes being
 *   committed)
 * * the change deltas of the individual builds are disjoint sets (each change in the history is a member of exactly one
 *   build's change delta)
 * * a change delta may be empty (again, this is the case when you rerun a build without any changes in it)
 * * in contrast, the last change is *never* empty because every build is built as of *some* change (logically, it's
 *   quite possible for a build *not* to be built as of *any* change, e.g. if the build configuration has no VCS root
 *   attached - but if we encounter this, we refuse to construct the FinishedBuildWithChanges object)
 *
 *
 */
public class FinishedBuildWithChanges {
    @NotNull
    public SFinishedBuild getBuild() {
        return build;
    }

    @NotNull
    public SVcsModification getLastChange() {
        return lastChange;
    }

    @NotNull
    public List<SVcsModification> getChangeDelta() {
        return changeDelta;
    }

    @NotNull private final SFinishedBuild build;
    @NotNull private final SVcsModification lastChange;
    @NotNull private final List<SVcsModification> changeDelta;

    // keep the constructor public so I can use it in tests; don't document it; it shouldn't be used by clients
    public FinishedBuildWithChanges(@NotNull SFinishedBuild build, @NotNull SVcsModification lastChange, @NotNull List<SVcsModification> changeDelta) {
        this.build = build;
        this.lastChange = lastChange;
        this.changeDelta = changeDelta;
    }

    /**
     * This is a less glorious method than getListFromBuildType() but it's useful when all you need is to find the last
     * change for a build. For that job, calling getListFromBuildType() would be an overkill because
     * 1) it iterates the full history and this is not needed (as explained by JetBrains in
     *    https://devnet.jetbrains.com/message/5561032)
     * 2) it also collects the changeDeltas which is more expensive (it requires that you *finish* the iteration of the
     *    full history - whereas finding the last change can terminate the iteration prematurely)
     * @param build the build for which to find the last change
     * @return the last change that went into the build
     */
    static SVcsModification getLastChange(SFinishedBuild build) {
        BuildPromotion buildPromotion = build.getBuildPromotion();
        while (buildPromotion != null && buildPromotion.getContainingChanges().isEmpty())
            buildPromotion = buildPromotion.getPreviousBuildPromotion(SelectPrevBuildPolicy.SINCE_LAST_BUILD);

        if (buildPromotion == null)
            return null;

        return buildPromotion.getContainingChanges().get(0);
    }

    /**
     * Construct a list of FinishedBuildWithChanges. This is a workaround for the problem described in the class
     * documentation above. To construct this list, we process all the historical builds in the ascending change order,
     * and when we encounter one that has no containing changes, we assume it's built as of the same change as the
     * previous build.
     * @param buildType the build type whose builds we want transformed into a list of FinishedBuildWithChanges objects
     * @return the list of FinishedBuildWithChanges objects
     */
    @NotNull
    public static List<FinishedBuildWithChanges> getListFromBuildType(@NotNull SBuildType buildType) {

        // a full list of builds; the parameters matter:
        // * user = null (no personal builds)
        // * includeCanceled = false
        // * orderByChanges = true (!)
        List<SFinishedBuild> buildHistory = SinCityUtils.getFullHistory(buildType);

        // structures for identifying the last build
        // ~ the last change of every build
        Map<SFinishedBuild, SVcsModification> lastChangeOfBuild = new HashMap<SFinishedBuild, SVcsModification>();
        // ~ a list of outstanding builds for which we have no last change; as we iterate the history backwards, this
        //   keeps track of the builds to which we should set the last change once we find one
        List<SFinishedBuild> buildsWithNoLastChange = new ArrayList<SFinishedBuild>();

        // structures for identifying the change deltas
        // ~ the changes contained in every build; initially/naively, this will be the same as getContainingChanges()
        //   for every build, but in reality we may remove some of the changes from the list in case of non-linear
        //   history
        Map<SFinishedBuild, LinkedHashSet<SVcsModification>> changeDeltaOfBuild = new HashMap<SFinishedBuild, LinkedHashSet<SVcsModification>>();
        // ~ a map mapping each change (key) to the first build that built it (value)
        Map<SVcsModification, SFinishedBuild> changeFirstBuiltBy = new HashMap<SVcsModification, SFinishedBuild>();

        // in our first pass through the history, we fill our auxiliary structures, noting the *real* relationships
        // between builds and changes
        for (SFinishedBuild build : buildHistory) {
            List<SVcsModification> containingChanges = build.getContainingChanges();

            // "last change" structures
            if (containingChanges.isEmpty()) {
                buildsWithNoLastChange.add(build);
            }
            else {
                SVcsModification lastChange = containingChanges.get(0);
                lastChangeOfBuild.put(build, lastChange);

                // this last change also applies to all the "orphaned" builds we've accumulated in
                // buildsWithNoLastChange
                for (SFinishedBuild buildWithNoLastChange : buildsWithNoLastChange)
                    lastChangeOfBuild.put(buildWithNoLastChange, lastChange);
                buildsWithNoLastChange.clear();
            }

            // "change delta" structures
            changeDeltaOfBuild.put(build, new LinkedHashSet<SVcsModification>(containingChanges));
            for (SVcsModification change : containingChanges) {
                if (changeFirstBuiltBy.containsKey(change)) {
                    // one of the more recent builds already thinks it was the first to build this change but this build
                    // obviously built it even sooner; let the other contender know that this change doesn't "belong" to
                    // it
                    changeDeltaOfBuild.get(changeFirstBuiltBy.get(change)).remove(change);
                }
                changeFirstBuiltBy.put(change, build);
            }
        }

        // now pass through the history once more, creating the FinishedBuildWithChanges objects based on the structures
        // we've accumulated (this cannot be done in one pass/in one loop because changeFirstBuiltBy can't be complete
        // until we've seen the whole history)
        List<FinishedBuildWithChanges> buildsWithChanges = new ArrayList<FinishedBuildWithChanges>();
        for (SFinishedBuild build : buildHistory) {
            if (lastChangeOfBuild.containsKey(build)) {
                // don't include a build if it has no last change; it's of no interest in the culprit finder
                buildsWithChanges.add(new FinishedBuildWithChanges(
                        build,
                        lastChangeOfBuild.get(build),
                        new ArrayList<SVcsModification>(changeDeltaOfBuild.get(build))));
            }
        }

        return buildsWithChanges;
    }
}
