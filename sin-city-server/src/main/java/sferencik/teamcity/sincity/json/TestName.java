package sferencik.teamcity.sincity.json;

import com.google.gson.annotations.SerializedName;

public class TestName {
    private String fullName;

    private String suite;

    private String fullNameWithoutSuite;

    @SerializedName("package")
    private String packageName;

    @SerializedName("class")
    private String className;

    private String test;

    private String parameters;

    public TestName(jetbrains.buildServer.tests.TestName tcTestName) {
        fullName = Util.nullifyEmptyString(tcTestName.getAsString());
        suite = Util.nullifyEmptyString(tcTestName.getSuite());
        fullNameWithoutSuite = Util.nullifyEmptyString(tcTestName.getNameWithoutSuite());
        packageName = Util.nullifyEmptyString(tcTestName.getPackageName());
        className = Util.nullifyEmptyString(tcTestName.getClassName());
        test = Util.nullifyEmptyString(tcTestName.getTestMethodName());
        parameters = Util.nullifyEmptyString(tcTestName.getParameters());
    }
}
