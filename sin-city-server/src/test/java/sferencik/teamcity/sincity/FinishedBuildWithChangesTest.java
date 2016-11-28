package sferencik.teamcity.sincity;

import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.vcs.SVcsModification;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.collection.IsIterableContainingInOrder;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class FinishedBuildWithChangesTest {
    private Mockery mockery;
    private SBuildType buildType;
    private SVcsModification change1;
    private SVcsModification change2;
    private SVcsModification change3;
    private SVcsModification change4;
    private List<SFinishedBuild> buildHistory;
    private SFinishedBuild build3;
    private SFinishedBuild build2;
    private SFinishedBuild build1;

    @BeforeMethod
    public void SetUp() {
        mockery = new Mockery();
        buildType = mockery.mock(SBuildType.class, "BuildType");

        build1 = mockery.mock(SFinishedBuild.class, "Build1");
        build2 = mockery.mock(SFinishedBuild.class, "Build2");
        build3 = mockery.mock(SFinishedBuild.class, "Build3");

        buildHistory = new ArrayList<SFinishedBuild>();
        buildHistory.add(build3);
        buildHistory.add(build2);
        buildHistory.add(build1);

        change1 = mockery.mock(SVcsModification.class, "change1");
        change2 = mockery.mock(SVcsModification.class, "change2");
        change3 = mockery.mock(SVcsModification.class, "change3");
        change4 = mockery.mock(SVcsModification.class, "change4");

        mockery.checking(new Expectations() {{
            oneOf(buildType).getHistory(null, true, true); will(returnValue(buildHistory));
            oneOf(build1).getCanceledInfo(); will(returnValue(null));
            oneOf(build2).getCanceledInfo(); will(returnValue(null));
            oneOf(build3).getCanceledInfo(); will(returnValue(null));
        }});
    }

    @Test
    public void test01LinearHistory() {

        /*
            A typical scenario: builds are triggered in linear sequence with respect to the changes.

                        time --------->
            change 1 -> build1
            change 2
            change 3 ->        build2
            change 4 ->               build3
         */

        // arrange
        mockery.checking(new Expectations() {{
            oneOf(build3).getContainingChanges(); will(returnValue(Collections.singletonList(change4)));
            oneOf(build2).getContainingChanges(); will(returnValue(Arrays.asList(change3, change2)));
            oneOf(build1).getContainingChanges(); will(returnValue(Collections.singletonList(change1)));
        }});

        // act
        List<FinishedBuildWithChanges> list = FinishedBuildWithChanges.getListFromBuildType(buildType);

        // assert
        mockery.assertIsSatisfied();

        assertThat(list.get(0).getBuild(), is(build3));
        assertThat(list.get(0).getLastChange(), is(change4));
        assertThat(list.get(0).getChangeDelta(), IsIterableContainingInOrder.contains(Collections.singletonList(change4).toArray()));
        assertThat(list.get(1).getBuild(), is(build2));
        assertThat(list.get(1).getLastChange(), is(change3));
        assertThat(list.get(1).getChangeDelta(), IsIterableContainingInOrder.contains(Arrays.asList(change3, change2).toArray()));
        assertThat(list.get(2).getBuild(), is(build1));
        assertThat(list.get(2).getLastChange(), is(change1));
        assertThat(list.get(2).getChangeDelta(), IsIterableContainingInOrder.contains(Collections.singletonList(change1).toArray()));

    }

    @Test
    public void test02RepeatedBuild() {

        /*
            Scenario: build1 and build2 run in sequence (e.g. VCS-triggered), then build3 runs with no changes in it
            (e.g. triggered manually by a user).

                        time --------->
            change 1 -> build1
            change 2
            change 3 ->        build2 build3
         */

        // arrange
        mockery.checking(new Expectations() {{
            oneOf(build3).getContainingChanges(); will(returnValue(new ArrayList<SVcsModification>()));
            oneOf(build2).getContainingChanges(); will(returnValue(Arrays.asList(change3, change2)));
            oneOf(build1).getContainingChanges(); will(returnValue(Collections.singletonList(change1)));
        }});

        // act
        List<FinishedBuildWithChanges> list = FinishedBuildWithChanges.getListFromBuildType(buildType);

        // assert
        mockery.assertIsSatisfied();

        assertThat(list.get(0).getBuild(), is(build3));
        assertThat(list.get(0).getLastChange(), is(change3));
        assertThat(list.get(0).getChangeDelta(), IsEmptyCollection.empty());
        assertThat(list.get(1).getBuild(), is(build2));
        assertThat(list.get(1).getLastChange(), is(change3));
        assertThat(list.get(1).getChangeDelta(), IsIterableContainingInOrder.contains(Arrays.asList(change3, change2).toArray()));
        assertThat(list.get(2).getBuild(), is(build1));
        assertThat(list.get(2).getLastChange(), is(change1));
        assertThat(list.get(2).getChangeDelta(), IsIterableContainingInOrder.contains(change1));

    }

    @Test
    public void test03CulpritFinderForward() {

        /*
            Scenario: After completing build3, we start build1, build2 (e.g. triggered by SinCity, or manually), which
            run in order with respect to the changes.

                        time --------->
            change 1 ->        build1
            change 2
            change 3 ->               build2
            change 4 -> build3
         */

        // arrange
        mockery.checking(new Expectations() {{
            oneOf(build3).getContainingChanges(); will(returnValue(Arrays.asList(change4, change3, change2, change1)));
            oneOf(build2).getContainingChanges(); will(returnValue(Arrays.asList(change3, change2)));
            oneOf(build1).getContainingChanges(); will(returnValue(Collections.singletonList(change1)));
        }});

        // act
        List<FinishedBuildWithChanges> list = FinishedBuildWithChanges.getListFromBuildType(buildType);

        // assert
        mockery.assertIsSatisfied();

        assertThat(list.get(0).getBuild(), is(build3));
        assertThat(list.get(0).getLastChange(), is(change4));
        assertThat(list.get(0).getChangeDelta(), IsIterableContainingInOrder.contains(change4));
        assertThat(list.get(1).getBuild(), is(build2));
        assertThat(list.get(1).getLastChange(), is(change3));
        assertThat(list.get(1).getChangeDelta(), IsIterableContainingInOrder.contains(Arrays.asList(change3, change2).toArray()));
        assertThat(list.get(2).getBuild(), is(build1));
        assertThat(list.get(2).getLastChange(), is(change1));
        assertThat(list.get(2).getChangeDelta(), IsIterableContainingInOrder.contains(Collections.singletonList(change1).toArray()));

    }

    @Test
    public void test04CulpritFinderBackward() {

        /*
            Scenario: After completing build3, we start build1, build2 (e.g. triggered by SinCity, or manually), which
            run out of order with respect to the changes.

                        time --------->
            change 1 ->               build1
            change 2
            change 3 ->        build2
            change 4 -> build3
         */

        // arrange
        mockery.checking(new Expectations() {{
            oneOf(build3).getContainingChanges();
            will(returnValue(Arrays.asList(change4, change3, change2, change1)));
            oneOf(build2).getContainingChanges(); will(returnValue(Arrays.asList(change3, change2, change1)));
            oneOf(build1).getContainingChanges(); will(returnValue(Collections.singletonList(change1)));
        }});

        // act
        List<FinishedBuildWithChanges> list = FinishedBuildWithChanges.getListFromBuildType(buildType);

        // assert
        mockery.assertIsSatisfied();

        assertThat(list.get(0).getBuild(), is(build3));
        assertThat(list.get(0).getLastChange(), is(change4));
        assertThat(list.get(0).getChangeDelta(), IsIterableContainingInOrder.contains(change4));
        assertThat(list.get(1).getBuild(), is(build2));
        assertThat(list.get(1).getLastChange(), is(change3));
        assertThat(list.get(1).getChangeDelta(), IsIterableContainingInOrder.contains(Arrays.asList(change3, change2).toArray()));
        assertThat(list.get(2).getBuild(), is(build1));
        assertThat(list.get(2).getLastChange(), is(change1));
        assertThat(list.get(2).getChangeDelta(), IsIterableContainingInOrder.contains(Collections.singletonList(change1).toArray()));
    }

    @Test
    public void test05NoChanges() {

        /*
            Scenario: build1 and build2 ran before the first change was recorded for the build configuration. As
            explained in FinishedBuildWithChanges, this is possible if a VCS root wasn't added until later.

                        time --------->
                        build1
                               build2
            change 1
            change 2
            change 3
            change 4 ->               build3
         */

        // arrange
        mockery.checking(new Expectations() {{
            oneOf(build3).getContainingChanges(); will(returnValue(Arrays.asList(change4, change3, change2, change1)));
            oneOf(build2).getContainingChanges(); will(returnValue(Collections.emptyList()));
            oneOf(build1).getContainingChanges(); will(returnValue(Collections.emptyList()));
        }});

        // act
        List<FinishedBuildWithChanges> list = FinishedBuildWithChanges.getListFromBuildType(buildType);

        // assert
        mockery.assertIsSatisfied();

        assertThat(list.get(0).getBuild(), is(build3));
        assertThat(list.get(0).getLastChange(), is(change4));
        assertThat(list.get(0).getChangeDelta(), IsIterableContainingInOrder.contains(Arrays.asList(change4, change3, change2, change1).toArray()));
        assertThat(list.size(), is(1)); // builds with no last change don't make it
    }
}
