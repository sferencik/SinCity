package sferencik.teamcity.sincity.manualTrigger;

import jetbrains.buildServer.serverSide.BuildPromotion;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.vcs.SVcsModification;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A FinishedBuildWithChange object is really just an SFinishedBuild decorated with a reference to the last change
 * (SVcsModification).
 *
 * Why we need this is described here: https://devnet.jetbrains.com/message/5561032.
 *
 * This addresses an issue with the TeamCity API, which is that when you rerun a build without any changes, the
 * finished build has getContainingChanges().isEmpty(). There is no good way to get at the top change contained in that
 * build. (You can get its build type -> its VCS roots -> their revisions but that doesn't get you any closer to an
 * SVcsModification instance.) This is frustrating, since even historical builds have !getContainingChanges.isEmpty()
 * (though strictly speaking their set should have a "negative" number of elements).
 */
public class FinishedBuildWithChange {
    @SuppressWarnings("unused") // used from triggerCulpritFinding.jsp
    public SFinishedBuild getBuild() {
        return build;
    }

    @SuppressWarnings("unused") // used from triggerCulpritFinding.jsp
    public SVcsModification getChange() {
        return change;
    }

    private final SFinishedBuild build;
    private final SVcsModification change;

    private FinishedBuildWithChange(SFinishedBuild build, SVcsModification change) {
        this.build = build;
        this.change = change;
    }

    private static FinishedBuildWithChange fromSFinishedBuild(@NotNull SFinishedBuild build) {
        BuildPromotion buildPromotion = build.getBuildPromotion();
        while (buildPromotion != null && buildPromotion.getContainingChanges().isEmpty())
            buildPromotion = buildPromotion.getPreviousBuildPromotion(SelectPrevBuildPolicy.SINCE_LAST_BUILD);

        if (buildPromotion == null)
            return null;

        return new FinishedBuildWithChange(build, buildPromotion.getContainingChanges().get(0));
    }

    /**
     * Construct a list of FinishedBuildWithChanges. This is a workaround for the problem described in the class
     * documentation above. To construct this list, we process all the historical builds in the ascending change order,
     * and when we encounter one that has no containing changes, we assume it's built as of the same change as the
     * previous build.
     * @param sBuildType the build type whose builds we want transformed into a list of FinishedBuildWithChange objects
     * @return the list of FinishedBuildWithChange objects
     */
    @NotNull
    static List<FinishedBuildWithChange> getListFromBuildType(@NotNull SBuildType sBuildType) {
        List<FinishedBuildWithChange> buildsWithChange = new ArrayList<FinishedBuildWithChange>();
        for (SFinishedBuild build : sBuildType.getHistory(null, false, true)) {
            FinishedBuildWithChange buildWithChange = fromSFinishedBuild(build);
            if (buildWithChange != null)
                buildsWithChange.add(buildWithChange);
        }
        return buildsWithChange;
    }
}
