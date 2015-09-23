package sferencik.teamcity.sincity.json;

import com.google.gson.Gson;
import jetbrains.buildServer.BuildProblemData;

import java.util.ArrayList;
import java.util.List;

public abstract class Encoder {
    public static String encodeTestNames(List<jetbrains.buildServer.tests.TestName> testNames) {
        List<TestName> serialisableTestNames = new ArrayList<TestName>();
        for (jetbrains.buildServer.tests.TestName tn : testNames) {
            serialisableTestNames.add(new TestName(tn));
        }
        return new Gson().toJson(serialisableTestNames);
    }

    public static String encodeBuildProblems(List<BuildProblemData> buildProblems) {
        List<BuildProblem> serialisableBuildProblems = new ArrayList<BuildProblem>();
        for (BuildProblemData bp : buildProblems) {
            serialisableBuildProblems.add(new BuildProblem(bp));
        }
        return new Gson().toJson(serialisableBuildProblems);
    }
}
